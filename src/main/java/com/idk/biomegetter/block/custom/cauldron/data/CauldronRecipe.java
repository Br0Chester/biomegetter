package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.util.HexColorCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

public record CauldronRecipe(
        List<IngredientRequirement> triggerLiquids,
        List<IngredientRequirement> triggerSolids,
        RecipeCondition conditions,
        Identifier intermediateTexture,
        Optional<Integer> intermediateTint,
        Optional<Identifier> resultLiquid,
        int resultCount,
        Optional<EntityRequirement> requiredEntity,
        List<ReactionEffect> completionEffects,
        Optional<Integer> foamColor,
        List<CauldronRecipeStage> stages,
        List<Identifier> allowedIngredients,
        List<Identifier> ritual,
        List<BlockConsumptionRule> blockConsumption,
        boolean magicEffect

) {
    /**
     * "ritual" в JSON принимает либо один id (строка), либо массив id (комбинированный ритуал —
     * см. RitualTemplate#matchComposite: несколько фигур накладываются друг на друга в порядке
     * списка, последняя побеждает при совпадении позиций, вся структура вращается как одно
     * целое). Пустой список = условие не задано.
     */
    public static final Codec<List<Identifier>> RITUAL_CODEC = Codec.either(Identifier.CODEC, Identifier.CODEC.listOf())
            .xmap(
                    either -> either.map(List::of, list -> list),
                    list -> list.size() == 1
                            ? com.mojang.datafixers.util.Either.left(list.get(0))
                            : com.mojang.datafixers.util.Either.right(list)
            );

    public static final Codec<CauldronRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            IngredientRequirement.CODEC.listOf().optionalFieldOf("required_liquids", List.of()).forGetter(CauldronRecipe::triggerLiquids),
            IngredientRequirement.CODEC.listOf().optionalFieldOf("required_solids", List.of()).forGetter(CauldronRecipe::triggerSolids),
            RecipeCondition.CODEC.fieldOf("conditions").forGetter(CauldronRecipe::conditions),
            Identifier.CODEC.fieldOf("intermediate_texture").forGetter(CauldronRecipe::intermediateTexture),
            HexColorCodec.CODEC.optionalFieldOf("intermediate_tint").forGetter(CauldronRecipe::intermediateTint),
            Identifier.CODEC.optionalFieldOf("result_liquid").forGetter(CauldronRecipe::resultLiquid),
            Codec.INT.optionalFieldOf("result_count", 0).forGetter(CauldronRecipe::resultCount),
            EntityRequirement.CODEC.optionalFieldOf("required_entity").forGetter(CauldronRecipe::requiredEntity),
            ReactionEffect.CODEC.listOf().optionalFieldOf("completion_effects", List.of()).forGetter(CauldronRecipe::completionEffects),
            HexColorCodec.CODEC.optionalFieldOf("foam_color").forGetter(CauldronRecipe::foamColor),
            CauldronRecipeStage.CODEC.listOf().fieldOf("stages").forGetter(CauldronRecipe::stages),
            Identifier.CODEC.listOf().optionalFieldOf("allowed_ingredients", List.of()).forGetter(CauldronRecipe::allowedIngredients),
            RITUAL_CODEC.optionalFieldOf("ritual", List.of()).forGetter(CauldronRecipe::ritual),
            BlockConsumptionRule.CODEC.listOf().optionalFieldOf("block_consumption", List.of()).forGetter(CauldronRecipe::blockConsumption),
            Codec.BOOL.optionalFieldOf("magic_effect", false).forGetter(CauldronRecipe::magicEffect)
    ).apply(instance, CauldronRecipe::new));
}