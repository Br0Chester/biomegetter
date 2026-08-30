package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CauldronTotemStatRingLoader extends SimpleJsonResourceReloadListener<TotemStatRing>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, TotemStatRing> REGISTRY = Map.of();

    public CauldronTotemStatRingLoader() {
        super(TotemStatRing.CODEC, FileToIdConverter.json("template_totem_stat_ring"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "totem_stat_ring_loader");
    }

    @Override
    protected void apply(Map<Identifier, TotemStatRing> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} totem stat rings", REGISTRY.size());
    }

    public static Map<Identifier, TotemStatRing> all() {
        return REGISTRY;
    }

    public static List<Map.Entry<Identifier, TotemStatRing>> allSortedBySizeDesc() {
        // Отсортированы по убыванию площади (число ячеек паттерна, без клетки котла) — нужно
        // для алгоритма разрешения "каши" (крупные фигуры матчатся первыми, см. согласованный
        // принцип). Пересчитывается на каждый вызов — списки маленькие (десятки записей),
        // кешировать не критично.
        List<Map.Entry<Identifier, TotemStatRing>> list = new java.util.ArrayList<>(REGISTRY.entrySet());
        list.sort((a, b) -> Integer.compare(cellCount(b.getValue()), cellCount(a.getValue())));
        return list;
    }

    private static int cellCount(TotemStatRing ring) {
        return ring.template().toCanonicalCells().map(List::size).orElse(0);
    }
}