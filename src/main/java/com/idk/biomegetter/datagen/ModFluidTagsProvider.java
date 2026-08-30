package com.idk.biomegetter.datagen;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.fluid.ModFluids;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.FluidTags;

import java.util.concurrent.CompletableFuture;

/**
 * Регистрация физических жидкостей мода в ванильных fluid-тегах. #minecraft:water — ЕДИНСТВЕННЫЙ
 * способ получить полноценную "плавательную" физику (всплытие, поза плавания, доступ к
 * управлению вверх/вниз) — она жёстко завязана в LivingEntity именно на этот тег, а не на
 * какое-либо параметризуемое свойство жидкости. Побочные эффекты тега: тушит огонь, лодки
 * плавают, рыбы считают её своей средой, реагирует с лавой как вода (превращает в булыжник).
 * Если для конкретной жидкости эти побочные эффекты нежелательны — не добавляйте её сюда,
 * плавание для неё останется недоступным (см. обсуждение в чате).
 */
public class ModFluidTagsProvider extends FabricTagsProvider.FluidTagsProvider {

    public ModFluidTagsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registryLookupFuture) {
        super(output, registryLookupFuture);
    }

    public static void registerModFluidTagsProvider() {
        BiomeGetter.LOGGER.info("Registered Mod Fluid Tags for " + BiomeGetter.MOD_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        valueLookupBuilder(FluidTags.WATER)
                .add(ModFluids.ACID.still())
                .add(ModFluids.ACID.flowing())
                .add(ModFluids.GEYSER_GAS.still())
                .add(ModFluids.GEYSER_GAS.flowing());
    }
}