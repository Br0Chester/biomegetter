package com.idk.biomegetter.entity.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;

/**
 * Визуальные и звуковые эффекты, сопровождающие способности сущностей мода.
 * Класс не содержит игровой логики (спавн мобов, урон, таргетинг) — только "показ".
 * Все методы статические и не хранят состояния между вызовами.
 */
public final class SummonEffects {

    private SummonEffects() {
        // утилитарный класс, экземпляры не нужны
    }

    /**
     * Проигрывает эффект "удара молнии" в указанной точке: визуальная молния
     * (без урона и поджога) + звук грома и удара.
     *
     * @param level серверный уровень, в котором нужно показать эффект
     * @param x     координата X
     * @param y     координата Y
     * @param z     координата Z
     */
    public static void playLightningCast(ServerLevel level, double x, double y, double z) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (lightning != null) {
            lightning.setPos(x, y, z);
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }

        level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 2.0F, 1.0F);
        level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.0F, 1.0F);
    }
}