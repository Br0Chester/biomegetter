package com.idk.biomegetter.block.custom;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.block.ModBlockEntities;
import com.idk.biomegetter.block.custom.cauldron.data.*;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Единственный класс нашего котла. Не хранит содержимое в blockstate — всё содержимое
 * (жидкостный и твёрдый стеки) живёт в {@link ModCauldronBlockEntity}.
 */
public class ModCauldronBlock extends AbstractCauldronBlock implements EntityBlock {

    private static final VoxelShape[] FILLED_SHAPES = Util.make(() -> Block.boxes(
            3, level -> Shapes.or(AbstractCauldronBlock.SHAPE, Block.column(12.0, 4.0, 6.0 + (level + 1) * 3.0))
    ));

    private static final MapCodec<ModCauldronBlock> CODEC = simpleCodec(ModCauldronBlock::new);

    /**
     * Заглушка для соли — до появления отдельного предмета в моде. Соль — обычный
     * solid_component (см. data/biomegetter/solid_component/salt.json, input_item —
     * дубовая кнопка), но во время сбора ингредиентов супа (isCollectingIngredients())
     * перехватывается ЗДЕСЬ раньше общего пункта 6 и действует мгновенно (как разовая
     * специя), не попадая в твёрдый стек.
     */
    private static final Item SALT_ITEM = Items.OAK_BUTTON;

    public ModCauldronBlock(Properties properties) {
        super(properties, new CauldronInteraction.Dispatcher()); // не используется — весь интеракт свой, ниже
        this.registerDefaultState(this.stateDefinition.any());
    }

    @Override
    public MapCodec<ModCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // Нет собственных blockstate-свойств — весь фактический контент хранится в BlockEntity
    }

    @Override
    public boolean isFull(BlockState state) {
        return false;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return 0.0;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int solidCount = level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron ? cauldron.getSolidCount() : 0;
        if (solidCount == 0) {
            return AbstractCauldronBlock.SHAPE;
        }
        return Shapes.or(AbstractCauldronBlock.SHAPE, Block.column(12.0, 4.0, 6.0 + solidCount * 3.0));
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        int liquidCount = level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron ? cauldron.getLiquidCount() : 0;
        if (liquidCount == 0) {
            return Shapes.empty();
        }
        return FILLED_SHAPES[liquidCount - 1];
    }

    // ---- Взаимодействия предметом ----

    @Override
    protected InteractionResult useItemOn(
            ItemStack itemStack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (cauldron.isCooking()) {
            return InteractionResult.CONSUME; // варится — никакое взаимодействие не проходит
        }

        Item item = itemStack.getItem();

        // 0. Деревянная миска — забор ГОТОВОГО СУПА по трети (аналогично ведру для обычной
        //    жидкости): верхний слой должен быть LiquidLayer.Soup. ВАЖНО: миска должна быть
        //    именно ПУСТОЙ (без компонентов) — иначе уже наполненная суп-миска тоже проходила
        //    бы проверку "item == Items.BOWL" (тип предмета у нашего супа тот же Items.BOWL,
        //    просто с навешенными компонентами) и позволяла бесконечно зачерпывать котёл, не
        //    нуждаясь в пустой миске вообще.
        if (item == Items.BOWL && itemStack.getComponentsPatch().isEmpty()
                && cauldron.getTopLiquidLayer() instanceof ModCauldronBlockEntity.LiquidLayer.Soup soup) {
            if (!level.isClientSide()) {
                cauldron.removeTopLiquidLayer();
                ItemStack soupStack = ModCauldronBlockEntity.toItemStack(soup.data());
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_FALL, SoundSource.BLOCKS, 1.0F, 1.0F); // TODO: подобрать звук под суп
                swapItem(player, hand, itemStack, soupStack);
            }
            return InteractionResult.SUCCESS;
        }

        // 1. Пустое ведро — забор содержимого, только если ровно 3/3 ОДНОГО типа и это НЕ суп
        //    (суп забирается только миской, см. пункт 0). Сначала жидкость, затем твёрдое.
        if (item == Items.BUCKET) {
            if (!(cauldron.getTopLiquidLayer() instanceof ModCauldronBlockEntity.LiquidLayer.Soup) && cauldron.isLiquidUniformFull()) {
                ModCauldronBlockEntity.LiquidLayer top = cauldron.getTopLiquidLayer();
                if (!level.isClientSide()) {
                    cauldron.takeAllLiquid();
                    if (top instanceof ModCauldronBlockEntity.LiquidLayer.Liquid liquid) {
                        LiquidComponentType type = CauldronLiquidComponentLoader.get(liquid.typeId());
                        level.playSound(null, pos, type.emptySound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                        swapItem(player, hand, itemStack, new ItemStack(type.collectBucketItem()));
                    } else {
                        level.playSound(null, pos, SoundEvents.BUCKET_FILL_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
                        swapItem(player, hand, itemStack, new ItemStack(Items.LAVA_BUCKET));
                    }
                }
                return InteractionResult.SUCCESS;
            }
            // не 3/3 одного типа (или это суп) — ведро не срабатывает, падаем в TRY_WITH_EMPTY_HAND
        }

        // 2. Наполнение ведром — реактивно (см. applyLiquidContact в BE). Whitelist
        //    (isIngredientAllowed) действует только во время сбора ингредиентов супа.
        Identifier liquidTypeId = CauldronLiquidComponentLoader.getIdByPourBucketItem(item);
        if (liquidTypeId != null && cauldron.isIngredientAllowed(liquidTypeId)) {
            LiquidComponentType type = CauldronLiquidComponentLoader.get(liquidTypeId);
            if (!level.isClientSide()) {
                int burned = cauldron.applyLiquidContact(liquidTypeId, 3);
                level.playSound(null, pos, type.fillSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                swapItem(player, hand, itemStack, new ItemStack(Items.BUCKET));
                dropCobblestone(level, pos, burned);
            }
            return InteractionResult.SUCCESS;
        }

        if (item == Items.LAVA_BUCKET) {
            if (!level.isClientSide()) {
                int burned = cauldron.applyLiquidContact(BiomeGetter.id("lava"), 3);
                level.playSound(null, pos, burned > 0 ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
                swapItem(player, hand, itemStack, new ItemStack(Items.BUCKET));
                dropCobblestone(level, pos, burned);
            }
            return InteractionResult.SUCCESS;
        }

        // 3. Пустая бутылка — забирает верхний слой любой ОБЫЧНОЙ жидкости (не суп), без реакции
        if (item == Items.GLASS_BOTTLE
                && cauldron.getTopLiquidLayer() instanceof ModCauldronBlockEntity.LiquidLayer.Liquid liquid) {
            LiquidComponentType type = CauldronLiquidComponentLoader.get(liquid.typeId());
            if (!level.isClientSide()) {
                cauldron.removeTopLiquidLayer();
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                Item resultItem = type != null ? type.collectBottleItem().orElse(null) : null;
                ItemStack result = resultItem != null
                        ? new ItemStack(resultItem)
                        : PotionContents.createItemStack(Items.POTION, Potions.WATER);
                swapItem(player, hand, itemStack, result);
            }
            return InteractionResult.SUCCESS;
        }

        // 3b. Бутылка воды — наливает 1 слой воды (реактивно, как и вёдра). Whitelist
        //     действует только во время сбора ингредиентов супа.
        if (isWaterBottle(itemStack) && cauldron.isIngredientAllowed(ModCauldronBlockEntity.WATER_COMPONENT_ID)) {
            if (!level.isClientSide()) {
                int burned = cauldron.applyLiquidContact(BiomeGetter.id("water"), 1);
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                swapItem(player, hand, itemStack, new ItemStack(Items.GLASS_BOTTLE));
                dropCobblestone(level, pos, burned);
            }
            return InteractionResult.SUCCESS;
        }

        // 4. Снятие красителя с кожаных вещей — только если верхний слой это чистая вода
        if (itemStack.get(DataComponents.DYED_COLOR) != null
                && cauldron.getTopLiquidLayer() instanceof ModCauldronBlockEntity.LiquidLayer.Liquid liquid
                && liquid.typeId().equals(ModCauldronBlockEntity.WATER_COMPONENT_ID)) {
            if (!level.isClientSide()) {
                itemStack.remove(DataComponents.DYED_COLOR);
                cauldron.removeTopLiquidLayer();
                level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }

        // 5. Соль — только во время сбора ингредиентов супа: срабатывает мгновенно, НЕ ложится
        //    в твёрдый стек. Вне этого контекста проваливается ниже и обрабатывается как
        //    обычный solid_component (пункт 6, см. data/biomegetter/solid_component/salt.json).
        if (item == SALT_ITEM && cauldron.isCollectingIngredients()) {
            if (!level.isClientSide()) {
                if (cauldron.useSalt()) {
                    level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F); // TODO: подобрать звук под соль
                    if (!player.getAbilities().instabuild) itemStack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // 5b. Специя — только во время сбора ингредиентов супа, 1 раз за варку. Список специй —
        //     data/biomegetter/soup_spice/*.json (см. CauldronSoupSpiceLoader).
        if (cauldron.isCollectingIngredients()) {
            Identifier spiceId = CauldronSoupSpiceLoader.getIdByInputItem(item);
            if (spiceId != null) {
                if (!level.isClientSide()) {
                    boolean used = cauldron.useSpice(spiceId);
                    BiomeGetter.LOGGER.info("Cauldron soup: spice {} matched item {}, useSpice()={}", spiceId, item, used);
                    if (used) {
                        level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F); // TODO: подобрать звук под специю
                        if (!player.getAbilities().instabuild) itemStack.shrink(1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }

        // 6. Универсальный обработчик твёрдых компонентов — полностью датапак-driven.
        //    Whitelist (isIngredientAllowed) действует только во время сбора ингредиентов супа.
        Identifier solidTypeId = CauldronSolidComponentLoader.getIdByInputItem(item);
        if (solidTypeId != null && cauldron.isIngredientAllowed(solidTypeId)) {
            SolidComponentType type = CauldronSolidComponentLoader.get(solidTypeId);
            if (type != null && itemStack.getCount() >= type.inputCount()) {
                boolean handled = tryAddSolidOrRejectBurning(level, pos, cauldron, type.group(), () -> {
                    cauldron.addSolid(solidTypeId);
                    level.playSound(null, pos, type.placeSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (type.returnsEmptyBucket()) {
                        swapItem(player, hand, itemStack, new ItemStack(Items.BUCKET));
                    } else {
                        itemStack.shrink(type.inputCount());
                    }
                });
                if (handled) return InteractionResult.SUCCESS;
            }
        }
//        else if (cauldron.isCollectingIngredients()) {
//            // Диагностика: во время сбора ингредиентов супа предмет не принят — логируем ПОЧЕМУ,
//            // чтобы не гадать между "не зарегистрирован как solid_component вообще" и
//            // "зарегистрирован, но нет соответствующего soup_ingredient".
//            if (solidTypeId == null) {
//                BiomeGetter.LOGGER.info("Cauldron soup: item {} not registered as solid_component at all", item);
//            } else {
//                BiomeGetter.LOGGER.info("Cauldron soup: component {} registered, but not allowed as soup_ingredient (isIngredientAllowed=false)", solidTypeId);
//            }
//        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static boolean tryAddSolidOrRejectBurning(
            Level level, BlockPos pos, ModCauldronBlockEntity cauldron, String solidGroup, Runnable addAction
    ) {
        if (cauldron.canAddSolid(solidGroup)) {
            if (!level.isClientSide()) addAction.run();
            return true;
        }
        if (cauldron.reactsWithCurrentLiquid(solidGroup)) {
            if (!level.isClientSide()) {
                level.playSound(null, pos, SoundEvents.GENERIC_BURN, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        return false;
    }

    private static void swapItem(Player player, InteractionHand hand, ItemStack original, ItemStack result) {
        if (!player.getAbilities().instabuild) {
            original.shrink(1);
            if (original.isEmpty()) {
                player.setItemInHand(hand, result);
            } else if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }
    }

    private static void dropCobblestone(Level level, BlockPos pos, int count) {
        for (int i = 0; i < count; i++) {
            popResource(level, pos, new ItemStack(Items.COBBLESTONE));
        }
    }

    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }

    // ---- Взаимодействие пустой рукой: Shift+ПКМ / перемешивание ----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron)) {
            return InteractionResult.PASS;
        }
        if (cauldron.isCooking()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            ItemStack extracted = cauldron.extractTopSolid();
            if (extracted == null) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide()) {
                level.playSound(null, pos, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (cauldron.hasBlockAbove()) {
            return InteractionResult.PASS;
        }
        ItemStack heldItem = player.getMainHandItem();
        if (!isStirringTool(heldItem)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            if (cauldron.isCooking()) {
                // варится автоматически — мешать нечего
            } else if (cauldron.isAwaitingIngredient()) {
                cauldron.tryAdvanceCurrentStage();
            } else if (cauldron.canStartSoup()) {
                cauldron.startSoupBrewing();
            } else {
                logStirredState(pos, cauldron);
                Identifier recipeId = CauldronRecipeLoader.findMatching(level, pos, cauldron);
                if (recipeId != null) {
                    CauldronRecipe recipe = CauldronRecipeLoader.get(recipeId);
                    Entity consumedEntity = recipe.requiredEntity().isPresent()
                            ? CauldronRecipeLoader.findMatchingEntity(level, pos, recipe.requiredEntity().get().entityType())
                            : null;
                    cauldron.startBrewing(recipeId, consumedEntity);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean isStirringTool(ItemStack stack) {
        return stack.is(Items.STICK)
                || stack.is(ItemTags.SAPLINGS)
                || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES)
                || stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.SPEARS);
    }

    private static void logStirredState(BlockPos pos, ModCauldronBlockEntity cauldron) {
        BiomeGetter.LOGGER.info(
                "Cauldron stirred at {}: liquid_layers={}, solid_entries={}, heated={}, light={}",
                pos,
                cauldron.getLiquidLayers(),
                cauldron.getSolidSlot(),
                cauldron.isHeatedBelow(),
                cauldron.getLightLevel()
        );
    }

    // ---- Разрушение блока ----

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron) {
            for (ItemStack drop : cauldron.drainAllSolids()) {
                popResource(level, pos, drop);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ---- Дождь / снегопад ----

    private static boolean shouldHandlePrecipitation(final Level level, final Biome.Precipitation precipitation) {
        if (precipitation == Biome.Precipitation.RAIN) {
            return level.getRandom().nextFloat() < 0.05F;
        } else {
            return precipitation == Biome.Precipitation.SNOW ? level.getRandom().nextFloat() < 0.1F : false;
        }
    }

    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        if (!shouldHandlePrecipitation(level, precipitation)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron)) {
            return;
        }

        if (precipitation == Biome.Precipitation.RAIN) {
            int burned = cauldron.applyLiquidContact(BiomeGetter.id("water"), 1);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
            dropCobblestone(level, pos, burned);
        } else if (precipitation == Biome.Precipitation.SNOW) {
            SolidComponentType snowType = CauldronSolidComponentLoader.get(ModCauldronBlockEntity.SNOW_COMPONENT_ID);
            if (snowType != null && cauldron.canAddSolid(snowType.group())) {
                cauldron.addSolid(ModCauldronBlockEntity.SNOW_COMPONENT_ID);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
            }
        }
    }

    // ---- Дрипстон ----

    @Override
    protected boolean canReceiveStalactiteDrip(Fluid fluid) {
        return fluid == Fluids.WATER || fluid == Fluids.LAVA;
    }

    @Override
    protected void receiveStalactiteDrip(BlockState state, Level level, BlockPos pos, Fluid fluid) {
        if (!(level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron)) {
            return;
        }

        int burned;
        if (fluid == Fluids.LAVA) {
            burned = cauldron.applyLiquidContact(BiomeGetter.id("lava"), 3);
        } else {
            burned = cauldron.applyLiquidContact(BiomeGetter.id("water"), 1);
        }
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(state));
        level.levelEvent(1047, pos, 0);
        dropCobblestone(level, pos, burned);
    }

    // ---- Кипение/партиклы ----

    @Override
    protected void entityInside(
            BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise
    ) {
        if (!(level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron) || cauldron.getLiquidCount() == 0) {
            return;
        }
        if (cauldron.isHeatedBelow() && level instanceof ServerLevel serverLevel) {
            entity.hurtServer(serverLevel, level.damageSources().hotFloor(), 2.0F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron) || cauldron.getLiquidCount() == 0) {
            return;
        }
        if (cauldron.isHeatedBelow()) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.9;
            double z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.BUBBLE_POP, x + random.nextDouble() * 0.6 - 0.3, y, z + random.nextDouble() * 0.6 - 0.3, 0.0, 0.05, 0.0);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block
            neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron) {
            BlockState above = level.getBlockState(pos.above());
            cauldron.setHasBlockAbove(!above.isAir());
            cauldron.setPowderSnowAbove(above.is(Blocks.POWDER_SNOW));
            cauldron.setHeatedBelow(isHeatSource(level.getBlockState(pos.below())));
        }
    }

    private static boolean isHeatSource(BlockState below) {
        return below.is(BlockTags.FIRE)
                || below.is(Blocks.LAVA)
                || below.is(Blocks.MAGMA_BLOCK)
                || (below.getBlock() instanceof CampfireBlock && below.getValue(CampfireBlock.LIT));
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (level.getBlockEntity(pos) instanceof ModCauldronBlockEntity cauldron
                && cauldron.getSolidCount() > 0 && fallDistance > 0.5) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                ModCauldronBlockEntity.PressOutcome outcome = cauldron.pressTopComponent();
                if (outcome.pressed()) {
                    popResource(level, pos, new ItemStack(Items.SUGAR));
                    dropCobblestone(level, pos, outcome.burnedThirds());
                    serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.05);
                    level.playSound(null, pos, SoundEvents.HONEY_BLOCK_FALL, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
            entity.causeFallDamage((float) fallDistance, 1.0F, entity.damageSources().fall());
            return;
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    @Nullable
    @Override
    public <T extends
            BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof ModCauldronBlockEntity cauldron) {
                serverTick(lvl, pos, st, cauldron);
            }
        };
    }

    private static void serverTick(Level level, BlockPos pos, BlockState state, ModCauldronBlockEntity cauldron) {
        cauldron.updateLightSource();

        cauldron.setHeatedBelow(isHeatSource(level.getBlockState(pos.below())));
        BlockState above = level.getBlockState(pos.above());
        cauldron.setHasBlockAbove(!above.isAir());
        cauldron.setPowderSnowAbove(above.is(Blocks.POWDER_SNOW));
        cauldron.setLightLevel(level.getMaxLocalRawBrightness(pos));

        cauldron.tickBrewing();

        if (cauldron.isHeatedBelow() && cauldron.isPowderSnowAbove() && cauldron.tickMelt()) {
            level.setBlockAndUpdate(pos.above(), Blocks.WATER.defaultBlockState());
        }

        if (!cauldron.isHeatedBelow()) {
            return;
        }

        int burned = cauldron.meltSnow();
        dropCobblestone(level, pos, burned);

        // Испарение верхнего слоя жидкости при кипении без крышки (обычное "усушка", не связано
        // с лавой) — ГОТОВЫЙ СУП (LiquidLayer.Soup) из этого механизма исключён по вашей просьбе:
        // сваренный суп не должен "усыхать" сам по себе.
        if (cauldron.getLiquidCount() > 0 && !cauldron.hasBlockAbove()
                && !(cauldron.getTopLiquidLayer() instanceof ModCauldronBlockEntity.LiquidLayer.Soup)
                && cauldron.tickEvaporation()) {
            cauldron.removeTopLiquidLayer();
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModCauldronBlockEntity(ModBlockEntities.CAULDRON, pos, state);
    }
}