package com.idk.biomegetter.entity.custom.state;

/**
 * Боевое состояние юникорна.
 * NEUTRAL — пасётся, бродит.
 * ALERT   — заметил игрока с оружием в радиусе предупреждения, следит за ним, не атакует.
 * COMBAT  — полноценный бой: сам атакует и призывает союзников.
 */
public enum UnicornCombatState {
    NEUTRAL,
    ALERT,
    COMBAT
}