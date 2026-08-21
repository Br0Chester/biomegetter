package com.idk.biomegetter.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ParticleCodec {

    public static final Codec<ParticleOptions> CODEC = BuiltInRegistries.PARTICLE_TYPE.byNameCodec().comapFlatMap(
            type -> type instanceof SimpleParticleType simple
                    ? DataResult.success((ParticleOptions) simple)
                    : DataResult.error(() -> "Only simple (parameterless) particle types are supported here"),
            options -> (ParticleType<?>) options
    );

    private ParticleCodec() {
    }
}