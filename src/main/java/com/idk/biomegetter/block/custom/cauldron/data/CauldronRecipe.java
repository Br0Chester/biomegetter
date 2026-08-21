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
        List<Identifier> allowedIngredients

) {
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
            Identifier.CODEC.listOf().optionalFieldOf("allowed_ingredients", List.of()).forGetter(CauldronRecipe::allowedIngredients)
    ).apply(instance, CauldronRecipe::new));
}