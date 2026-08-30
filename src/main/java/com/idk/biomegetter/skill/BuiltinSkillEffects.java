package com.idk.biomegetter.skill;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class BuiltinSkillEffects {

    private BuiltinSkillEffects() {
    }

    public static void registerAll() {
        TotemSkillEffect.register("mob_effect_buff", MobEffectBuff.CODEC);
        TotemSkillEffect.register("direct_damage", DirectDamage.CODEC);
        TotemSkillEffect.register("transform_block", TransformBlock.CODEC);

        SkillDelivery.registerAll();
        AreaShape.registerAll();
    }

    /**
     * {@code duration_ticks} НЕ указан в JSON -> бесконечный эффект (MobEffectInstance с
     * duration = -1, ваниль трактует это как "бессрочно"). Для баффов, привязанных к
     * "держит предмет в руке" (пассивки тотема), используйте infinite — источник (например
     * TotemItem.inventoryTick) сам обязан СНЯТЬ эффект, когда предмет покинул руку (см.
     * PassiveBuffTracker) — иначе ежесекундное переналожение мерцает и раздражает.
     */
    public record MobEffectBuff(String mobEffectId,
                                java.util.Optional<Integer> durationTicks) implements TotemSkillEffect {
        public static final MapCodec<MobEffectBuff> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.STRING.fieldOf("mob_effect").forGetter(MobEffectBuff::mobEffectId),
                com.mojang.serialization.Codec.INT.optionalFieldOf("duration_ticks").forGetter(MobEffectBuff::durationTicks)
        ).apply(instance, MobEffectBuff::new));

        @Override
        public String typeId() {
            return "mob_effect_buff";
        }

        @Override
        public void apply(SkillCastContext context, SkillTarget target) {
            if (!(target instanceof SkillTarget.EntityTarget entityTarget))
                return; // block-цель этому эффекту не подходит
            LivingEntity livingTarget = entityTarget.entity();
            var effectValue = BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse(mobEffectId));
            if (effectValue == null) return;
            var effectHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effectValue);
            float power = context.stat("power", 1.0f);
            int amplifier = Math.max(0, Math.round(power) - 1);

            if (durationTicks.isEmpty()) {
                // infinite-режим — только отмечаем в трекере, реальное addEffect делает
                // PlayerPassiveBuffCoordinator ОДИН РАЗ за тик игрока (после сбора со всех рук),
                // чтобы reconcile корректно снимал баф при выпадении источника.
                com.idk.biomegetter.skill.PassiveBuffTracker.markActive(livingTarget, effectHolder);
                if (!livingTarget.hasEffect(effectHolder)) {
                    livingTarget.addEffect(new MobEffectInstance(effectHolder, -1, amplifier));
                }
            } else {
                livingTarget.addEffect(new MobEffectInstance(effectHolder, durationTicks.get(), amplifier));
            }
        }
    }

    /**
     * Прямой урон + опциональный дебафф — теперь применяется к УЖЕ найденной цели (area/delivery), не ищет её сам.
     */
    /**
     * {@code self_damage_on} (default false) — по умолчанию НЕ бьёт самого применяющего, даже
     * если он попал в найденную зону (обычный "ударная волна не ранит себя"); явно true —
     * разрешает урон и по себе тоже (согласованный кейс "иногда действительно нужно себе").
     */
    public record DirectDamage(String debuffEffectId, boolean selfDamageOn) implements TotemSkillEffect {
        public static final MapCodec<DirectDamage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.STRING.optionalFieldOf("debuff_effect", "minecraft:weakness").forGetter(DirectDamage::debuffEffectId),
                com.mojang.serialization.Codec.BOOL.optionalFieldOf("self_damage_on", false).forGetter(DirectDamage::selfDamageOn)
        ).apply(instance, DirectDamage::new));

        @Override
        public String typeId() {
            return "direct_damage";
        }

        @Override
        public void apply(SkillCastContext context, SkillTarget target) {
            if (!(target instanceof SkillTarget.EntityTarget entityTarget)) return;
            LivingEntity livingTarget = entityTarget.entity();
            if (livingTarget == context.caster() && !selfDamageOn) return;
            float power = context.stat("power", 5.0f);
            int debuffDuration = Math.round(context.stat("debuff_duration_ticks", 100.0f));

            livingTarget.hurtServer(context.level(), context.caster().damageSources().magic(), power);

            var effectHolder = BuiltInRegistries.MOB_EFFECT.getValue(Identifier.parse(debuffEffectId));
            if (effectHolder != null) {
                livingTarget.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effectHolder), debuffDuration, 0));
            }
        }
    }

    /**
     * Заменяет блок на {@code result_block}, через {@code duration_ticks} возвращает исходный
     * блок обратно (пример "ice_cage" из обсуждения — упрощённая версия: ограничение "нельзя
     * заменить блок с прочностью выше Y" пока НЕ реализовано, добавим отдельным полем, когда
     * понадобится). Требует BlockTarget — с EntityTarget не делает ничего.
     */
    /**
     * {@code max_hardness} НЕ указан в JSON -> ограничения нет, заменяется любой блок.
     * Если указан — блоки твёрже этого значения (getDestroySpeed) пропускаются (эффект на них
     * просто не применяется — не крашится).
     */
    public record TransformBlock(Identifier resultBlock, int durationTicks,
                                 java.util.Optional<Float> maxHardness) implements TotemSkillEffect {
        public static final MapCodec<TransformBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.STRING.xmap(Identifier::parse, Identifier::toString)
                        .fieldOf("result_block").forGetter(TransformBlock::resultBlock),
                com.mojang.serialization.Codec.INT.optionalFieldOf("duration_ticks", 200).forGetter(TransformBlock::durationTicks),
                com.mojang.serialization.Codec.FLOAT.optionalFieldOf("max_hardness").forGetter(TransformBlock::maxHardness)
        ).apply(instance, TransformBlock::new));

        @Override
        public String typeId() {
            return "transform_block";
        }

        @Override
        public void apply(SkillCastContext context, SkillTarget target) {
            if (!(target instanceof SkillTarget.BlockTarget blockTarget)) return;
            var pos = blockTarget.pos();
            var level = context.level();

            var oldState = level.getBlockState(pos);
            if (!oldState.getFluidState().isEmpty() && oldState.isAir()) return;

            if (maxHardness.isPresent()) {
                float hardness = oldState.getDestroySpeed(level, pos);
                if (hardness < 0 || hardness > maxHardness.get())
                    return; // hardness<0 = небьющийся блок (бедрок и т.п.) — тоже пропускаем
            }

            var newBlock = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(resultBlock);
            if (newBlock == null) return;

            level.setBlockAndUpdate(pos, newBlock.defaultBlockState());
            com.idk.biomegetter.skill.TemporaryBlockTracker.schedule(level, pos, oldState, durationTicks);
        }
    }

}