package com.idk.biomegetter.fluid;

import com.idk.biomegetter.BiomeGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Регистрация физических жидкостей мода. ЧТОБЫ ДОБАВИТЬ НОВУЮ ЖИДКОСТЬ:
 * 1) скопировать блок как у ACID ниже, сменить только name;
 * 2) создать data/biomegetter/fluid_config/<name>.json (текстуры/вязкость/эффекты);
 * 3) в ModModelProvider.generateItemModels добавить generateFlatItem для нового bucket;
 * 4) в BiomeGetterCustomEntityClient.onInitializeClient зарегистрировать рендер (см. пример там);
 * 5) запустить DataGen.
 * <p>
 * Java-часть НЕ содержит поведенческих чисел — они все читаются из fluid_config на лету
 * (см. ConfigurableFluid) — здесь фиксируется только сама регистрация типов в movable.
 */
public class ModFluids {

    public static final FluidEntry ACID = registerFluid("acid");
    public static final FluidEntry GEYSER_GAS = registerFluid("geyser_gas");

    /**
     * Регистрирует связку Source/Flowing Fluid + Block + BucketItem под одним id. Поля Source и
     * Flowing узнают друг о друге ЛЕНИВО (через лямбды-Supplier) — обычный способ разорвать
     * циклическую зависимость "source ссылается на flowing и наоборот" при регистрации.
     */
    private static FluidEntry registerFluid(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name);

        ResourceKey<Fluid> stillKey = ResourceKey.create(Registries.FLUID, id.withSuffix("")); // "<name>"
        ResourceKey<Fluid> flowingKey = ResourceKey.create(Registries.FLUID, id.withSuffix("_flowing"));

        java.util.concurrent.atomic.AtomicReference<net.minecraft.world.level.material.FlowingFluid> stillRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<net.minecraft.world.level.material.FlowingFluid> flowingRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Block> blockRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Item> bucketRef = new java.util.concurrent.atomic.AtomicReference<>();

        ConfigurableFluid.Source source = new ConfigurableFluid.Source(
                id, stillRef::get, flowingRef::get, bucketRef::get, blockRef::get);
        ConfigurableFluid.Flowing flowing = new ConfigurableFluid.Flowing(
                id, stillRef::get, flowingRef::get, bucketRef::get, blockRef::get);

        ConfigurableFluid.Source registeredStill = Registry.register(BuiltInRegistries.FLUID, stillKey, source);
        ConfigurableFluid.Flowing registeredFlowing = Registry.register(BuiltInRegistries.FLUID, flowingKey, flowing);
        stillRef.set(registeredStill);
        flowingRef.set(registeredFlowing);

        Block block = Registry.register(BuiltInRegistries.BLOCK,
                Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name),
                new ConfigurableFluidBlock(registeredStill,
                        BlockBehaviour.Properties.of()
                                .mapColor(MapColor.WATER)
                                .replaceable()
                                .noCollision()
                                .strength(100.0F)
                                .pushReaction(PushReaction.DESTROY)
                                .sound(SoundType.EMPTY)
                                .noLootTable()
                                .liquid()
                                .setId(ResourceKey.create(Registries.BLOCK, id))));
        blockRef.set(block);

        Item bucket = Registry.register(BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name + "_bucket"),
                new BucketItem(registeredStill, new Item.Properties()
                        .craftRemainder(net.minecraft.world.item.Items.BUCKET)
                        .stacksTo(1)
                        .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name + "_bucket")))));
        bucketRef.set(bucket);

        return new FluidEntry(id, registeredStill, registeredFlowing, block, bucket);
    }

    /**
     * Готовая связка id/жидкость/блок/ведро — используйте {@code .bucket()} в
     * "pour_bucket_item"/"collect_bucket_item" ваших уже существующих liquid_component json
     * (см. следующее сообщение — там же обсудим, как связать эту физическую жидкость с
     * абстрактной "жидкостью котла").
     */
    public record FluidEntry(Identifier id, Fluid still, Fluid flowing, Block block, Item bucket) {
    }

    public static void registerModFluids() {
        BiomeGetter.LOGGER.info("Registered Mod Fluids for " + BiomeGetter.MOD_ID);
    }
}