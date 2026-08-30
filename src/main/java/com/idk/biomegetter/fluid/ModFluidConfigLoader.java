package com.idk.biomegetter.fluid;

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

public class ModFluidConfigLoader extends SimpleJsonResourceReloadListener<FluidConfig>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, FluidConfig> REGISTRY = Map.of();

    public ModFluidConfigLoader() {
        super(FluidConfig.CODEC, FileToIdConverter.json("fluid_config"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "fluid_config_loader");
    }

    @Override
    protected void apply(Map<Identifier, FluidConfig> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} fluid configs", REGISTRY.size());
    }

    @Nullable
    public static FluidConfig get(Identifier id) {
        return REGISTRY.get(id);
    }
}