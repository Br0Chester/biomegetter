package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.util.ParticleCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;

import java.util.List;
import java.util.Optional;

public sealed interface CauldronRecipeStage {

    Codec<CauldronRecipeStage> CODEC = Codec.STRING.dispatch("type", CauldronRecipeStage::typeId, type -> switch (type) {
        case "cook" -> CookStage.CODEC;
        case "await_ingredient" -> AwaitIngredientStage.CODEC;
        case "collect_open" -> CollectOpenStage.CODEC;
        default -> throw new IllegalArgumentException("Unknown recipe stage type: " + type);
    });

    String typeId();

    /**
     * Автоматическая стадия — просто идёт заданное время, ничего не требует от игрока.
     */
    record CookStage(int durationTicks, Optional<ParticleOptions> particle) implements CauldronRecipeStage {
        public static final com.mojang.serialization.MapCodec<CookStage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("duration_ticks").forGetter(CookStage::durationTicks),
                ParticleCodec.CODEC.optionalFieldOf("particle").forGetter(CookStage::particle)
        ).apply(instance, CookStage::new));

        @Override
        public String typeId() {
            return "cook";
        }
    }

    /**
     * Стадия свободного сбора ("творческая" часть, супы): игрок докидывает ЛЮБЫЕ (в пределах
     * {@code allowed_ingredients} рецепта, если он задан) ингредиенты и мешает — накопленное
     * идёт в {@code composite}. Пока крутится по {@code idlePhases} — истечение последней фазы
     * без мешания обрывает варку (испарение), симметрично {@link AwaitIngredientStage}.
     */
    record CollectOpenStage(List<IdlePhase> idlePhases) implements CauldronRecipeStage {
        public static final com.mojang.serialization.MapCodec<CollectOpenStage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                IdlePhase.CODEC.listOf().fieldOf("idle_phases").forGetter(CollectOpenStage::idlePhases)
        ).apply(instance, CollectOpenStage::new));

        @Override
        public String typeId() {
            return "collect_open";
        }
    }

    /**
     * Стадия ожидания — котёл "живой" (стеки доступны как обычно), ждёт, пока игрок положит
     * ровно {@code requiredLiquids}+{@code requiredSolids} и снова помешает. Пока ждёт, крутится
     * по списку {@code idlePhases} (каждая — своя длительность и партикл); истечение последней
     * фазы без успеха обрывает варку (содержимое испаряется). Используется для СТРОГИХ рецептов
     * (не супов) — точное совпадение состава, без свободы выбора игрока.
     */
    record AwaitIngredientStage(
            List<IngredientRequirement> requiredLiquids,
            List<IngredientRequirement> requiredSolids,
            List<IdlePhase> idlePhases
    ) implements CauldronRecipeStage {
        public static final com.mojang.serialization.MapCodec<AwaitIngredientStage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                IngredientRequirement.CODEC.listOf().optionalFieldOf("required_liquids", List.of()).forGetter(AwaitIngredientStage::requiredLiquids),
                IngredientRequirement.CODEC.listOf().optionalFieldOf("required_solids", List.of()).forGetter(AwaitIngredientStage::requiredSolids),
                IdlePhase.CODEC.listOf().fieldOf("idle_phases").forGetter(AwaitIngredientStage::idlePhases)
        ).apply(instance, AwaitIngredientStage::new));

        @Override
        public String typeId() {
            return "await_ingredient";
        }
    }

    record IdlePhase(int durationTicks, Optional<ParticleOptions> particle) {
        public static final Codec<IdlePhase> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("duration_ticks").forGetter(IdlePhase::durationTicks),
                ParticleCodec.CODEC.optionalFieldOf("particle").forGetter(IdlePhase::particle)
        ).apply(instance, IdlePhase::new));
    }
}