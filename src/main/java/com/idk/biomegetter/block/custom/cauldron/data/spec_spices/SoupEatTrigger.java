package com.idk.biomegetter.block.custom.cauldron.data.spec_spices;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.Vec3;

/**
 * Конструктор именованных функций, срабатывающих в момент ПОЕДАНИЯ супа игроком (не при
 * варке) — взрывы, отталкивание, отпугивание мобов и т.п. Требует хука в кастомном Item
 * (finishUsingItem), т.к. ванильный Items.BOWL такого хука не предоставляет — применение
 * подключается отдельным шагом (см. обсуждение в чате), сама схема функций уже готова.
 * Чтобы добавить новую функцию: 1) новый record с реализацией apply(); 2) один case в
 * {@link #codecFor(String)}.
 */
public sealed interface SoupEatTrigger {

    Codec<SoupEatTrigger> CODEC = Codec.STRING.dispatch("type", SoupEatTrigger::typeId, SoupEatTrigger::codecFor);

    String typeId();

    /**
     * @param eater сущность, которая только что доела суп — эффекты, направленные на "всех
     *              вокруг, кроме едока", обязаны сами исключать eater из выборки целей.
     */
    void apply(ServerLevel level, LivingEntity eater);

    private static com.mojang.serialization.MapCodec<? extends SoupEatTrigger> codecFor(String type) {
        return switch (type) {
            case "explosive_backlash" -> ExplosiveBacklash.CODEC;
            case "hostile_repel" -> HostileRepel.CODEC;
            default -> throw new IllegalArgumentException("Unknown soup eat trigger type: " + type);
        };
    }

    /**
     * Отталкивает и травит ядом всех живых существ в радиусе, КРОМЕ едока, с визуальным
     * эффектом взрыва (без урона по блокам).
     */
    record ExplosiveBacklash(double radius, int poisonDurationTicks, int poisonAmplifier) implements SoupEatTrigger {
        public static final com.mojang.serialization.MapCodec<ExplosiveBacklash> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("radius", 3.0).forGetter(ExplosiveBacklash::radius),
                Codec.INT.optionalFieldOf("poison_duration_ticks", 100).forGetter(ExplosiveBacklash::poisonDurationTicks),
                Codec.INT.optionalFieldOf("poison_amplifier", 0).forGetter(ExplosiveBacklash::poisonAmplifier)
        ).apply(instance, ExplosiveBacklash::new));

        @Override
        public String typeId() {
            return "explosive_backlash";
        }

        @Override
        public void apply(ServerLevel level, LivingEntity eater) {
            level.playSound(
                    null,
                    eater.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE.value(),  // <- .value()
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                    eater.getX(), eater.getY() + 0.5, eater.getZ(), 1, 0, 0, 0, 0);

            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class,
                    eater.getBoundingBox().inflate(radius), e -> e != eater)) {
                Vec3 diff = nearby.position().subtract(eater.position());
                double dist = Math.max(diff.length(), 0.5);
                Vec3 knockback = diff.normalize().scale(Mth.clamp(radius / dist, 0.5, 2.0));
                nearby.push(knockback.x, 0.3, knockback.z);
                nearby.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDurationTicks, poisonAmplifier));
            }
        }
    }

    /**
     * Заставляет враждебных мобов в радиусе временно "бежать" от едока (аналог поведения
     * крипера рядом с кошкой) — реализовано через временный эффект скорости+тряски цели прочь.
     */
    record HostileRepel(double radius, int durationTicks) implements SoupEatTrigger {
        public static final com.mojang.serialization.MapCodec<HostileRepel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("radius", 8.0).forGetter(HostileRepel::radius),
                Codec.INT.optionalFieldOf("duration_ticks", 200).forGetter(HostileRepel::durationTicks)
        ).apply(instance, HostileRepel::new));

        @Override
        public String typeId() {
            return "hostile_repel";
        }

        @Override
        public void apply(ServerLevel level, LivingEntity eater) {
            // Приближённая реализация без миксинов: сильный импульс прочь от едока + временная
            // скорость+прыгучесть (визуально читается как "паническое бегство"). Настоящее
            // AI-поведение (постоянное убегание, как у крипера от кошки) требует внедрения
            // кастомной Goal в приватный GoalSelector чужих ванильных мобов — это отдельная
            // задача, обсуждали отдельно.
            for (Monster monster : level.getEntitiesOfClass(Monster.class, eater.getBoundingBox().inflate(radius))) {
                Vec3 away = monster.position().subtract(eater.position());
                double dist = Math.max(away.length(), 0.5);
                Vec3 push = away.normalize().scale(1.2);
                monster.push(push.x, 0.4, push.z);
                monster.addEffect(new MobEffectInstance(MobEffects.SPEED, durationTicks, 2));
                monster.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, durationTicks, 1));
            }
        }
    }
}