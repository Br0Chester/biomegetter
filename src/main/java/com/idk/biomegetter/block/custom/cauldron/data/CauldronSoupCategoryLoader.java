package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.*;

public class CauldronSoupCategoryLoader extends SimpleJsonResourceReloadListener<SoupCategoryRule>
        implements IdentifiableResourceReloadListener {

    private static List<SoupCategoryRule> RULES = List.of();

    /**
     * Запасное правило, если ни одно из датапак-правил не подошло (пустых requires_categories
     * тоже нет) — суп без текстуры/тинта из категории, рендерер сам подставит FALLBACK_TEXTURE.
     */
    private static final SoupCategoryRule DEFAULT_RULE =
            new SoupCategoryRule(List.of(), Integer.MAX_VALUE, "Suspicious Stew", Optional.empty(), Optional.empty());

    public CauldronSoupCategoryLoader() {
        super(SoupCategoryRule.CODEC, FileToIdConverter.json("soup_category"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "soup_category_loader");
    }

    @Override
    protected void apply(Map<Identifier, SoupCategoryRule> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        RULES = new ArrayList<>(data.values());
        BiomeGetter.LOGGER.info("Loaded {} soup categories", RULES.size());
    }

    public static SoupCategoryRule pickRule(Set<String> presentCategories) {
        return RULES.stream()
                .filter(rule -> presentCategories.containsAll(rule.requiresCategories()))
                .min(Comparator.comparingInt(SoupCategoryRule::priority))
                .orElse(DEFAULT_RULE);
    }

    public static String pickDisplayName(Set<String> presentCategories) {
        return pickRule(presentCategories).displayName();
    }
}