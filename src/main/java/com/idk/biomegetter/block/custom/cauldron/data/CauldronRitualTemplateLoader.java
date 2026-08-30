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

public class CauldronRitualTemplateLoader extends SimpleJsonResourceReloadListener<RitualTemplate>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, RitualTemplate> REGISTRY = Map.of();

    public CauldronRitualTemplateLoader() {
        super(RitualTemplate.CODEC, FileToIdConverter.json("template_rituals"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "template_ritual_loader");
    }

    @Override
    protected void apply(Map<Identifier, RitualTemplate> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} ritual templates", REGISTRY.size());
    }

    @Nullable
    public static RitualTemplate get(Identifier id) {
        return REGISTRY.get(id);
    }
}