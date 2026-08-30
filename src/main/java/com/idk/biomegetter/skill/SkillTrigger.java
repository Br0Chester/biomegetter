package com.idk.biomegetter.skill;

/**
 * Строковый (не закрытый enum) момент срабатывания компонента — builtin-компоненты публикуют
 * стандартные значения ниже; кастомные Java-компоненты (например "волна встретила волну" из
 * обсуждения) вольны публиковать/слушать СВОИ произвольные строки — но тогда координация между
 * такими компонентами реализуется внутри Java, не собирается из примитивов JSON.
 */
public final class SkillTrigger {
    public static final String ON_CAST = "on_cast";       // сразу при применении скилла
    public static final String ON_HIT = "on_hit";         // projectile/raycast нашёл цель (следующий подэтап)
    public static final String ON_EXPIRE = "on_expire";   // projectile/raycast истёк без цели, если запрошено (следующий подэтап)
    public static final String ON_AREA_TICK = "on_area_tick"; // персистентная AreaShape тикает (следующий подэтап)

    private SkillTrigger() {
    }
}