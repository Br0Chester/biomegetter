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

public class CauldronLiquidReactionLoader extends SimpleJsonResourceReloadListener<LiquidReactionRule>
        implements IdentifiableResourceReloadListener {

    private static List<LiquidReactionRule> RULES = List.of();

    public CauldronLiquidReactionLoader() {
        super(LiquidReactionRule.CODEC, FileToIdConverter.json("liquid_reaction"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "liquid_reaction_loader");
    }

    @Override
    protected void apply(Map<Identifier, LiquidReactionRule> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        RULES = new ArrayList<>(data.values());
        BiomeGetter.LOGGER.info("Loaded {} liquid reactions", RULES.size());
    }

    /**
     * Ищет правило для пары групп, независимо от порядка (симметрично).
     */
    @Nullable
    public static LiquidReactionRule find(String groupA, String groupB) {
        for (LiquidReactionRule rule : RULES) {
            boolean direct = rule.groupA().equals(groupA) && rule.groupB().equals(groupB);
            boolean reversed = rule.groupA().equals(groupB) && rule.groupB().equals(groupA);
            if (direct || reversed) return rule;
        }
        return null;
    }
}