package com.idk.biomegetter;

import com.idk.biomegetter.datagen.ModBlockTagsProvider;
import com.idk.biomegetter.datagen.ModLootTableProvider;
import com.idk.biomegetter.datagen.ModModelProvider;
import com.idk.biomegetter.datagen.ModRecepiesProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class BiomeGetterDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        var pack = fabricDataGenerator.createPack();

        pack.addProvider(ModModelProvider::new);
        pack.addProvider(ModBlockTagsProvider::new);
        pack.addProvider(ModLootTableProvider::new);
        pack.addProvider(ModRecepiesProvider::new);
    }
}
