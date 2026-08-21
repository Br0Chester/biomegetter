package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class CauldronLiquidComponentLoader extends SimpleJsonResourceReloadListener<LiquidComponentType>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, LiquidComponentType> REGISTRY = Map.of();

    public CauldronLiquidComponentLoader() {
        super(LiquidComponentType.CODEC, FileToIdConverter.json("liquid_component"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "liquid_component_loader");
    }

    @Override
    protected void apply(Map<Identifier, LiquidComponentType> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} liquid components", REGISTRY.size());
    }

    public static LiquidComponentType get(Identifier id) {
        return REGISTRY.get(id);
    }

    public static Identifier getIdByPourBucketItem(Item item) {
        for (Map.Entry<Identifier, LiquidComponentType> e : REGISTRY.entrySet()) {
            if (e.getValue().pourBucketItem().isPresent() && e.getValue().pourBucketItem().get() == item) {
                return e.getKey();
            }
        }
        return null;
    }
}