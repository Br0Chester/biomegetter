package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.util.HexColorCodec;
import com.idk.biomegetter.util.ParticleCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Единые параметры процесса варки СУПА (одинаковые для любого бульона/сочетания
 * ингредиентов) — data/biomegetter/soup_process/*.json, ожидается ровно один файл в проекте.
 */
public record SoupProcessConfig(
        int cookDurationTicks,
        Optional<ParticleOptions> cookParticle,
        Identifier cookTexture,
        Optional<Integer> cookTint,
        List<CauldronRecipeStage.IdlePhase> idlePhases,
        List<ReactionEffect> completionEffects
) {
    public static final Codec<SoupProcessConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("cook_duration_ticks").forGetter(SoupProcessConfig::cookDurationTicks),
            ParticleCodec.CODEC.optionalFieldOf("cook_particle").forGetter(SoupProcessConfig::cookParticle),
            Identifier.CODEC.optionalFieldOf("cook_texture", Identifier.fromNamespaceAndPath("minecraft", "block/water_still")).forGetter(SoupProcessConfig::cookTexture),
            HexColorCodec.CODEC.optionalFieldOf("cook_tint").forGetter(SoupProcessConfig::cookTint),
            CauldronRecipeStage.IdlePhase.CODEC.listOf().fieldOf("idle_phases").forGetter(SoupProcessConfig::idlePhases),
            ReactionEffect.CODEC.listOf().optionalFieldOf("completion_effects", List.of()).forGetter(SoupProcessConfig::completionEffects)

    ).apply(instance, SoupProcessConfig::new));
}