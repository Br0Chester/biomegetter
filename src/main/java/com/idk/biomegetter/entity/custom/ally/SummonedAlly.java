package com.idk.biomegetter.entity.custom.ally;

/**
 * Маркер-интерфейс для существ, призванных юникорном.
 * Нужен, чтобы юникорн не выбирал собственных приспешников целью для атаки/призыва.
 */
public interface SummonedAlly {
    void setLifetimeTicks(int ticks);
}