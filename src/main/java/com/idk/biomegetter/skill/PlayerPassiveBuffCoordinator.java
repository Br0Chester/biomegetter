package com.idk.biomegetter.skill;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

import java.util.Set;

/**
 * Раз в серверный тик, для каждого игрока: MobEffectBuff.apply УЖЕ вызвал markActive для всех
 * инфинити-эффектов от предметов, реально находящихся в руках в ЭТОМ тике (через
 * inventoryTick каждого ItemStack) — здесь просто нечего собирать заново, PassiveBuffTracker
 * сам хранит "что было отмечено с прошлого reconcile". Поэтому здесь нужен ДВУХФАЗНЫЙ подход:
 * это не считыватель "что должно быть активно" (это делает markActive), а финализатор
 * "снять то, что не подтвердилось". Регистрируется в BiomeGetter.onInitialize().
 */
public final class PlayerPassiveBuffCoordinator {

    private PlayerPassiveBuffCoordinator() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                Set<Holder<MobEffect>> confirmed = PassiveBuffTracker.consumeThisTick(player);
                PassiveBuffTracker.reconcile(player, confirmed);
            }
        });
    }
}