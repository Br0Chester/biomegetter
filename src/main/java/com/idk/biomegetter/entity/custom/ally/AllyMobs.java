package com.idk.biomegetter.entity.custom.ally;

import com.idk.biomegetter.entity.custom.UnicornEntity;
import net.minecraft.world.entity.LivingEntity;

public final class AllyMobs {

    private AllyMobs() {
        // утилитарный класс, экземпляры не нужны
    }

    public static boolean isValidTarget(LivingEntity target) {
        return !(target instanceof UnicornEntity);
    }
}