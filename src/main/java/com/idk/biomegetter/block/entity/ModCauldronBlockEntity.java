package com.idk.biomegetter.block.entity;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.block.custom.cauldron.data.*;
import com.idk.biomegetter.block.custom.cauldron.data.spec_spices.BuffTransform;
import com.idk.biomegetter.block.custom.cauldron.data.spec_spices.SoupEatTrigger;
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
        if (!recipe.conditions().isSatisfied(this)) return;

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