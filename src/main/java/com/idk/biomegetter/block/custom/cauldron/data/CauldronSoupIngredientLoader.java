package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Регистрация "обычных" ингредиентов супа (не приправ) — data/biomegetter/soup_ingredient/*.json.
 * Присутствие файла с id, совпадающим с id из solid_component/liquid_component — это и есть
 * whitelist: если файла нет, ингредиент физически можно положить в котёл (если он вообще
 * зарегистрирован как solid/liquid component), но во время сбора ингредиентов супа он не
 * будет допущен (см. ModCauldronBlockEntity#isIngredientAllowed).
 */
public class CauldronSoupIngredientLoader extends SimpleJsonResourceReloadListener<IngredientEffectDef>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, IngredientEffectDef> REGISTRY = Map.of();

    public CauldronSoupIngredientLoader() {
        super(IngredientEffectDef.CODEC, FileToIdConverter.json("soup_ingredient"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "soup_ingredient_loader");
    }

    @Override
    protected void apply(Map<Identifier, IngredientEffectDef> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} soup ingredients", REGISTRY.size());
    }

    @Nullable
    public static IngredientEffectDef get(Identifier componentId) {
        return REGISTRY.get(componentId);
    }
}