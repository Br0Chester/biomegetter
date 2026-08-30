package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class TotemProcessLoader extends SimpleJsonResourceReloadListener<TotemProcessConfig>
        implements IdentifiableResourceReloadListener {

    private static final TotemProcessConfig DEFAULT = new TotemProcessConfig(200, 600, true);
    private static TotemProcessConfig CONFIG = DEFAULT;

    public TotemProcessLoader() {
        super(TotemProcessConfig.CODEC, FileToIdConverter.json("totem_process"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "totem_process_loader");
    }

    @Override
    protected void apply(Map<Identifier, TotemProcessConfig> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        CONFIG = data.values().stream().findFirst().orElse(DEFAULT);
        BiomeGetter.LOGGER.info("Loaded totem process config: {}", data.isEmpty() ? "DEFAULT" : "custom");
    }

    public static TotemProcessConfig get() {
        return CONFIG;
    }
}