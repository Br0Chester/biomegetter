package com.idk.biomegetter.block.custom.cauldron.data.spec_spices;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.List;
import java.util.Map;

/**
 * Конструктор именованных функций, преобразующих ИТОГОВЫЙ список эффектов супа целиком —
 * срабатывают в момент завершения варки (после {@code buildEffectInstance}, после применения
 * {@link IngredientEffectDef.EffectModifier}), результат — окончательный список эффектов
 * предмета. Чтобы добавить новую функцию: 1) новый record с реализацией apply(); 2) один case
 * в {@link #codecFor(String)}. Больше никуда лезть не нужно.
 * <p>
 * Отличается от {@link SoupEatTrigger} по времени срабатывания — тот работает не над списком
 * эффектов, а в момент реального поедания супа игроком (взрыв, отпугивание мобов и т.п.).
 */
public sealed interface BuffTransform {

    Codec<BuffTransform> CODEC = Codec.STRING.dispatch("type", BuffTransform::typeId, BuffTransform::codecFor);

    String typeId();

    List<MobEffectInstance> apply(List<MobEffectInstance> effects);

    private static com.mojang.serialization.MapCodec<? extends BuffTransform> codecFor(String type) {
        return switch (type) {
            case "invert_buffs" -> InvertBuffs.CODEC;
            default -> throw new IllegalArgumentException("Unknown buff transform type: " + type);
        };
    }

    /**
     * Меняет каждый эффект на его "противоположность" по заранее известной паре (см.
     * {@link #OPPOSITES}). Эффекты, для которых пары не задано, остаются без изменений
     * (безопасный fallback — не роняем весь суп из-за одного неизвестного эффекта).
     */
    record InvertBuffs() implements BuffTransform {
        public static final com.mojang.serialization.MapCodec<InvertBuffs> CODEC =
                com.mojang.serialization.MapCodec.unit(InvertBuffs::new);

        /**
         * Явные пары "противоположностей" — единственное место в системе, где это нужно
         * захардкодить (в ваниле нет канонического понятия "обратный эффект"). Дополняйте по
         * необходимости — список не исчерпывающий.
         */
        private static final Map<Holder<MobEffect>, Holder<MobEffect>> OPPOSITES = Map.ofEntries(
                Map.entry(MobEffects.SPEED, MobEffects.SLOWNESS),
                Map.entry(MobEffects.SLOWNESS, MobEffects.SPEED),
                Map.entry(MobEffects.STRENGTH, MobEffects.WEAKNESS),
                Map.entry(MobEffects.WEAKNESS, MobEffects.STRENGTH),
                Map.entry(MobEffects.REGENERATION, MobEffects.POISON),
                Map.entry(MobEffects.POISON, MobEffects.REGENERATION),
                Map.entry(MobEffects.JUMP_BOOST, MobEffects.SLOWNESS),
                Map.entry(MobEffects.LUCK, MobEffects.UNLUCK),
                Map.entry(MobEffects.UNLUCK, MobEffects.LUCK),
                Map.entry(MobEffects.ABSORPTION, MobEffects.WITHER)
        );

        @Override
        public String typeId() {
            return "invert_buffs";
        }

        @Override
        public List<MobEffectInstance> apply(List<MobEffectInstance> effects) {
            List<MobEffectInstance> result = new java.util.ArrayList<>(effects.size());
            for (MobEffectInstance instance : effects) {
                Holder<MobEffect> opposite = OPPOSITES.get(instance.getEffect());
                if (opposite != null) {
                    result.add(new MobEffectInstance(opposite, instance.getDuration(), instance.getAmplifier()));
                } else {
                    result.add(instance);
                }
            }
            return result;
        }
    }
}