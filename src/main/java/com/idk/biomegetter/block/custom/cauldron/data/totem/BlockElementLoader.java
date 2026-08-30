package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * data/biomegetter/block_element/<element>.json — id файла = название элемента (то же имя,
 * что используется в ElementMatrixLoader). Один блок может числиться сразу в нескольких
 * файлах-элементах.
 */
public class BlockElementLoader extends SimpleJsonResourceReloadListener<BlockElementSet>
        implements IdentifiableResourceReloadListener {

    // блок -> множество элементов, к которым он принадлежит
    private static Map<Identifier, Set<String>> BLOCK_TO_ELEMENTS = Map.of();

    public BlockElementLoader() {
        super(BlockElementSet.CODEC, FileToIdConverter.json("block_element"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "block_element_loader");
    }

    @Override
    protected void apply(Map<Identifier, BlockElementSet> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, Set<String>> map = new HashMap<>();
        for (Map.Entry<Identifier, BlockElementSet> entry : data.entrySet()) {
            String element = entry.getKey().getPath(); // имя файла (без пути data/.../block_element/) = название элемента
            for (Identifier blockId : entry.getValue().blocks()) {
                map.computeIfAbsent(blockId, k -> new HashSet<>()).add(element);
            }
        }
        BLOCK_TO_ELEMENTS = map;
        BiomeGetter.LOGGER.info("Loaded block-element classification: {} distinct blocks classified", map.size());
    }

    public static Set<String> elementsOf(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return BLOCK_TO_ELEMENTS.getOrDefault(id, Set.of());
    }
}