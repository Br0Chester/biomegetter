package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CauldronIngredientEffectLoader extends SimpleJsonResourceReloadListener<IngredientEffectDef>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, IngredientEffectDef> REGISTRY = Map.of();

    public CauldronIngredientEffectLoader() {
        super(IngredientEffectDef.CODEC, FileToIdConverter.json("ingredient_effect"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "ingredient_effect_loader");
    }

    @Override
    protected void apply(Map<Identifier, IngredientEffectDef> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} ingredient effects", REGISTRY.size());
    }

    @Nullable
    public static IngredientEffectDef get(Identifier componentId) {
        return REGISTRY.get(componentId);
    }
}