package com.idk.biomegetter.block.custom.cauldron.data.totem;

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

/**
 * data/biomegetter/totem_liquid_component_level/*.json — id совпадает с liquid_component id.
 */
public class CauldronTotemLiquidLevelLoader extends SimpleJsonResourceReloadListener<TotemComponentLevel>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, TotemComponentLevel> REGISTRY = Map.of();

    public CauldronTotemLiquidLevelLoader() {
        super(TotemComponentLevel.CODEC, FileToIdConverter.json("totem_liquid_component_level"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "totem_liquid_component_level_loader");
    }

    @Override
    protected void apply(Map<Identifier, TotemComponentLevel> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} totem liquid component levels", REGISTRY.size());
    }

    @Nullable
    public static TotemComponentLevel get(Identifier id) {
        return REGISTRY.get(id);
    }
}