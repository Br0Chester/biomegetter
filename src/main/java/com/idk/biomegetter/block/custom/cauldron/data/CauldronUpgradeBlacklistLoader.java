package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.BiomeGetter;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Загружает РОВНО ОДИН файл — data/biomegetter/unupgraded_cauldron_blacklist/config.json
 */
public class CauldronUpgradeBlacklistLoader implements IdentifiableResourceReloadListener {

    private static final Identifier CONFIG_PATH = Identifier.fromNamespaceAndPath(
            BiomeGetter.MOD_ID, "unupgraded_cauldron_blacklist/config.json");

    private static Set<String> BLACKLISTED_GROUPS = Set.of();
    private static Set<String> BLACKLISTED_TAGS = Set.of();

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "unupgraded_cauldron_blacklist_loader");
    }

    /**
     * Актуальная сигнатура PreparableReloadListener / IdentifiableResourceReloadListener
     * в твоей версии MC + Fabric API.
     */
    @Override
    public CompletableFuture<Void> reload(
            PreparableReloadListener.SharedState currentReload,
            Executor taskExecutor,
            PreparableReloadListener.PreparationBarrier preparationBarrier,
            Executor reloadExecutor
    ) {
        // Если resourceManager() не находится — смотри методы SharedState в IDE (getResourceManager / manager и т.п.)
        ResourceManager resourceManager = currentReload.resourceManager();

        return CompletableFuture
                .supplyAsync(() -> loadConfig(resourceManager), taskExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(config -> {
                    BLACKLISTED_GROUPS = Set.copyOf(config.groups());
                    BLACKLISTED_TAGS = Set.copyOf(config.tags());
                    BiomeGetter.LOGGER.info(
                            "Loaded cauldron upgrade blacklist: {} groups, {} tags",
                            BLACKLISTED_GROUPS.size(),
                            BLACKLISTED_TAGS.size()
                    );
                }, reloadExecutor);
    }

    private static CauldronUpgradeBlacklistConfig loadConfig(ResourceManager resourceManager) {
        var resourceOpt = resourceManager.getResource(CONFIG_PATH);
        if (resourceOpt.isEmpty()) {
            BiomeGetter.LOGGER.info("No unupgraded_cauldron_blacklist/config.json found — blacklist empty");
            return new CauldronUpgradeBlacklistConfig(List.of(), List.of());
        }
        Resource resource = resourceOpt.get();
        try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            com.google.gson.JsonElement json = com.google.gson.JsonParser.parseReader(reader);
            return CauldronUpgradeBlacklistConfig.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(BiomeGetter.LOGGER::error)
                    .orElse(new CauldronUpgradeBlacklistConfig(List.of(), List.of()));
        } catch (IOException e) {
            BiomeGetter.LOGGER.error("Failed to read unupgraded_cauldron_blacklist/config.json", e);
            return new CauldronUpgradeBlacklistConfig(List.of(), List.of());
        }
    }

    public static boolean isBlacklisted(LiquidComponentType type) {
        if (BLACKLISTED_GROUPS.contains(type.group())) return true;
        for (String tag : type.tags()) {
            if (BLACKLISTED_TAGS.contains(tag)) return true;
        }
        return false;
    }
}