package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * Загружает JuiceType из data/biomegetter/juice_type/*.json — добавить новый сок = добавить файл.
 */
public class CauldronJuiceTypeLoader extends SimpleJsonResourceReloadListener<JuiceType>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, JuiceType> REGISTRY = Map.of();

    public CauldronJuiceTypeLoader() {
        super(JuiceType.CODEC, FileToIdConverter.json("juice_type"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "juice_type_loader");
    }

    @Override
    protected void apply(Map<Identifier, JuiceType> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} juice types", REGISTRY.size());
        BiomeGetter.LOGGER.info("Raw data keys: {}", data.keySet());
    }

    public static JuiceType get(Identifier id) {
        return REGISTRY.get(id);
    }
}
