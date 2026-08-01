package com.idk.biomegetter.entity.custom.mana;

import com.idk.biomegetter.entity.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Пул маны существа: текущий запас, регенерация, "прилив маны" при опустошении.
 * Инкапсулирует управление раскруткой — сущность-владелец просто дергает tick()/canAfford()/trySpend().
 */
public final class ManaPool {

    private static final String NBT_MANA = "Mana";
    private static final String NBT_SURGE_LOCKOUT = "ManaSurgeLockoutTicks";
    private static final int SURGE_LOCKOUT_TICKS = 40; // "во время прилива + 2 секунды после"

    private final LivingEntity owner;
    private double mana;
    private int surgeLockoutTicks = 0;

    public ManaPool(LivingEntity owner) {
        this.owner = owner;
        this.mana = this.getMaxMana();
    }

    public double getMaxMana() {
        return this.owner.getAttributeValue(ModAttributes.MAX_MANA);
    }

    public double getMana() {
        return this.mana;
    }

    public boolean isInSurge() {
        return this.surgeLockoutTicks > 0;
    }

    public boolean canAfford(double cost) {
        return !this.isInSurge() && this.mana >= cost;
    }

    /**
     * @return true, если списание удалось (маны хватило и не было прилива)
     */
    public boolean trySpend(double cost) {
        if (!this.canAfford(cost)) {
            return false;
        }
        this.mana -= cost;
        if (this.mana <= 0.0) {
            this.mana = 0.0;
            this.surgeLockoutTicks = SURGE_LOCKOUT_TICKS; // прилив маны — скиллы недоступны, но регенерация x3
        }
        return true;
    }

    public void tick() {
        if (this.surgeLockoutTicks > 0) {
            --this.surgeLockoutTicks;
        }
        double regenPerSecond = this.owner.getAttributeValue(ModAttributes.MANA_REGENERATION);
        double multiplier = this.isInSurge() ? 3.0 : 1.0;
        double regenPerTick = regenPerSecond * multiplier / 20.0;
        this.mana = Math.min(this.getMaxMana(), this.mana + regenPerTick);
    }

    public void save(ValueOutput output) {
        output.putDouble(NBT_MANA, this.mana);
        output.putInt(NBT_SURGE_LOCKOUT, this.surgeLockoutTicks);
    }

    public void load(ValueInput input) {
        this.mana = input.getDoubleOr(NBT_MANA, this.getMaxMana());
        this.surgeLockoutTicks = input.getIntOr(NBT_SURGE_LOCKOUT, 0);
    }
}