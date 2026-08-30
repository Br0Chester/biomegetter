package com.idk.biomegetter.block.entity;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.block.custom.cauldron.data.*;
import com.idk.biomegetter.block.custom.cauldron.data.spec_spices.BuffTransform;
import com.idk.biomegetter.block.custom.cauldron.data.spec_spices.SoupEatTrigger;
import com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemStartRingLoader;
import com.idk.biomegetter.block.custom.cauldron.data.totem.TotemProcessLoader;
import com.idk.biomegetter.block.custom.cauldron.data.totem.TotemStartRing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Общее состояние наших котлов.
 * <p>
 * Содержимое хранится ДВУМЯ независимыми стеками, максимум по 3 "трети" каждый:
 * <ul>
 *   <li>{@link #liquidLayers} — стек жидкости (вода/молоко/сок/лава/готовый СУП), слой 0 =
 *       нижний, последний = верхний. Новый слой всегда добавляется сверху.</li>
 *   <li>{@link #solidSlot} — стек твёрдого. Каждая запись — это просто ссылка (Identifier)
 *       на {@link SolidComponentType} из датапака.</li>
 * </ul>
 * <p>
 * СУПОВАРЕНИЕ — отдельный "творческий" процесс, НЕ описываемый JSON-рецептом
 * ({@link CauldronRecipe}/{@link CauldronRecipeLoader}). Точка входа: жидкость 3/3 одного
 * типа (любой бульон) + ровно 1 предмет в твёрдом стеке с {@code SolidComponentType.uniqueStarter()
 * == "soup"} (см. {@link #canStartSoup()}). Дальше — фиксированный 2-стадийный процесс
 * (cook → collect_open), параметры которого общие для всех супов ({@link SoupProcessConfig}).
 * Во время сбора ингредиентов допустимо докидывать всё, для чего зарегистрирован файл в
 * {@code soup_ingredient} (см. {@link CauldronSoupIngredientLoader}), плюс разово использовать
 * соль (доп. раунд) и любую зарегистрированную приправу ({@link CauldronSoupSpiceLoader}).
 * Результат — НЕ отдельный предмет, а полноценные 3 слоя ЖИДКОСТИ-супа
 * ({@link LiquidLayer.Soup}) с уже посчитанными эффектами/сытостью/именем/текстурой/тинтом
 * (по категории — см. {@link CauldronSoupCategoryLoader}); миска зачерпывает по трети,
 * аналогично ведру для обычной жидкости (см. {@link #toItemStack(SoupLiquidData)}).
 * <p>
 * Обычные JSON-рецепты ({@link CauldronRecipe}) продолжают работать полностью независимо от
 * этого механизма — их состояние тоже живёт в {@link #brewing}, различается флагом
 * {@link BrewingState#soup()}.
 */
public class ModCauldronBlockEntity extends BlockEntity {

    public static final int EVAPORATION_INTERVAL_TICKS = 600; // 30 секунд
    public static final int MELT_INTERVAL_TICKS = 600; // 30 секунд
    public static final int MAX_STACK_SIZE = 3;

    private static final Identifier DEFAULT_SOUP_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "block/water_still");

    @Nullable
    private BrewingState brewing;

    /**
     * @param soup      true — это варка СУПА (нет привязки к {@link CauldronRecipeLoader}, стадии
     *                  и их длительности берутся из {@link SoupProcessConfig}); false — обычный
     *                  JSON-рецепт ({@code recipeId} резолвится через {@link CauldronRecipeLoader}).
     * @param composite накопленные id ингредиентов+приправ (солид+жидкость), используется ТОЛЬКО
     *                  соп-процессом сейчас; для обычных рецептов зарезервировано на будущее
     *                  (см. javadoc {@link CauldronRecipeStage.CollectOpenStage}).
     */
    public record BrewingState(
            Identifier recipeId, int stageIndex, int ticksRemaining, int idlePhaseIndex,
            List<Identifier> composite,
            int roundsRemaining,
            boolean saltUsed, boolean spiceUsed,
            boolean soup
    ) {
    }

    public static final Identifier SNOW_COMPONENT_ID = BiomeGetter.id("powder_snow");
    public static final Identifier WATER_COMPONENT_ID = BiomeGetter.id("water");

    /**
     * Домен-метка для {@code unique_starter} обычной картошки — запускает именно СУП, а не
     * какую-то другую будущую творческую подсистему (алхимия и т.п.).
     */
    private static final String SOUP_STARTER_DOMAIN = "soup";

    // ---- Окружение (кэш, обновляется в serverTick / neighborChanged) ----

    private boolean hasBlockAbove;
    private boolean heatedBelow;
    private int evaporationTimer = EVAPORATION_INTERVAL_TICKS;

    private boolean powderSnowAbove;
    private int meltTimer = MELT_INTERVAL_TICKS;

    private int lightLevel;

    // ---- Кеш проверки ритуальной структуры (перепроверяется раз в RITUAL_CHECK_INTERVAL_TICKS,
    // не каждый тик — обход блоков в мире дороже обычных проверок) ----
    private static final int RITUAL_CHECK_INTERVAL_TICKS = 10;
    private int ritualCheckCooldown = 0;
    private boolean cachedRitualSatisfied = true;
    private List<BlockPos> cachedRitualPositions = List.of();

    // ---- Очередь "поглощения"/трансформации блоков (см. BlockConsumptionRule) ----
    private static final int CONSUMPTION_STEP_TICKS = 10;   // задержка между обработкой соседних блоков очереди
    private static final int CONSUMPTION_PLACEMENT_DELAY_TICKS = 2; // задержка между эффектом разрушения и эффектом установки одного и того же блока
    private static final Identifier AIR_ID = Identifier.fromNamespaceAndPath("minecraft", "air");

    /**
     * Пара "позиция + свой пул результатов" — каждая запись очереди несёт СВОЙ result_pool, а
     * не общий на всю очередь. Это устраняет баг: если два разных BlockConsumptionRule
     * срабатывают на одной стадии ОДНОГО котла, позиции второго правила больше не будут
     * ошибочно брать пул первого.
     */
    private record ConsumptionEntry(BlockPos pos, List<Identifier> pool) {
    }

    private final java.util.Deque<ConsumptionEntry> consumptionQueue = new java.util.ArrayDeque<>();
    private int consumptionCooldown = 0;

    @Nullable
    private BlockPos consumptionAwaitingPlacementPos;
    @Nullable
    private Identifier consumptionAwaitingPlacementBlock;
    private int consumptionPlacementDelay = 0;

    @Nullable
    private TotemBrewingState totemBrewing;

    // ---- Содержимое ----

    public sealed interface LiquidLayer {
        record Liquid(Identifier typeId) implements LiquidLayer {
        }

        /**
         * Готовый суп — все 3 слоя после варки заполняются идентичным {@link SoupLiquidData}
         * (это одна и та же "партия"). Миска забирает 1 слой = 1 порцию.
         */
        record Soup(SoupLiquidData data) implements LiquidLayer {
        }
    }

    /**
     * Посчитанные один раз (в момент завершения варки) итоговые данные супа.
     */
    public record SoupLiquidData(
            String displayName,
            List<MobEffectInstance> effects,
            int sustenance,
            Identifier texture,
            int tint,
            List<SoupEatTrigger> eatTriggers
    ) {
    }

    public record SolidEntry(Identifier typeId) {
    }

    public record PressOutcome(boolean pressed, int burnedThirds) {
        public static final PressOutcome NONE = new PressOutcome(false, 0);
    }

    private final List<LiquidLayer> liquidLayers = new ArrayList<>();
    private final List<SolidEntry> solidSlot = new ArrayList<>();

    public ModCauldronBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ---- Окружение: геттеры/сеттеры ----

    public boolean hasBlockAbove() {
        return this.hasBlockAbove;
    }

    public void setHasBlockAbove(boolean value) {
        this.hasBlockAbove = value;
    }

    public boolean isHeatedBelow() {
        return this.heatedBelow;
    }

    public void setHeatedBelow(boolean value) {
        this.heatedBelow = value;
    }

    public boolean isPowderSnowAbove() {
        return this.powderSnowAbove;
    }

    public void setPowderSnowAbove(boolean value) {
        this.powderSnowAbove = value;
    }

    public int getLightLevel() {
        return this.lightLevel;
    }

    public void setLightLevel(int value) {
        this.lightLevel = value;
    }

    public boolean tickEvaporation() {
        if (--this.evaporationTimer <= 0) {
            this.evaporationTimer = EVAPORATION_INTERVAL_TICKS;
            return true;
        }
        return false;
    }

    public boolean tickMelt() {
        if (--this.meltTimer <= 0) {
            this.meltTimer = MELT_INTERVAL_TICKS;
            return true;
        }
        return false;
    }

    private int computeEmittedLight() {
        int max = 0;
        for (LiquidLayer layer : this.liquidLayers) {
            if (layer instanceof LiquidLayer.Liquid liquid) {
                LiquidComponentType type = CauldronLiquidComponentLoader.get(liquid.typeId());
                if (type != null) max = Math.max(max, type.lightLevel());
            }
        }
        for (SolidEntry entry : this.solidSlot) {
            SolidComponentType type = CauldronSolidComponentLoader.get(entry.typeId());
            if (type != null) max = Math.max(max, type.lightLevel());
        }
        return max;
    }

    public void updateLightSource() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        int desired = computeEmittedLight();
        BlockPos abovePos = worldPosition.above();
        BlockState aboveState = serverLevel.getBlockState(abovePos);
        boolean isOurLight = aboveState.is(Blocks.LIGHT);

        if (desired <= 0) {
            if (isOurLight) {
                serverLevel.setBlock(abovePos, Blocks.AIR.defaultBlockState(), 3);
            }
            return;
        }

        if (!aboveState.isAir() && !isOurLight) {
            return;
        }
        if (isOurLight && aboveState.getValue(LightBlock.LEVEL) == desired) {
            return;
        }

        serverLevel.setBlock(abovePos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, desired), 3);
    }

    // ---- Жидкостный стек ----

    public List<LiquidLayer> getLiquidLayers() {
        return Collections.unmodifiableList(this.liquidLayers);
    }

    public int getLiquidCount() {
        return this.liquidLayers.size();
    }

    @Nullable
    public LiquidLayer getTopLiquidLayer() {
        return this.liquidLayers.isEmpty() ? null : this.liquidLayers.get(this.liquidLayers.size() - 1);
    }

    public boolean isLiquidUniformFull() {
        return this.liquidLayers.size() == MAX_STACK_SIZE
                && this.liquidLayers.get(0).equals(this.liquidLayers.get(1))
                && this.liquidLayers.get(1).equals(this.liquidLayers.get(2));
    }

    public int applyLiquidContact(Identifier incomingTypeId, int amount) {
        LiquidComponentType incomingType = CauldronLiquidComponentLoader.get(incomingTypeId);
        String incomingGroup = incomingType != null ? incomingType.group() : "neutral";

        int burned = 0;
        LiquidReactionRule activeRule = null;
        while (burned < amount && !this.liquidLayers.isEmpty()) {
            LiquidLayer top = this.liquidLayers.get(this.liquidLayers.size() - 1);
            String topGroup = topGroupOf(top);
            LiquidReactionRule rule = CauldronLiquidReactionLoader.find(incomingGroup, topGroup);
            if (rule == null) break;
            this.liquidLayers.remove(this.liquidLayers.size() - 1);
            burned++;
            activeRule = rule;
        }

        int remaining = amount - burned;
        int freeSpace = MAX_STACK_SIZE - this.liquidLayers.size();
        int added = Math.max(0, Math.min(remaining, freeSpace));
        for (int i = 0; i < added; i++) {
            this.liquidLayers.add(new LiquidLayer.Liquid(incomingTypeId));
        }

        if (burned > 0 || added > 0) {
            if (burned > 0) {
                applyLiquidReactionEffects(activeRule, burned);
            }
            trimSolidAfterLiquidChange(incomingGroup);
            this.setChanged();
            syncToClient();
        }

        return burned;
    }

    private String topGroupOf(LiquidLayer layer) {
        if (layer instanceof LiquidLayer.Liquid liquid) {
            LiquidComponentType type = CauldronLiquidComponentLoader.get(liquid.typeId());
            return type != null ? type.group() : "neutral";
        }
        return "neutral"; // включая LiquidLayer.Soup — суп ни с чем не реагирует
    }

    private void applyLiquidReactionEffects(@Nullable LiquidReactionRule rule, int burnedThirds) {
        if (rule == null || !(level instanceof ServerLevel)) return;
        for (ReactionEffect effect : rule.effects()) {
            effect.apply(level, worldPosition, burnedThirds);
        }
    }

    private void trimSolidAfterLiquidChange(String liquidGroup) {
        while (!this.solidSlot.isEmpty()) {
            SolidEntry top = this.solidSlot.get(this.solidSlot.size() - 1);
            SolidComponentType type = CauldronSolidComponentLoader.get(top.typeId());
            String solidGroup = type != null ? type.group() : "neutral";
            SolidReactionRule rule = CauldronSolidReactionLoader.find(liquidGroup, solidGroup);
            if (rule == null) break;
            this.solidSlot.remove(this.solidSlot.size() - 1);
            if (level instanceof ServerLevel) {
                for (ReactionEffect effect : rule.effects()) {
                    effect.apply(level, worldPosition, 1);
                }
            }
        }
    }

    @Nullable
    public LiquidLayer removeTopLiquidLayer() {
        if (this.liquidLayers.isEmpty()) return null;
        LiquidLayer removed = this.liquidLayers.remove(this.liquidLayers.size() - 1);
        this.setChanged();
        syncToClient();
        return removed;
    }

    public List<LiquidLayer> takeAllLiquid() {
        List<LiquidLayer> taken = new ArrayList<>(this.liquidLayers);
        this.liquidLayers.clear();
        this.setChanged();
        syncToClient();
        return taken;
    }

    // ---- Твёрдый стек ----

    public List<SolidEntry> getSolidSlot() {
        return Collections.unmodifiableList(this.solidSlot);
    }

    public int getSolidCount() {
        return this.solidSlot.size();
    }

    @Nullable
    public SolidEntry getTopSolidEntry() {
        return this.solidSlot.isEmpty() ? null : this.solidSlot.get(this.solidSlot.size() - 1);
    }

    public boolean isSolidUniformFull() {
        if (this.solidSlot.size() != MAX_STACK_SIZE) return false;
        Identifier first = this.solidSlot.get(0).typeId();
        return this.solidSlot.stream().allMatch(e -> e.typeId().equals(first));
    }

    public boolean canAddSolid(String incomingSolidGroup) {
        if (this.solidSlot.size() >= MAX_STACK_SIZE) return false;
        if (reactsWithCurrentLiquid(incomingSolidGroup)) return false;
        if (this.solidSlot.isEmpty()) return true;
        SolidComponentType existingType = CauldronSolidComponentLoader.get(this.solidSlot.get(0).typeId());
        String existingGroup = existingType != null ? existingType.group() : "neutral";
        return existingGroup.equals(incomingSolidGroup);
    }

    public boolean reactsWithCurrentLiquid(String solidGroup) {
        for (LiquidLayer layer : this.liquidLayers) {
            if (CauldronSolidReactionLoader.find(topGroupOf(layer), solidGroup) != null) return true;
        }
        return false;
    }

    public void addSolid(Identifier typeId) {
        this.solidSlot.add(new SolidEntry(typeId));
        this.setChanged();
        syncToClient();
    }

    @Nullable
    public ItemStack extractTopSolid() {
        if (this.solidSlot.isEmpty()) return null;
        SolidEntry entry = this.solidSlot.remove(this.solidSlot.size() - 1);
        this.setChanged();
        syncToClient();
        return toDroppedStack(entry);
    }

    public void clearSolid() {
        this.solidSlot.clear();
        this.setChanged();
        syncToClient();
    }

    public PressOutcome pressTopComponent() {
        if (this.solidSlot.isEmpty()) return PressOutcome.NONE;
        int topIndex = this.solidSlot.size() - 1;
        SolidEntry entry = this.solidSlot.get(topIndex);
        SolidComponentType type = CauldronSolidComponentLoader.get(entry.typeId());
        if (type == null || !type.isPressable()) return PressOutcome.NONE;

        this.solidSlot.remove(topIndex);
        int burned = applyLiquidContact(type.producesJuice().orElseThrow(), 1);
        this.setChanged();
        syncToClient();
        return new PressOutcome(true, burned);
    }

    public int meltSnow() {
        if (!isLiquidAllowed(WATER_COMPONENT_ID)) return 0; // на будущее, если вода когда-то попадёт в блэклист
        long snowCount = this.solidSlot.stream().filter(e -> e.typeId().equals(SNOW_COMPONENT_ID)).count();
        if (snowCount == 0) return 0;

        this.solidSlot.removeIf(e -> e.typeId().equals(SNOW_COMPONENT_ID));
        int burned = applyLiquidContact(WATER_COMPONENT_ID, (int) snowCount);
        this.setChanged();
        syncToClient();
        return burned;
    }

    public List<ItemStack> drainAllSolids() {
        List<ItemStack> drops = new ArrayList<>();
        for (SolidEntry entry : this.solidSlot) {
            ItemStack stack = toDroppedStack(entry);
            if (stack != null) drops.add(stack);
        }
        this.solidSlot.clear();
        this.setChanged();
        return drops;
    }

    @Nullable
    private static ItemStack toDroppedStack(SolidEntry entry) {
        SolidComponentType type = CauldronSolidComponentLoader.get(entry.typeId());
        if (type == null) return null;
        return new ItemStack(type.outputItem(), type.outputCount());
    }

    private void syncToClient() {
        updateLightSource();
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(worldPosition);
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    public BrewingState getBrewing() {
        return this.brewing;
    }

    public boolean isBrewing() {
        return this.brewing != null;
    }

    public boolean isCooking() {
        if (totemBrewing != null) return totemBrewing.cooking();
        if (brewing == null) return false;
        if (brewing.soup()) return brewing.stageIndex() == 0;
        CauldronRecipe recipe = CauldronRecipeLoader.get(brewing.recipeId());
        return recipe != null && recipe.stages().get(brewing.stageIndex()) instanceof CauldronRecipeStage.CookStage;
    }

    public boolean isAwaitingIngredient() {
        return isBrewing() && !isCooking();
    }

    /**
     * true — прямо сейчас идёт этап свободного докидывания ингредиентов СУПА (единственное
     * место, где действуют соль/специя/whitelist ингредиентов).
     */
    public boolean isCollectingIngredients() {
        return brewing != null && brewing.soup() && brewing.stageIndex() == 1;
    }


    /**
     * true, если у рецепта нет поля "ritual" (условие не задано), либо структура блоков вокруг
     * котла реально собрана прямо сейчас. Кешируется на {@link #RITUAL_CHECK_INTERVAL_TICKS}
     * тиков — обход блоков в мире на каждый тик был бы избыточно дорог, раз проверка идёт
     * только во время активной варки (cook/await), а не постоянно.
     */
    private boolean isRitualSatisfied(CauldronRecipe recipe) {
        if (recipe.ritual().isEmpty()) return true;
        if (ritualCheckCooldown > 0) {
            ritualCheckCooldown--;
            return cachedRitualSatisfied;
        }
        ritualCheckCooldown = RITUAL_CHECK_INTERVAL_TICKS;

        List<RitualTemplate> templates = new ArrayList<>();
        for (Identifier id : recipe.ritual()) {
            RitualTemplate template = CauldronRitualTemplateLoader.get(id);
            if (template == null) {
                cachedRitualSatisfied = false;
                cachedRitualPositions = List.of();
                return false;
            }
            templates.add(template);
        }
        if (level == null) {
            cachedRitualSatisfied = false;
            cachedRitualPositions = List.of();
            return false;
        }

        var result = RitualTemplate.matchComposite(templates, level, worldPosition);
        cachedRitualSatisfied = result.isPresent();
        cachedRitualPositions = result.orElse(List.of());
        return cachedRitualSatisfied;
    }


    /**
     * Запускает поглощение блоков для всех правил рецепта, у которых {@code stageIndex}
     * совпадает с {@code completingStageIndex} ({@code -1} — сентинел "рецепт завершается
     * целиком", используется в finishBrewing). Несколько правил на один триггер — каждое
     * независимо кидает свою кость на chance.
     */
    private void triggerBlockConsumption(CauldronRecipe recipe, int completingStageIndex) {
        if (!(level instanceof ServerLevel)) return;
        for (BlockConsumptionRule rule : recipe.blockConsumption()) {
            boolean triggersHere = rule.stageIndex()
                    .map(idx -> idx == completingStageIndex)
                    .orElse(completingStageIndex == -1);
            if (!triggersHere) continue;
            if (level.getRandom().nextFloat() >= rule.chance()) continue;
            if (rule.resultPool().isEmpty()) continue;

            List<BlockPos> area = resolveConsumptionArea(rule);
            if (area.isEmpty()) continue;

            List<BlockPos> shuffled = new ArrayList<>(area);
            java.util.Collections.shuffle(shuffled, new java.util.Random(level.getRandom().nextLong()));
            int limit = Math.min(rule.maxBlocks(), shuffled.size());

            for (BlockPos pos : shuffled.subList(0, limit)) {
                consumptionQueue.addLast(new ConsumptionEntry(pos, rule.resultPool()));
            }
            consumptionCooldown = 0; // первый блок обработается на ближайшем tickBlockConsumption
        }
    }

    private List<BlockPos> resolveConsumptionArea(BlockConsumptionRule rule) {
        if (rule.area().isEmpty()) {
            return cachedRitualPositions; // область не задана явно — последняя успешно сматченная область ritual-условия рецепта
        }
        if (level == null) return List.of();

        List<RitualTemplate> templates = new ArrayList<>();
        for (Identifier id : rule.area()) {
            RitualTemplate template = CauldronRitualTemplateLoader.get(id);
            if (template == null) return List.of(); // один из шаблонов area невалиден — вся area невалидна
            templates.add(template);
        }
        return RitualTemplate.matchComposite(templates, level, worldPosition).orElse(List.of());
    }

    /**
     * Тикает очередь поглощения (вызывается КАЖДЫЙ серверный тик из ModCauldronBlock.serverTick,
     * независимо от состояния варки — очередь должна доработать даже если варка уже завершилась
     * или была прервана). Каждые CONSUMPTION_STEP_TICKS обрабатывает один блок: если он всё ещё
     * физически на месте — сначала эффект разрушения, спустя CONSUMPTION_PLACEMENT_DELAY_TICKS —
     * замена на случайный блок из пула с эффектом установки. Если блока там уже нет — тихий
     * пропуск (см. Q3), очередь просто идёт дальше.
     */
    public void tickBlockConsumption() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (consumptionAwaitingPlacementPos != null) {
            if (--consumptionPlacementDelay <= 0) {
                BlockPos pos = consumptionAwaitingPlacementPos;
                Identifier resultId = consumptionAwaitingPlacementBlock;
                consumptionAwaitingPlacementPos = null;
                consumptionAwaitingPlacementBlock = null;
                placeConsumptionResult(serverLevel, pos, resultId);
            }
            return; // пока не разместили текущий результат — очередь дальше не двигаем
        }

        if (consumptionQueue.isEmpty()) return;
        if (consumptionCooldown > 0) {
            consumptionCooldown--;
            return;
        }
        consumptionCooldown = CONSUMPTION_STEP_TICKS;

        ConsumptionEntry entry = consumptionQueue.poll();
        BlockPos pos = entry.pos();
        BlockState oldState = level.getBlockState(pos);
        if (oldState.isAir()) return; // блока уже нет — тихий пропуск, шаг сгорел впустую

        // Эффект разрушения старого блока — ванильный "block break" (партиклы + звук), сразу
        serverLevel.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(oldState));

        List<Identifier> pool = entry.pool();
        Identifier resultId = pool.get(level.getRandom().nextInt(pool.size()));
        consumptionAwaitingPlacementPos = pos;
        consumptionAwaitingPlacementBlock = resultId;
        consumptionPlacementDelay = CONSUMPTION_PLACEMENT_DELAY_TICKS;
    }

    private void placeConsumptionResult(ServerLevel serverLevel, BlockPos pos, Identifier resultId) {
        boolean toAir = resultId.equals(AIR_ID);
        if (!toAir && serverLevel.getBlockState(pos).isAir()) return; // за 2 тика ожидания блок уже кто-то убрал

        BlockState newState;
        if (toAir) {
            newState = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        } else {
            net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(resultId);
            newState = block != null ? block.defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }

        serverLevel.setBlockAndUpdate(pos, newState);
        if (!newState.isAir()) {
            net.minecraft.world.level.block.SoundType soundType = newState.getSoundType();
            serverLevel.playSound(null, pos, soundType.getPlaceSound(),
                    net.minecraft.sounds.SoundSource.BLOCKS, soundType.getVolume(), soundType.getPitch());
        }
    }

    // Тотемы
    public boolean isTotemBrewing() {
        return totemBrewing != null;
    }

    @Nullable
    public TotemBrewingState getTotemBrewing() {
        return totemBrewing;
    }

    /**
     * true, если можно запустить варку тотема прямо сейчас: твёрдый стек однороден по group
     * (material) и все записи имеют ОДИНАКОВЫЙ уровень (totem_solid_component_level); если
     * есть жидкость — она тоже обязана быть одного уровня (totem_liquid_component_level),
     * совпадающего с твёрдым.
     */
    public boolean canStartTotem() {
        if (isBrewing() || isTotemBrewing()) return false;
        if (solidSlot.isEmpty() || solidSlot.size() != MAX_STACK_SIZE) return false;
        if (liquidLayers.isEmpty() || liquidLayers.size() != MAX_STACK_SIZE)
            return false; // ФИКС: жидкость теперь ОБЯЗАТЕЛЬНА (3/3), а не пропускалась молча при пустом стеке

        String material = null;
        Integer level = null;
        for (SolidEntry entry : solidSlot) {
            SolidComponentType type = CauldronSolidComponentLoader.get(entry.typeId());
            var levelDef = com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemSolidLevelLoader.get(entry.typeId());
            if (type == null || levelDef == null) return false;
            if (material == null) material = type.group();
            else if (!material.equals(type.group())) return false;
            if (level == null) level = levelDef.level();
            else if (!level.equals(levelDef.level())) return false;
        }

        for (LiquidLayer layer : liquidLayers) {
            if (!(layer instanceof LiquidLayer.Liquid liquid)) return false;
            var levelDef = com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemLiquidLevelLoader.get(liquid.typeId());
            if (levelDef == null || levelDef.level() != level) return false;
        }

        // ФИКС: стартовое кольцо никогда не проверялось — canStartTotem смотрел только на
        // состав котла, полностью игнорируя структуру вокруг него.
        if (level == null || this.level == null) return false;
        return findMatchingStartRing(level) != null;
    }

    /**
     * Ищет ПЕРВОЕ зарегистрированное стартовое кольцо нужного уровня, реально построенное
     * вокруг котла прямо сейчас — если несколько разных колец одного уровня описывают разную
     * форму, побеждает первое найденное по порядку (детерминированный, но зависящий от
     * загрузки файловой системы выбор — как и было оговорено для менее критичных случаев).
     */
    @Nullable
    private TotemStartRing findMatchingStartRing(int requiredLevel) {
        for (var entry : CauldronTotemStartRingLoader.all().entrySet()) {
            var ring = entry.getValue();
            if (ring.level() != requiredLevel) continue;
            if (ring.template().match(this.level, worldPosition).isPresent()) return ring;
        }
        return null;
    }

    public void startTotemBrewing() {
        if (!canStartTotem()) return;
        String material = CauldronSolidComponentLoader.get(solidSlot.get(0).typeId()).group();
        int level = com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemSolidLevelLoader
                .get(solidSlot.get(0).typeId()).level();

        var startRing = findMatchingStartRing(level);
        if (startRing != null) {
            var matchOpt = startRing.template().match(this.level, worldPosition);
            matchOpt.ifPresent(positions -> {
                if (this.level instanceof ServerLevel serverLevel) {
                    for (BlockPos pos : positions) {
                        serverLevel.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(serverLevel.getBlockState(pos)));
                        serverLevel.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            });
        }

        this.solidSlot.clear();
        this.liquidLayers.clear();
        var cfg = com.idk.biomegetter.block.custom.cauldron.data.totem.TotemProcessLoader.get();
        this.totemBrewing = new TotemBrewingState(material, level, 0, true, cfg.cookDurationTicks(), level,
                List.of(), java.util.Optional.empty(), java.util.Optional.empty(), Map.of(), 0);
        this.setChanged();
        syncToClient();
    }

    /**
     * Вызывается ModCauldronBlock ПОСЛЕ обычного addSolid/applyLiquidContact, пока идёт варка
     * тотема — фиксирует id как потенциальный тег-источник для текущего этапа.
     */
    public void noteTotemIngredient(Identifier componentId) {
        if (totemBrewing == null || totemBrewing.cooking()) return; // во время cook-фазы докидывать нельзя
        List<Identifier> collected = new ArrayList<>(totemBrewing.collectedTags());
        collected.add(componentId);
        this.totemBrewing = new TotemBrewingState(totemBrewing.material(), totemBrewing.level(), totemBrewing.stage(),
                totemBrewing.cooking(), totemBrewing.ticksRemaining(),
                totemBrewing.ritualBudgetRemaining(), collected, totemBrewing.forcedSkill(),
                totemBrewing.chosenPassiveSkill(), totemBrewing.passiveStats(), totemBrewing.emptyStageCount());
    }

    /**
     * Клик готовым тотемом по котлу во время сборки — форсирует скилл, если его теги подходят.
     */
    public void showTotemForForcedSkill(Identifier skillId) {
        if (totemBrewing == null) return;
        this.totemBrewing = new TotemBrewingState(totemBrewing.material(), totemBrewing.level(), totemBrewing.stage(),
                totemBrewing.cooking(), totemBrewing.ticksRemaining(),
                totemBrewing.ritualBudgetRemaining(), totemBrewing.collectedTags(), java.util.Optional.of(skillId),
                totemBrewing.chosenPassiveSkill(), totemBrewing.passiveStats(), totemBrewing.emptyStageCount());
    }

    public void tryAdvanceTotemStage() {
        if (totemBrewing == null || level == null || totemBrewing.cooking()) return;

        java.util.Set<String> tags = new java.util.HashSet<>();
        for (Identifier id : totemBrewing.collectedTags()) {
            var solidType = CauldronSolidComponentLoader.get(id);
            var liquidType = CauldronLiquidComponentLoader.get(id);
            if (solidType != null) tags.addAll(solidType.tags());
            if (liquidType != null) tags.addAll(liquidType.tags());
        }

        boolean isPassiveStage = totemBrewing.stage() == 0;

        java.util.Map<Identifier, com.idk.biomegetter.block.custom.cauldron.data.totem.TotemSkill> candidates = new java.util.LinkedHashMap<>();
        for (String tag : tags) {
            var matches = isPassiveStage
                    ? com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemPassiveSkillLoader.byTag(tag)
                    : com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemActiveSkillLoader.byTag(tag);
            for (var entry : matches) candidates.put(entry.getKey(), entry.getValue());
        }

        BiomeGetter.LOGGER.info("ModCauldromBlockEntity Totem stage {} advance: collectedTags(ids)={}, resolvedTags={}, candidates={}",
                isPassiveStage ? "PASSIVE" : "ACTIVE", totemBrewing.collectedTags(), tags, candidates.keySet());

        Identifier chosenId = null;
        if (totemBrewing.forcedSkill().isPresent() && candidates.containsKey(totemBrewing.forcedSkill().get())) {
            chosenId = totemBrewing.forcedSkill().get();
        } else if (!candidates.isEmpty()) {
            List<Identifier> ids = new ArrayList<>(candidates.keySet());
            chosenId = ids.get(level.getRandom().nextInt(ids.size()));
        }
        BiomeGetter.LOGGER.info("ModCauldromBlockEntity Totem stage {} chose: {}", isPassiveStage ? "PASSIVE" : "ACTIVE", chosenId);

        var chosenSkill = chosenId != null ? candidates.get(chosenId) : null;

        var appliedRings = com.idk.biomegetter.block.custom.cauldron.data.totem.TotemStatRingResolver.resolve(
                level, worldPosition, totemBrewing.ritualBudgetRemaining(), level.getRandom());
        int spent = appliedRings.stream().mapToInt(r -> r.ring().level()).sum();

        java.util.Set<String> cauldronElements = new java.util.HashSet<>(); // см. известное упрощение — не заполняется пока
        Map<String, Float> stats = chosenSkill != null ? chosenSkill.computedStats(appliedRings, cauldronElements) : Map.of();
        int newEmptyCount = totemBrewing.emptyStageCount() + (chosenSkill == null ? 1 : 0);

        // Поглощаем ВСЁ, что физически лежит в котле, НЕЗАВИСИМО от того, найден скилл или
        // нет — согласованное правило: "нашли — используем, не нашли — ничего страшного, всё
        // равно поглощаем".
        this.solidSlot.clear();
        this.liquidLayers.clear();


        if (isPassiveStage) {
            var cfg = com.idk.biomegetter.block.custom.cauldron.data.totem.TotemProcessLoader.get();
            this.totemBrewing = new TotemBrewingState(totemBrewing.material(), totemBrewing.level(), 1,
                    true, cfg.cookDurationTicks(), // переходим во вторую cook-фазу перед сборкой активки
                    totemBrewing.ritualBudgetRemaining() - spent, List.of(), java.util.Optional.empty(),
                    chosenId != null ? java.util.Optional.of(chosenId) : java.util.Optional.empty(), stats, newEmptyCount);
        } else {
            finishTotemBrewing(chosenId, stats, newEmptyCount);
        }
        this.setChanged();
        syncToClient();
    }

    private void finishTotemBrewing(@Nullable Identifier activeSkillId, Map<String, Float> activeStats, int emptyStageCount) {
        if (totemBrewing == null) return;
        int baseDurability = 20 + totemBrewing.level() * 30;
        int compensatedDurability = Math.round(baseDurability * (1.0f + 0.25f * emptyStageCount));

        boolean hasPassive = totemBrewing.chosenPassiveSkill().isPresent();
        ItemStack result = com.idk.biomegetter.item.custom.TotemItem.build(
                totemBrewing.material(), totemBrewing.level(),
                hasPassive ? totemBrewing.chosenPassiveSkill().get() : Identifier.fromNamespaceAndPath("minecraft", "air"),
                totemBrewing.passiveStats(),
                activeSkillId != null ? activeSkillId : Identifier.fromNamespaceAndPath("minecraft", "air"),
                activeStats,
                compensatedDurability, compensatedDurability
        );
        this.totemBrewing = null;

        // Тотем выпадает АВТОМАТИЧЕСКИ, как булыжник при реакции лавы с водой — не нужно
        // забирать его отдельным кликом.
        if (level != null) {
            net.minecraft.world.level.block.Block.popResource(level, worldPosition, result);
        }
    }

    @Nullable
    private ItemStack pendingTotemResult;

    public boolean hasPendingTotemResult() {
        return pendingTotemResult != null;
    }

    @Nullable
    public ItemStack collectTotemResult() {
        if (pendingTotemResult == null) return null;
        ItemStack result = pendingTotemResult.copy();
        pendingTotemResult = null;
        this.setChanged();
        syncToClient();
        return result;
    }

    // ---- Запуск обычного (JSON) рецепта ----

    public void startBrewing(Identifier recipeId, @Nullable Entity consumedEntity) {
        CauldronRecipe recipe = CauldronRecipeLoader.get(recipeId);
        if (recipe == null || recipe.stages().isEmpty()) return;
        this.liquidLayers.clear();
        this.solidSlot.clear();
        if (consumedEntity != null && level instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    consumedEntity.getX(), consumedEntity.getY() + 0.5, consumedEntity.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
            consumedEntity.discard();
        }
        this.ritualCheckCooldown = 0; // форсируем немедленную проверку на первом же tickBrewing после старта
        CauldronRecipeStage firstStage = recipe.stages().get(0);
        int initialRounds = firstStage instanceof CauldronRecipeStage.CollectOpenStage ? 1 : 0;
        this.brewing = new BrewingState(recipeId, 0, firstStageTicks(firstStage), 0, List.of(), initialRounds, false, false, false);

        this.setChanged();
        syncToClient();
    }

    private static int firstStageTicks(CauldronRecipeStage stage) {
        if (stage instanceof CauldronRecipeStage.CookStage cook) return cook.durationTicks();
        if (stage instanceof CauldronRecipeStage.AwaitIngredientStage await && !await.idlePhases().isEmpty()) {
            return await.idlePhases().get(0).durationTicks();
        }
        if (stage instanceof CauldronRecipeStage.CollectOpenStage collect && !collect.idlePhases().isEmpty()) {
            return collect.idlePhases().get(0).durationTicks();
        }
        return 0;
    }

    // ---- Запуск СУПА (без JSON-рецепта) ----

    /**
     * true, если прямо сейчас можно запустить суп: жидкость 3/3 одного типа (любой бульон) +
     * ровно 1 предмет в твёрдом стеке, отмеченный {@code unique_starter: "soup"}.
     */
    public boolean canStartSoup() {
        if (isBrewing()) return false;
        if (!isLiquidUniformFull()) return false;
        if (this.solidSlot.size() != 1) return false;
        SolidComponentType type = CauldronSolidComponentLoader.get(this.solidSlot.get(0).typeId());
        return type != null && type.uniqueStarter().map(SOUP_STARTER_DOMAIN::equals).orElse(false);
    }

    public void startSoupBrewing() {
        if (!canStartSoup()) return;

        // Бульон (ЛЮБАЯ однородная жидкость, из которой стартует суп — вода, молоко, мясной
        // бульон и т.д.) сам становится первым элементом composite. Если для его id
        // зарегистрирован soup_ingredient/*.json — он даст эффект/сытость/категорию точно так
        // же, как и любой докидываемый ингредиент (см. resolveComponent). Для воды такого
        // файла нет — она просто ничего не даёт, штатное поведение.
        List<Identifier> initialComposite = new ArrayList<>();
        if (getTopLiquidLayer() instanceof LiquidLayer.Liquid brothLiquid) {
            initialComposite.add(brothLiquid.typeId());
        }

        this.liquidLayers.clear();
        this.solidSlot.clear();
        SoupProcessConfig config = CauldronSoupProcessLoader.get();
        this.brewing = new BrewingState(BiomeGetter.id("soup"), 0, config.cookDurationTicks(), 0, initialComposite, 0, false, false, true);
        this.setChanged();
        syncToClient();
    }

    // ---- Соль / специя (только во время сбора ингредиентов супа) ----

    public boolean useSalt() {
        if (brewing == null || brewing.saltUsed() || !isCollectingIngredients()) return false;
        this.brewing = new BrewingState(brewing.recipeId(), brewing.stageIndex(), brewing.ticksRemaining(), brewing.idlePhaseIndex(),
                brewing.composite(), brewing.roundsRemaining() + 1, true, brewing.spiceUsed(), brewing.soup());
        this.setChanged();
        syncToClient();
        return true;
    }

    public boolean useSpice(Identifier spiceComponentId) {
        if (brewing == null || brewing.spiceUsed() || !isCollectingIngredients()) return false;
        List<Identifier> newComposite = new ArrayList<>(brewing.composite());
        newComposite.add(spiceComponentId);
        this.brewing = new BrewingState(brewing.recipeId(), brewing.stageIndex(), brewing.ticksRemaining(), brewing.idlePhaseIndex(),
                newComposite, brewing.roundsRemaining(), brewing.saltUsed(), true, brewing.soup());
        this.setChanged();
        syncToClient();
        return true;
    }

    /**
     * Whitelist докидываемых ингредиентов — действует ТОЛЬКО во время сбора ингредиентов супа
     * (см. {@link #isCollectingIngredients()}); вне этого контекста ограничений нет.
     */
    public boolean isIngredientAllowed(Identifier componentId) {
        if (!isCollectingIngredients()) return true;
        return CauldronSoupIngredientLoader.get(componentId) != null;
    }

    /**
     * Гейт улучшения котла — некоторые жидкости (лава, в будущем помеченные тегом
     * "unstable_magic") можно налить/иметь ТОЛЬКО в улучшенном котле (см.
     * CauldronUpgradeBlacklistLoader). На улучшенном котле (обычный ModCauldronBlock, не
     * ModCauldronBlockBasic) — всегда true. Неизвестный тип жидкости (нет
     * liquid_component-файла) трактуется КОНСЕРВАТИВНО как запрещённый — fail-safe на случай,
     * если для какой-то опасной жидкости (например лавы) забыли завести JSON-файл.
     */
    public boolean isLiquidAllowed(Identifier liquidTypeId) {
        boolean isBasic = getBlockState().getBlock() instanceof com.idk.biomegetter.block.custom.ModCauldronBlockBasic;
        if (!isBasic) {
//            BiomeGetter.LOGGER.info("isLiquidAllowed({}): block is NOT ModCauldronBlockBasic (actual class: {}) -> allowed",
//                    liquidTypeId, getBlockState().getBlock().getClass().getSimpleName());
            return true;
        }
        LiquidComponentType type = CauldronLiquidComponentLoader.get(liquidTypeId);
        if (type == null) {
//            BiomeGetter.LOGGER.info("isLiquidAllowed({}): no liquid_component found -> denied (fail-safe)", liquidTypeId);
            return false;
        }
        boolean blacklisted = CauldronUpgradeBlacklistLoader.isBlacklisted(type);
//        BiomeGetter.LOGGER.info("isLiquidAllowed({}): group={}, tags={}, blacklisted={} -> {}",
//                liquidTypeId, type.group(), type.tags(), blacklisted, !blacklisted);
        return !blacklisted;
    }
    // ---- Перемешивание ----

    public void tryAdvanceAwaitStage() {
        if (brewing == null || brewing.soup()) return;
        CauldronRecipe recipe = CauldronRecipeLoader.get(brewing.recipeId());
        if (recipe == null) return;
        if (!(recipe.stages().get(brewing.stageIndex()) instanceof CauldronRecipeStage.AwaitIngredientStage await))
            return;

        if (!CauldronRecipeLoader.matchesExactly(this, await.requiredLiquids(), await.requiredSolids())) return;

        this.liquidLayers.clear();
        this.solidSlot.clear();
        advanceToNextStage(recipe);
    }

    public void tryAdvanceCollectOpenStage() {
        if (brewing == null) return;
        if (brewing.soup()) {
            tryAdvanceSoupCollecting();
            return;
        }
        CauldronRecipe recipe = CauldronRecipeLoader.get(brewing.recipeId());
        if (recipe == null) return;
        if (!(recipe.stages().get(brewing.stageIndex()) instanceof CauldronRecipeStage.CollectOpenStage collectStage))
            return;

        // Зарезервировано на будущее (например, алхимия) — composite копится, но встроенного
        // "финализатора" для НЕ-супа сейчас нет; итог просто переходит на следующую стадию.
        List<Identifier> newComposite = new ArrayList<>(brewing.composite());
        boolean addedAnything = !this.solidSlot.isEmpty() || !this.liquidLayers.isEmpty();
        for (SolidEntry entry : this.solidSlot) newComposite.add(entry.typeId());
        for (LiquidLayer layer : this.liquidLayers) {
            if (layer instanceof LiquidLayer.Liquid liquid) newComposite.add(liquid.typeId());
        }
        this.solidSlot.clear();
        this.liquidLayers.clear();

        if (!addedAnything || brewing.roundsRemaining() <= 0) {
            this.brewing = new BrewingState(brewing.recipeId(), brewing.stageIndex(), 0, 0,
                    newComposite, brewing.roundsRemaining(), brewing.saltUsed(), brewing.spiceUsed(), false);
            advanceToNextStage(recipe);
        } else {
            int nextTicks = collectStage.idlePhases().isEmpty() ? 0 : collectStage.idlePhases().get(0).durationTicks();
            this.brewing = new BrewingState(brewing.recipeId(), brewing.stageIndex(), nextTicks, 0,
                    newComposite, brewing.roundsRemaining() - 1, brewing.saltUsed(), brewing.spiceUsed(), false);
        }
        this.setChanged();
        syncToClient();
    }

    private void tryAdvanceSoupCollecting() {
        SoupProcessConfig config = CauldronSoupProcessLoader.get();
        List<Identifier> newComposite = new ArrayList<>(brewing.composite());
        boolean addedAnything = !this.solidSlot.isEmpty() || !this.liquidLayers.isEmpty();
        for (SolidEntry entry : this.solidSlot) newComposite.add(entry.typeId());
        for (LiquidLayer layer : this.liquidLayers) {
            if (layer instanceof LiquidLayer.Liquid liquid) newComposite.add(liquid.typeId());
        }
        this.solidSlot.clear();
        this.liquidLayers.clear();

        if (!addedAnything || brewing.roundsRemaining() <= 0) {
            finishSoup(newComposite);
        } else {
            int nextTicks = config.idlePhases().isEmpty() ? 0 : config.idlePhases().get(0).durationTicks();
            this.brewing = new BrewingState(brewing.recipeId(), brewing.stageIndex(), nextTicks, 0,
                    newComposite, brewing.roundsRemaining() - 1, brewing.saltUsed(), brewing.spiceUsed(), true);
            this.setChanged();
            syncToClient();
        }
    }

    /**
     * Единая точка входа для перемешивания вне cook-стадии — определяет реальный тип текущей
     * стадии (либо соп-режим) и делегирует нужному обработчику.
     */
    public void tryAdvanceCurrentStage() {
        if (brewing == null) return;
        if (brewing.soup()) {
            tryAdvanceCollectOpenStage();
            return;
        }
        CauldronRecipe recipe = CauldronRecipeLoader.get(brewing.recipeId());
        if (recipe == null) return;
        CauldronRecipeStage stage = recipe.stages().get(brewing.stageIndex());
        if (stage instanceof CauldronRecipeStage.AwaitIngredientStage) {
            tryAdvanceAwaitStage();
        } else if (stage instanceof CauldronRecipeStage.CollectOpenStage) {
            tryAdvanceCollectOpenStage();
        }
    }

    private void advanceToNextStage(CauldronRecipe recipe) {
        triggerBlockConsumption(recipe, brewing.stageIndex());
        int nextIndex = brewing.stageIndex() + 1;
        if (nextIndex >= recipe.stages().size()) {
            finishBrewing(recipe);
            return;
        }
        CauldronRecipeStage nextStage = recipe.stages().get(nextIndex);
        int rounds = nextStage instanceof CauldronRecipeStage.CollectOpenStage ? 1 : 0;
        this.brewing = new BrewingState(brewing.recipeId(), nextIndex, firstStageTicks(nextStage), 0,
                brewing.composite(), rounds, brewing.saltUsed(), brewing.spiceUsed(), false);
        this.setChanged();
        syncToClient();
    }

    private void finishBrewing(CauldronRecipe recipe) {
        triggerBlockConsumption(recipe, -1);
        if (recipe.resultLiquid().isPresent()) {
            Identifier resultId = recipe.resultLiquid().get();
            for (int i = 0; i < recipe.resultCount(); i++) this.liquidLayers.add(new LiquidLayer.Liquid(resultId));
        }
        if (level instanceof ServerLevel) {
            for (ReactionEffect effect : recipe.completionEffects()) effect.apply(level, worldPosition, 1);
        }
        this.brewing = null;
        this.setChanged();
        syncToClient();
    }

    /**
     * Завершение варки супа — считает итог и заполняет котёл 3 слоями {@link LiquidLayer.Soup}
     * (одна и та же "партия" во всех трёх слоях — миска забирает по трети).
     */
    private void finishSoup(List<Identifier> composite) {
        SoupLiquidData data = aggregateSoup(composite);
        this.liquidLayers.clear();
        for (int i = 0; i < MAX_STACK_SIZE; i++) this.liquidLayers.add(new LiquidLayer.Soup(data));
        if (level instanceof ServerLevel) {
            for (ReactionEffect effect : CauldronSoupProcessLoader.get().completionEffects()) {
                effect.apply(level, worldPosition, 1);
            }
        }
        this.brewing = null;
        this.setChanged();
        syncToClient();
    }

    /**
     * Единый вид "разрешённого" компонента — не важно, обычный ингредиент это или приправа.
     */
    private record ResolvedComponent(
            List<IngredientEffectDef.EffectEntry> effects, int sustenance,
            Optional<String> category, List<IngredientEffectDef.EffectModifier> modifiers,
            List<BuffTransform> buffTransforms, List<SoupEatTrigger> eatTriggers
    ) {
    }

    @Nullable
    private static ResolvedComponent resolveComponent(Identifier id) {
        IngredientEffectDef ingredient = CauldronSoupIngredientLoader.get(id);
        if (ingredient != null) {
            return new ResolvedComponent(ingredient.effects(), ingredient.sustenance(), ingredient.category(),
                    ingredient.modifiers(), ingredient.buffTransforms(), List.of()); // обычные ингредиенты eat_triggers не имеют
        }
        SoupSpiceDef spice = CauldronSoupSpiceLoader.get(id);
        if (spice != null) {
            return new ResolvedComponent(spice.effects(), spice.sustenance(), spice.category(),
                    spice.modifiers(), spice.buffTransforms(), spice.eatTriggers());
        }
        return null;
    }

    /**
     * Согласованный алгоритм силы/длительности эффектов:
     * <ul>
     *   <li>Один и тот же ингредиент, положенный несколько раз — длительности его эффектов
     *       суммируются, СИЛА (amplifier) не растёт.</li>
     *   <li>Разные ингредиенты, дающие один и тот же эффект — за каждый ДОПОЛНИТЕЛЬНЫЙ
     *       отличающийся источник сила увеличивается на +1 (не выше {@code maxAmplifier}).</li>
     *   <li>Модификаторы приправ применяются последним проходом — домножают длительность/
     *       добавляют силу эффектам, чей источник (ингредиент ИЛИ его категория) совпадает
     *       с целью модификатора. На насыщение не влияют.</li>
     * </ul>
     */
    private SoupLiquidData aggregateSoup(List<Identifier> componentIds) {
        Map<Holder<MobEffect>, List<EffectSource>> byEffect = new LinkedHashMap<>();
        Set<String> categories = new LinkedHashSet<>();
        List<IngredientEffectDef.EffectModifier> spiceModifiers = new ArrayList<>();
        List<BuffTransform> buffTransforms = new ArrayList<>();
        List<SoupEatTrigger> eatTriggers = new ArrayList<>();
        int totalSustenance = 0;

        for (Identifier id : componentIds) {
            ResolvedComponent def = resolveComponent(id);
            if (def == null) continue;
            totalSustenance += def.sustenance();
            def.category().ifPresent(categories::add);
            spiceModifiers.addAll(def.modifiers());
            buffTransforms.addAll(def.buffTransforms());
            eatTriggers.addAll(def.eatTriggers());
            for (IngredientEffectDef.EffectEntry entry : def.effects()) {
                byEffect.computeIfAbsent(entry.effect(), k -> new ArrayList<>())
                        .add(new EffectSource(id, def.category(), entry.amplifier(), entry.durationTicks(), entry.maxAmplifier()));
            }
        }

        List<MobEffectInstance> instances = new ArrayList<>();

        for (var entry : byEffect.entrySet()) {
            instances.add(buildEffectInstance(entry.getKey(), entry.getValue(), spiceModifiers));
        }

        // Конструктор именованных функций-трансформаций (см. BuffTransform) — применяется
        // последним проходом над уже готовым списком эффектов, каждая специя/ингредиент может
        // заявить свою функцию через "buff_transforms" в JSON.
        for (BuffTransform transform : buffTransforms) {
            instances = transform.apply(instances);
        }

        SoupCategoryRule rule = CauldronSoupCategoryLoader.pickRule(categories);
        Identifier texture = rule.icon().orElse(DEFAULT_SOUP_TEXTURE);
        int tint = rule.tintColor().orElse(0xFFFFFFFF);

        BiomeGetter.LOGGER.info("Cauldron soup finished: composite={}, eatTriggers={}", componentIds, eatTriggers.size());

        return new SoupLiquidData(rule.displayName(), instances, totalSustenance, texture, tint, eatTriggers);
    }

    private record EffectSource(Identifier ingredientId, Optional<String> category, int amplifier, int durationTicks,
                                int maxAmplifier) {
    }

    private static MobEffectInstance buildEffectInstance(
            Holder<MobEffect> holder, List<EffectSource> sources, List<IngredientEffectDef.EffectModifier> spiceModifiers
    ) {
        Set<Identifier> distinctIngredients = new LinkedHashSet<>();
        int baseAmplifier = 0;
        int cap = 0;
        int totalDuration = 0;
        for (EffectSource src : sources) {
            distinctIngredients.add(src.ingredientId());
            baseAmplifier = Math.max(baseAmplifier, src.amplifier());
            cap = Math.max(cap, src.maxAmplifier());
            totalDuration += src.durationTicks();
        }
        int amplifier = Math.min(baseAmplifier + (distinctIngredients.size() - 1), cap);

        float durationMultiplier = 1.0f;
        int amplifierBonus = 0;
        for (IngredientEffectDef.EffectModifier modifier : spiceModifiers) {
            boolean targetsThisEffect = sources.stream().anyMatch(src ->
                    modifier.targetIngredient().map(t -> t.equals(src.ingredientId())).orElse(false)
                            || (modifier.targetCategory().isPresent() && modifier.targetCategory().equals(src.category())));
            if (!targetsThisEffect) continue;
            durationMultiplier *= modifier.durationMultiplier().orElse(1.0f);
            amplifierBonus += modifier.amplifierBonus().orElse(0);
        }

        amplifier = Math.min(amplifier + amplifierBonus, cap);
        int finalDuration = Math.max(0, Math.round(totalDuration * durationMultiplier));
        return new MobEffectInstance(holder, finalDuration, amplifier);
    }

    /**
     * Строит забираемый миской предмет из одной порции супа (см. {@link LiquidLayer.Soup}).
     */
    public static ItemStack toItemStack(SoupLiquidData data) {
        ItemStack result = new ItemStack(com.idk.biomegetter.item.ModItems.SOUP_BOWL); // предмет готового супа (см. SoupBowlItem)
        result.set(DataComponents.CUSTOM_NAME, Component.literal(data.displayName()));
        result.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.empty(),
                Optional.empty(),
                data.effects(),
                Optional.of(data.displayName())
        ));

        CompoundTag customData = new CompoundTag();
        customData.putInt("Sustenance", data.sustenance());
        if (!data.eatTriggers().isEmpty()) {
            SoupEatTrigger.CODEC.listOf().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, data.eatTriggers())
                    .result().ifPresent(tag -> customData.put("EatTriggers", tag));
        }
        result.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

        if (data.sustenance() > 0) {
            result.set(DataComponents.FOOD, new FoodProperties(data.sustenance(), data.sustenance() * 0.6f, false));
            result.set(DataComponents.CONSUMABLE, Consumable.builder().consumeSeconds(1.6f).build());
            result.set(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStackTemplate(Items.BOWL))); // остаток — ОБЫЧНАЯ пустая миска
        }

        return result;
    }

    private void evaporateFailedBrew() {
        this.liquidLayers.clear();
        this.solidSlot.clear();
        this.brewing = null;
        this.setChanged();
        syncToClient();
    }

    private void spawnStageParticle(ServerLevel serverLevel, net.minecraft.core.particles.ParticleOptions particle) {
        serverLevel.sendParticles(
                particle,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.75, worldPosition.getZ() + 0.5,
                8, 0.02, 0.01, 0.15, 0.02
        );
    }

    /**
     * "Магический" фоновый эффект — endern-партиклы внутри котла, показывающий, что варится
     * что-то мистическое, независимо от наличия/отсутствия честной жидкостной текстуры.
     * Управляется флагом magic_effect в JSON (CauldronRecipe для обычных рецептов,
     * TotemProcessConfig для тотема) — по умолчанию выключен для рецептов, включён для тотема.
     */
    private void spawnMagicParticles(ServerLevel serverLevel) {
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.7, worldPosition.getZ() + 0.5,
                2, 0.25, 0.15, 0.25, 0.0);
    }

    public void tickBrewing() {
        if (brewing == null || !(level instanceof ServerLevel serverLevel)) return;

        if (brewing.soup()) {
            tickSoupBrewing(serverLevel);
            return;
        }

        CauldronRecipe recipe = CauldronRecipeLoader.get(brewing.recipeId());
        if (recipe == null) {
            this.brewing = null;
            return;
        }
        if (recipe.magicEffect() && level instanceof ServerLevel) {
            spawnMagicParticles((ServerLevel) level);
        }
        if (!recipe.conditions().isSatisfied(this) || !isRitualSatisfied(recipe)) {
            if (level instanceof ServerLevel && serverLevel.getGameTime() % 30 == 0) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5,
                        3, 0.25, 0.1, 0.25, 0.0);
            }
            return; // пауза — партиклы стадии не идут, время не тикает, но раз в секунду шлём angry_villager как сигнал
        }

        CauldronRecipeStage stage = recipe.stages().get(brewing.stageIndex());

        if (stage instanceof CauldronRecipeStage.CookStage cook) {
            cook.particle().ifPresent(p -> {
                if (serverLevel.getGameTime() % 5 == 0) spawnStageParticle(serverLevel, p);
            });
            int remaining = brewing.ticksRemaining() - 1;
            if (remaining <= 0) {
                advanceToNextStage(recipe);
            } else {
                this.brewing = new BrewingState(brewing.recipeId(), brewing.stageIndex(), remaining, 0,
                        brewing.composite(), brewing.roundsRemaining(), brewing.saltUsed(), brewing.spiceUsed(), false);
            }
            return;
        }

        if (stage instanceof CauldronRecipeStage.AwaitIngredientStage await) {
            tickIdlePhases(serverLevel, await.idlePhases(), false);
            return;
        }

        if (stage instanceof CauldronRecipeStage.CollectOpenStage collect) {
            tickIdlePhases(serverLevel, collect.idlePhases(), false);
        }
    }

    public void tickTotemBrewing() {
        if (totemBrewing == null || !(level instanceof ServerLevel serverLevel)) return;

        if (TotemProcessLoader.get().magicEffect()) {
            spawnMagicParticles(serverLevel);
        }

        if (totemBrewing.cooking()) {
            if (serverLevel.getGameTime() % 5 == 0) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.9, worldPosition.getZ() + 0.5,
                        3, 0.2, 0.1, 0.2, 0.0);
            }
            int remaining = totemBrewing.ticksRemaining() - 1;
            if (remaining <= 0) {
                this.totemBrewing = new TotemBrewingState(totemBrewing.material(), totemBrewing.level(), totemBrewing.stage(),
                        false, com.idk.biomegetter.block.custom.cauldron.data.totem.TotemProcessLoader.get().awaitTimeoutTicks(),
                        totemBrewing.ritualBudgetRemaining(), totemBrewing.collectedTags(), totemBrewing.forcedSkill(),
                        totemBrewing.chosenPassiveSkill(), totemBrewing.passiveStats(), totemBrewing.emptyStageCount());
            } else {
                this.totemBrewing = new TotemBrewingState(totemBrewing.material(), totemBrewing.level(), totemBrewing.stage(),
                        true, remaining,
                        totemBrewing.ritualBudgetRemaining(), totemBrewing.collectedTags(), totemBrewing.forcedSkill(),
                        totemBrewing.chosenPassiveSkill(), totemBrewing.passiveStats(), totemBrewing.emptyStageCount());
            }
            return;
        }

        // await-фаза: истечение таймаута = провал варки (аналог evaporateFailedBrew для обычных рецептов)
        int remaining = totemBrewing.ticksRemaining() - 1;
        if (remaining <= 0) {
            this.totemBrewing = null;
            this.setChanged();
            syncToClient();
        } else {
            this.totemBrewing = new TotemBrewingState(totemBrewing.material(), totemBrewing.level(), totemBrewing.stage(),
                    false, remaining,
                    totemBrewing.ritualBudgetRemaining(), totemBrewing.collectedTags(), totemBrewing.forcedSkill(),
                    totemBrewing.chosenPassiveSkill(), totemBrewing.passiveStats(), totemBrewing.emptyStageCount());
        }
    }

    private void tickSoupBrewing(ServerLevel serverLevel) {
        SoupProcessConfig config = CauldronSoupProcessLoader.get();

        if (brewing.stageIndex() == 0) { // cook
            config.cookParticle().ifPresent(p -> {
                if (serverLevel.getGameTime() % 5 == 0) spawnStageParticle(serverLevel, p);
            });
            int remaining = brewing.ticksRemaining() - 1;
            if (remaining <= 0) {
                int firstTicks = config.idlePhases().isEmpty() ? 0 : config.idlePhases().get(0).durationTicks();
                this.brewing = new BrewingState(brewing.recipeId(), 1, firstTicks, 0,
                        brewing.composite(), 0, brewing.saltUsed(), brewing.spiceUsed(), true);
                this.setChanged();
                syncToClient();
            } else {
                this.brewing = new BrewingState(brewing.recipeId(), 0, remaining, 0,
                        brewing.composite(), brewing.roundsRemaining(), brewing.saltUsed(), brewing.spiceUsed(), true);
            }
            return;
        }

        tickIdlePhases(serverLevel, config.idlePhases(), true);
    }

    /**
     * Общая логика "простоя в ожидании игрока" — партиклы текущей фазы, переход по фазам,
     * провал варки по истечении последней фазы без успеха.
     */
    private void tickIdlePhases(ServerLevel serverLevel, List<CauldronRecipeStage.IdlePhase> phases, boolean soup) {
        if (phases.isEmpty()) return;
        CauldronRecipeStage.IdlePhase phase = phases.get(brewing.idlePhaseIndex());
        phase.particle().ifPresent(p -> {
            if (serverLevel.getGameTime() % 5 == 0) spawnStageParticle(serverLevel, p);
        });

        int remaining = brewing.ticksRemaining() - 1;
        if (remaining > 0) {
            this.brewing = new BrewingState(brewing.recipeId(), brewing.stageIndex(), remaining, brewing.idlePhaseIndex(),
                    brewing.composite(), brewing.roundsRemaining(), brewing.saltUsed(), brewing.spiceUsed(), soup);
            return;
        }
        int nextPhase = brewing.idlePhaseIndex() + 1;
        if (nextPhase >= phases.size()) {
            evaporateFailedBrew();
        } else {
            this.brewing = new BrewingState(brewing.recipeId(), brewing.stageIndex(), phases.get(nextPhase).durationTicks(), nextPhase,
                    brewing.composite(), brewing.roundsRemaining(), brewing.saltUsed(), brewing.spiceUsed(), soup);
            this.setChanged();
            syncToClient();
        }
    }

    // ---- Персист ----

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("HasBlockAbove", this.hasBlockAbove);
        output.putBoolean("HeatedBelow", this.heatedBelow);
        output.putInt("EvaporationTimer", this.evaporationTimer);
        output.putBoolean("PowderSnowAbove", this.powderSnowAbove);
        output.putInt("MeltTimer", this.meltTimer);
        output.putInt("LightLevel", this.lightLevel);

        if (!this.liquidLayers.isEmpty()) {
            output.putString("LiquidLayers", encodeLiquidLayers(this.liquidLayers));
        }
        if (!this.solidSlot.isEmpty()) {
            output.putString("SolidSlot", encodeSolidSlot(this.solidSlot));
        }

        if (this.brewing != null) {
            output.putString("BrewRecipe", this.brewing.recipeId().toString());
            output.putInt("BrewStage", this.brewing.stageIndex());
            output.putInt("BrewTicks", this.brewing.ticksRemaining());
            output.putInt("BrewIdlePhase", this.brewing.idlePhaseIndex());

            if (!this.brewing.composite().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < this.brewing.composite().size(); i++) {
                    if (i > 0) sb.append(';');
                    sb.append(this.brewing.composite().get(i));
                }
                output.putString("BrewComposite", sb.toString());
            }
            output.putInt("BrewRounds", this.brewing.roundsRemaining());
            output.putBoolean("BrewSaltUsed", this.brewing.saltUsed());
            output.putBoolean("BrewSpiceUsed", this.brewing.spiceUsed());
            output.putBoolean("BrewSoup", this.brewing.soup());
        }
        // ---- Очередь "поглощения" блоков (см. BlockConsumptionRule) ----
        if (!this.consumptionQueue.isEmpty()) {
            output.putString("ConsumptionQueue", encodeConsumptionQueue(this.consumptionQueue));
        }
        output.putInt("ConsumptionCooldown", this.consumptionCooldown);
        if (this.consumptionAwaitingPlacementPos != null) {
            output.putString("ConsumptionAwaitingPos", encodePositions(List.of(this.consumptionAwaitingPlacementPos)));
        }
        if (this.consumptionAwaitingPlacementBlock != null) {
            output.putString("ConsumptionAwaitingBlock", this.consumptionAwaitingPlacementBlock.toString());
        }
        output.putInt("ConsumptionPlacementDelay", this.consumptionPlacementDelay);

    }

    private static String encodeConsumptionQueue(Iterable<ConsumptionEntry> entries) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ConsumptionEntry entry : entries) {
            if (!first) sb.append('|');
            sb.append(entry.pos().getX()).append(',').append(entry.pos().getY()).append(',').append(entry.pos().getZ())
                    .append(':').append(encodeIdentifiers(entry.pool()));
            first = false;
        }
        return sb.toString();
    }

    private List<ConsumptionEntry> decodeConsumptionQueue(String encoded) {
        List<ConsumptionEntry> result = new ArrayList<>();
        if (encoded.isEmpty()) return result;
        for (String token : encoded.split("\\|")) {
            if (token.isEmpty()) continue;
            String[] parts = token.split(":", 2);
            if (parts.length != 2) continue;
            String[] coords = parts[0].split(",");
            if (coords.length != 3) continue;
            BlockPos pos = new BlockPos(Integer.parseInt(coords[0]), Integer.parseInt(coords[1]), Integer.parseInt(coords[2]));
            result.add(new ConsumptionEntry(pos, decodeIdentifiers(parts[1])));
        }
        return result;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.hasBlockAbove = input.getBooleanOr("HasBlockAbove", false);
        this.heatedBelow = input.getBooleanOr("HeatedBelow", false);
        this.evaporationTimer = input.getIntOr("EvaporationTimer", EVAPORATION_INTERVAL_TICKS);
        this.powderSnowAbove = input.getBooleanOr("PowderSnowAbove", false);
        this.meltTimer = input.getIntOr("MeltTimer", MELT_INTERVAL_TICKS);
        this.lightLevel = input.getIntOr("LightLevel", 0);

        this.liquidLayers.clear();
        input.getString("LiquidLayers").ifPresent(encoded -> this.liquidLayers.addAll(decodeLiquidLayers(encoded)));

        this.solidSlot.clear();
        input.getString("SolidSlot").ifPresent(encoded -> this.solidSlot.addAll(decodeSolidSlot(encoded)));

        this.brewing = input.getString("BrewRecipe")
                .map(s -> {
                    List<Identifier> composite = new ArrayList<>();
                    input.getString("BrewComposite").ifPresent(enc -> {
                        for (String token : enc.split(";")) {
                            if (!token.isEmpty()) {
                                composite.add(Identifier.parse(token));
                            }
                        }
                    });
                    return new BrewingState(
                            Identifier.parse(s),
                            input.getIntOr("BrewStage", 0),
                            input.getIntOr("BrewTicks", 0),
                            input.getIntOr("BrewIdlePhase", 0),
                            composite,
                            input.getIntOr("BrewRounds", 0),
                            input.getBooleanOr("BrewSaltUsed", false),
                            input.getBooleanOr("BrewSpiceUsed", false),
                            input.getBooleanOr("BrewSoup", false)
                    );
                })
                .orElse(null);
        this.consumptionQueue.clear();
        input.getString("ConsumptionQueue").ifPresent(encoded -> this.consumptionQueue.addAll(decodeConsumptionQueue(encoded)));
        this.consumptionCooldown = input.getIntOr("ConsumptionCooldown", 0);
        this.consumptionAwaitingPlacementPos = input.getString("ConsumptionAwaitingPos")
                .map(this::decodePositions)
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
        this.consumptionAwaitingPlacementBlock = input.getString("ConsumptionAwaitingBlock")
                .map(Identifier::parse)
                .orElse(null);
        this.consumptionPlacementDelay = input.getIntOr("ConsumptionPlacementDelay", 0);
    }

    private static String encodeLiquidLayers(List<LiquidLayer> layers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < layers.size(); i++) {
            if (i > 0) sb.append(';');
            LiquidLayer layer = layers.get(i);
            if (layer instanceof LiquidLayer.Liquid liquid) {
                sb.append("liquid|").append(liquid.typeId());
            } else if (layer instanceof LiquidLayer.Soup soup) {
                sb.append(encodeSoup(soup.data()));
            }
        }
        return sb.toString();
    }

    private static String encodeSoup(SoupLiquidData data) {
        String nameB64 = Base64.getEncoder().encodeToString(data.displayName().getBytes(StandardCharsets.UTF_8));
        StringBuilder eff = new StringBuilder();
        for (int i = 0; i < data.effects().size(); i++) {
            if (i > 0) eff.append(',');
            MobEffectInstance inst = data.effects().get(i);
            Identifier effId = BuiltInRegistries.MOB_EFFECT.getKey(inst.getEffect().value());
            eff.append(effId).append(':').append(inst.getAmplifier()).append(':').append(inst.getDuration());
        }
        String triggersB64 = "";
        if (!data.eatTriggers().isEmpty()) {
            var encoded = SoupEatTrigger.CODEC.listOf().encodeStart(com.mojang.serialization.JsonOps.INSTANCE, data.eatTriggers());
            if (encoded.result().isPresent()) {
                triggersB64 = Base64.getEncoder().encodeToString(encoded.result().get().toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        return "soup|" + nameB64 + "|" + data.texture() + "|" + data.tint() + "|" + data.sustenance() + "|" + eff + "|" + triggersB64;
    }

    private static List<LiquidLayer> decodeLiquidLayers(String encoded) {
        List<LiquidLayer> result = new ArrayList<>();
        if (encoded.isEmpty()) return result;
        for (String token : encoded.split(";")) {
            if (token.isEmpty()) continue;

            if (token.startsWith("soup|")) {
                LiquidLayer decoded = decodeSoup(token);
                if (decoded instanceof LiquidLayer.Soup soup) {
                    result.add(soup);
                }
                continue;
            }

            String[] parts = token.split("\\|", 2);
            if (parts.length < 2) continue;
            if (parts[0].equals("liquid")) {
                result.add(new LiquidLayer.Liquid(Identifier.parse(parts[1])));
            }
        }
        return result;
    }

    @Nullable
    private static LiquidLayer decodeSoup(String token) {
        String[] parts = token.split("\\|", -1);
        if (parts.length < 6) return null;
        String displayName = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Identifier texture = Identifier.parse(parts[2]);
        int tint = Integer.parseInt(parts[3]);
        int sustenance = Integer.parseInt(parts[4]);

        List<MobEffectInstance> effects = new ArrayList<>();
        if (!parts[5].isEmpty()) {
            for (String effToken : parts[5].split(",")) {
                String[] effParts = effToken.split(":");
                if (effParts.length != 3) continue;
                Identifier effId = Identifier.parse(effParts[0]);
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(effId);
                if (effect == null) continue;
                int amplifier = Integer.parseInt(effParts[1]);
                int duration = Integer.parseInt(effParts[2]);
                effects.add(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), duration, amplifier));
            }
        }

        List<SoupEatTrigger> eatTriggers = List.of();
        if (parts.length >= 7 && !parts[6].isEmpty()) {
            String json = new String(Base64.getDecoder().decode(parts[6]), StandardCharsets.UTF_8);
            com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(json);
            eatTriggers = SoupEatTrigger.CODEC.listOf().parse(com.mojang.serialization.JsonOps.INSTANCE, element)
                    .result().orElse(List.of());
        }

        return new LiquidLayer.Soup(new SoupLiquidData(displayName, effects, sustenance, texture, tint, eatTriggers));
    }

    private static String encodeSolidSlot(List<SolidEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(';');
            sb.append(entries.get(i).typeId());
        }
        return sb.toString();
    }


    private static String encodePositions(Iterable<BlockPos> positions) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (BlockPos pos : positions) {
            if (!first) sb.append(';');
            sb.append(pos.getX()).append(',').append(pos.getY()).append(',').append(pos.getZ());
            first = false;
        }
        return sb.toString();
    }

    private List<BlockPos> decodePositions(String encoded) {
        List<BlockPos> result = new ArrayList<>();
        if (encoded.isEmpty()) return result;
        for (String token : encoded.split(";")) {
            if (token.isEmpty()) continue;
            String[] parts = token.split(",");
            if (parts.length != 3) continue;
            result.add(new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
        }
        return result;
    }

    private static String encodeIdentifiers(List<Identifier> ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append(';');
            sb.append(ids.get(i));
        }
        return sb.toString();
    }

    private List<Identifier> decodeIdentifiers(String encoded) {
        List<Identifier> result = new ArrayList<>();
        if (encoded.isEmpty()) return result;
        for (String token : encoded.split(";")) {
            if (!token.isEmpty()) result.add(Identifier.parse(token));
        }
        return result;
    }

    private static List<SolidEntry> decodeSolidSlot(String encoded) {
        List<SolidEntry> result = new ArrayList<>();
        if (encoded.isEmpty()) return result;
        for (String token : encoded.split(";")) {
            if (!token.isEmpty()) {
                result.add(new SolidEntry(Identifier.parse(token)));
            }
        }
        return result;
    }
}