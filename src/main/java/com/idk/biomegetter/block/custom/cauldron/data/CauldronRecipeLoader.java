package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.block.entity.ModCauldronBlockEntity;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class CauldronRecipeLoader extends SimpleJsonResourceReloadListener<CauldronRecipe>
        implements IdentifiableResourceReloadListener {

    private static Map<Identifier, CauldronRecipe> REGISTRY = Map.of();

    public CauldronRecipeLoader() {
        super(CauldronRecipe.CODEC, FileToIdConverter.json("cauldron_recipe"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "cauldron_recipe_loader");
    }

    @Override
    protected void apply(Map<Identifier, CauldronRecipe> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        REGISTRY = new HashMap<>(data);
        BiomeGetter.LOGGER.info("Loaded {} cauldron recipes", REGISTRY.size());
    }

    @Nullable
    public static CauldronRecipe get(Identifier id) {
        return REGISTRY.get(id);
    }

    /**
     * Ищет рецепт, чей trigger ТОЧНО совпадает с текущим составом обоих стеков котла
     * (без лишних предметов), и условия соблюдены прямо сейчас. Первый найденный по порядку
     * загрузки — если два рецепта описывают одинаковый триггер, это ошибка автора датапака,
     * поведение не гарантируется.
     */
    @Nullable
    public static Identifier findMatching(Level level, BlockPos pos, ModCauldronBlockEntity cauldron) {
        for (Map.Entry<Identifier, CauldronRecipe> entry : REGISTRY.entrySet()) {
            CauldronRecipe recipe = entry.getValue();
            if (!matchesExactly(cauldron, recipe.triggerLiquids(), recipe.triggerSolids())) continue;
            if (!recipe.conditions().isSatisfied(cauldron)) continue;
            if (recipe.requiredEntity().isPresent()
                    && findMatchingEntity(level, pos, recipe.requiredEntity().get().entityType()) == null) continue;
            return entry.getKey();
        }
        return null;
    }

    /**
     * Ищет ближайшую подходящую сущность — хитбокс которой пересекает либо зону самого котла,
     * либо блок непосредственно над ним (сущности часто стоят "на краю"/выпрыгивают, поэтому
     * зона над котлом тоже считается).
     */
    @Nullable
    public static Entity findMatchingEntity(Level level, BlockPos pos, EntityType<?> type) {
        AABB zone = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.above().getY() + 1, pos.getZ() + 1);
        List<Entity> found = level.getEntities((Entity) null, zone, e -> e.getType() == type);
        if (found.isEmpty()) return null;
        Vec3 center = Vec3.atCenterOf(pos);
        return found.stream().min(Comparator.comparingDouble(e -> e.position().distanceToSqr(center))).orElse(null);
    }

    /**
     * Точное совпадение (мультимножество, без лишнего) состава стеков с требованиями. Каждое
     * требование матчится либо по конкретному {@code type}, либо по ЛЮБОМУ предмету, несущему
     * метку {@code tag} (см. javadoc {@link IngredientRequirement}) — метка не связана с
     * физической совместимостью стека ({@code group}).
     */
    public static boolean matchesExactly(
            ModCauldronBlockEntity cauldron,
            java.util.List<IngredientRequirement> requiredLiquids,
            java.util.List<IngredientRequirement> requiredSolids
    ) {
        List<Identifier> actualLiquid = new ArrayList<>();
        for (var layer : cauldron.getLiquidLayers()) {
            if (layer instanceof ModCauldronBlockEntity.LiquidLayer.Liquid liquid) {
                actualLiquid.add(liquid.typeId());
            }
        }
        List<Identifier> actualSolid = new ArrayList<>();
        for (var entry : cauldron.getSolidSlot()) {
            actualSolid.add(entry.typeId());
        }
        return matchesRequirements(actualLiquid, requiredLiquids, CauldronRecipeLoader::liquidTagsOf)
                && matchesRequirements(actualSolid, requiredSolids, CauldronRecipeLoader::solidTagsOf);
    }

    private static boolean matchesRequirements(
            java.util.List<Identifier> actualIds, java.util.List<IngredientRequirement> required,
            java.util.function.Function<Identifier, java.util.List<String>> tagsOf
    ) {
        java.util.List<Identifier> remaining = new ArrayList<>(actualIds);
        for (IngredientRequirement req : required) {
            int matched = 0;
            Iterator<Identifier> it = remaining.iterator();
            while (it.hasNext() && matched < req.count()) {
                Identifier id = it.next();
                boolean matches = req.type().map(id::equals).orElse(false)
                        || req.tag().map(t -> tagsOf.apply(id).contains(t)).orElse(false);
                if (matches) {
                    it.remove();
                    matched++;
                }
            }
            if (matched != req.count()) return false;
        }
        return remaining.isEmpty();
    }

    private static java.util.List<String> liquidTagsOf(Identifier id) {
        LiquidComponentType type = CauldronLiquidComponentLoader.get(id);
        return type != null ? type.tags() : List.of();
    }

    private static java.util.List<String> solidTagsOf(Identifier id) {
        SolidComponentType type = CauldronSolidComponentLoader.get(id);
        return type != null ? type.tags() : List.of();
    }

    /**
     * Whitelist ингредиентов рецепта (поле {@code allowed_ingredients}). Пустой список = без
     * ограничений (совместимость со старыми рецептами, где поле не задано).
     */
    public static boolean isAllowedIngredient(CauldronRecipe recipe, Identifier componentId) {
        if (recipe.allowedIngredients().isEmpty()) return true;
        return recipe.allowedIngredients().contains(componentId);
    }
}