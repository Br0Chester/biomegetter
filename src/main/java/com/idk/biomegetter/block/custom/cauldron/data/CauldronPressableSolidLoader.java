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
 * Загружает PressableSolid из data/biomegetter/pressable_solid/*.json — добавить новую ягоду = добавить файл.
 */
public class CauldronPressableSolidLoader extends SimpleJsonResourceReloadListener<PressableSolid>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, PressableSolid> REGISTRY = Map.of();

    public CauldronPressableSolidLoader() {
        super(PressableSolid.CODEC, FileToIdConverter.json("biomegetter/pressable_solid"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "pressable_solid_loader");
    }

    @Override
    protected void apply(Map<Identifier, PressableSolid> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} pressable solids", REGISTRY.size());
    }

    /**
     * Найти запись по предмету в руке игрока (например, Items.SWEET_BERRIES).
     */
    public static PressableSolid getByItem(Item item) {
        return REGISTRY.values().stream()
                .filter(solid -> solid.item() == item)
                .findFirst()
                .orElse(null);
    }
}