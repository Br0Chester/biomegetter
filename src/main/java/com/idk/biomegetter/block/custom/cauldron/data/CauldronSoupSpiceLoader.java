package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CauldronSoupSpiceLoader extends SimpleJsonResourceReloadListener<SoupSpiceDef>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, SoupSpiceDef> REGISTRY = Map.of();

    public CauldronSoupSpiceLoader() {
        super(SoupSpiceDef.CODEC, FileToIdConverter.json("soup_spice"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "soup_spice_loader");
    }

    @Override
    protected void apply(Map<Identifier, SoupSpiceDef> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} soup spices", REGISTRY.size());
    }

    @Nullable
    public static SoupSpiceDef get(Identifier id) {
        return REGISTRY.get(id);
    }

    @Nullable
    public static Identifier getIdByInputItem(Item item) {
        for (Map.Entry<Identifier, SoupSpiceDef> entry : REGISTRY.entrySet()) {
            if (entry.getValue().inputItem() == item) return entry.getKey();
        }
        return null;
    }
}