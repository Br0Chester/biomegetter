package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CauldronSolidReactionLoader extends SimpleJsonResourceReloadListener<SolidReactionRule>
        implements IdentifiableResourceReloadListener {

    private static List<SolidReactionRule> RULES = List.of();

    public CauldronSolidReactionLoader() {
        super(SolidReactionRule.CODEC, FileToIdConverter.json("solid_reaction"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "solid_reaction_loader");
    }

    @Override
    protected void apply(Map<Identifier, SolidReactionRule> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        RULES = new ArrayList<>(data.values());
        BiomeGetter.LOGGER.info("Loaded {} solid reactions", RULES.size());
    }

    /**
     * Отсутствие правила = иммунитет (твёрдое не реагирует на эту жидкость).
     */
    @Nullable
    public static SolidReactionRule find(String liquidGroup, String solidGroup) {
        for (SolidReactionRule rule : RULES) {
            if (rule.liquidGroup().equals(liquidGroup) && rule.solidGroup().equals(solidGroup)) return rule;
        }
        return null;
    }
}