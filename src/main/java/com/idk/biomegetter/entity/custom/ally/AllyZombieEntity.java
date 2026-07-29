package com.idk.biomegetter.entity.custom.ally;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

public class AllyZombieEntity extends Zombie {

    public AllyZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return AllyMobs.isValidTarget(target) && super.canAttack(target);
    }

    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
        // намеренно ничего не делаем — союзная нежить не оставляет лут
    }
}