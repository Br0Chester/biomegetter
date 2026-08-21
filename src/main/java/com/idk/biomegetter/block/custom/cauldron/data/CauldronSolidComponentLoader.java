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

/**
 * Загружает SolidComponentType из data/biomegetter/solid_component/*.json — добавить новый
 * твёрдый предмет для котла (ягоду, снег, самородки, что угодно ещё) = добавить файл,
 * без изменений в Java.
 */
public class CauldronSolidComponentLoader extends SimpleJsonResourceReloadListener<SolidComponentType>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, SolidComponentType> REGISTRY = Map.of();

    public CauldronSolidComponentLoader() {
        super(SolidComponentType.CODEC, FileToIdConverter.json("solid_component"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "solid_component_loader");
    }

    @Override
    protected void apply(Map<Identifier, SolidComponentType> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} solid components", REGISTRY.size());
        BiomeGetter.LOGGER.info("Raw data keys: {}", data.keySet());
    }

    public static SolidComponentType get(Identifier id) {
        return REGISTRY.get(id);
    }

    /**
     * Найти id записи по предмету, которым игрок кликнул по котлу (например, Items.SWEET_BERRIES,
     * или Items.IRON_NUGGET, или Items.POWDER_SNOW_BUCKET).
     */
    public static Identifier getIdByInputItem(Item item) {
        for (Map.Entry<Identifier, SolidComponentType> entry : REGISTRY.entrySet()) {
            if (entry.getValue().inputItem() == item) {
                return entry.getKey();
            }
        }
        return null;
    }
}
