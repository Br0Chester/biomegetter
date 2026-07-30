package com.idk.biomegetter.entity.custom.ally;

import com.idk.biomegetter.entity.effect.SummonEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

/**
 * Инкапсулирует ограниченное время жизни союзного существа: обратный отсчёт,
 * сохранение/загрузку через NBT и эффект молнии при исчезновении.
 * Используется композицией во всех Ally*-сущностях — у них разные ваниль-родители
 * (Zombie/Skeleton/WitherSkeleton), общий базовый класс недоступен, поэтому не наследование.
 */
public final class AllyLifetime {

    private static final String NBT_KEY = "AllyLifetimeTicks";

    private final Mob owner;
    private int ticksLeft = -1; // -1 = не установлено — существо живёт как обычный моб

    public AllyLifetime(Mob owner) {
        this.owner = owner;
    }

    public void set(int ticks) {
        this.ticksLeft = ticks;
    }

    /**
     * Вызывать из tick() владельца, только на сервере.
     */
    public void tick() {
        if (this.ticksLeft < 0) {
            return;
        }

        if (this.ticksLeft == 0) {
            this.expire();
            return;
        }

        --this.ticksLeft;
    }

    private void expire() {
        if (this.owner.level() instanceof ServerLevel serverLevel) {
            SummonEffects.playLightningCast(serverLevel, this.owner.getX(), this.owner.getY(), this.owner.getZ());
        }
        this.owner.discard();
    }

    public void save(CompoundTag tag) {
        tag.putInt(NBT_KEY, this.ticksLeft);
    }

    public void load(CompoundTag tag) {
        if (tag.contains(NBT_KEY)) {
            this.ticksLeft = tag.getInt(NBT_KEY).orElse(0);
        }
    }
}