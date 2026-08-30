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

public class CauldronTotemStartRingLoader extends SimpleJsonResourceReloadListener<TotemStartRing>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, TotemStartRing> REGISTRY = Map.of();

    public CauldronTotemStartRingLoader() {
        super(TotemStartRing.CODEC, FileToIdConverter.json("template_totem_start_ring"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "totem_start_ring_loader");
    }

    @Override
    protected void apply(Map<Identifier, TotemStartRing> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} totem start rings", REGISTRY.size());
    }

    @Nullable
    public static TotemStartRing get(Identifier id) {
        return REGISTRY.get(id);
    }

    public static Map<Identifier, TotemStartRing> all() {
        return REGISTRY;
    }
}