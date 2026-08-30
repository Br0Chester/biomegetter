package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CauldronTotemActiveSkillLoader extends SimpleJsonResourceReloadListener<TotemSkill>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, TotemSkill> REGISTRY = Map.of();

    public CauldronTotemActiveSkillLoader() {
        super(TotemSkill.CODEC, FileToIdConverter.json("totem_active_skill"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "totem_active_skill_loader");
    }

    @Override
    protected void apply(Map<Identifier, TotemSkill> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} totem active skills", REGISTRY.size());
    }

    public static TotemSkill get(Identifier id) {
        return REGISTRY.get(id);
    }

    public static List<Map.Entry<Identifier, TotemSkill>> byTag(String tag) {
        List<Map.Entry<Identifier, TotemSkill>> result = new ArrayList<>();
        for (Map.Entry<Identifier, TotemSkill> entry : REGISTRY.entrySet()) {
            if (entry.getValue().tags().contains(tag)) result.add(entry);
        }
        return result;
    }
}