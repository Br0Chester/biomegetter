package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Загружает единственный конфиг процесса варки супа (data/biomegetter/soup_process/*.json).
 * Если файл отсутствует — используется жёстко зашитый дефолт (см. {@link #DEFAULT}), чтобы
 * система не падала на пустом датапаке.
 */
public class CauldronSoupProcessLoader extends SimpleJsonResourceReloadListener<SoupProcessConfig>
        implements IdentifiableResourceReloadListener {

    private static final SoupProcessConfig DEFAULT = new SoupProcessConfig(
            200,
            Optional.empty(),
            Identifier.fromNamespaceAndPath("minecraft", "block/water_still"),
            Optional.empty(),
            List.of(new CauldronRecipeStage.IdlePhase(200, Optional.empty())),
            List.of()
    );

    private static SoupProcessConfig CONFIG = DEFAULT;

    public CauldronSoupProcessLoader() {
        super(SoupProcessConfig.CODEC, FileToIdConverter.json("soup_process"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "soup_process_loader");
    }

    @Override
    protected void apply(Map<Identifier, SoupProcessConfig> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        CONFIG = data.values().stream().findFirst().orElse(DEFAULT);
        BiomeGetter.LOGGER.info("Loaded soup process config: {}", data.isEmpty() ? "DEFAULT (no file found)" : "custom");
    }

    public static SoupProcessConfig get() {
        return CONFIG;
    }
}