package com.idk.biomegetter.entity.custom.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;

public final class CombatUtils {

    private CombatUtils() {
    }

    /**
     * Считаем предметом-оружием мечи и топоры.
     * Список легко расширить (трезубцы, булавы и т.п.) добавлением instanceof-проверок ниже.
     */
    public static boolean isHoldingWeapon(Player player) {
        ItemStack mainHand = player.getMainHandItem();
//        mainHand.getItem() instanceof SwordItem ||
        return mainHand.getItem() instanceof AxeItem;
    }
}