package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.util.ParticleCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Единая "библиотека" действий, которые может выполнить реакция (liquid_reaction /
 * solid_reaction). JSON описывает СПИСОК уже существующих эффектов и их параметры —
 * саму логику каждого эффекта нельзя выразить в JSON, только здесь, в Java.
 * <p>
 * Чтобы добавить НОВЫЙ вид эффекта (взрыв, урон по площади, спавн блоков и т.п.):
 * 1) добавить новый record с реализацией apply();
 * 2) добавить один case в {@link #codecFor(String)}.
 * Больше никуда лезть не нужно — новый эффект сразу доступен из любого JSON-правила.
 */
public sealed interface ReactionEffect {

    Codec<ReactionEffect> CODEC = Codec.STRING.dispatch("type", ReactionEffect::typeId, ReactionEffect::codecFor);

    String typeId();

    /**
     * @param amount сколько "единиц" реакции произошло за это событие — для жидкости это
     *               число сгоревших третей, для твёрдого — всегда 1 (одна извлечённая запись)
     */
    void apply(Level level, BlockPos pos, int amount);

    private static com.mojang.serialization.MapCodec<? extends ReactionEffect> codecFor(String type) {
        return switch (type) {
            case "drop_item" -> DropItem.CODEC;
            case "play_sound" -> PlaySound.CODEC;
            case "drop_item_near" -> DropItemNear.CODEC;
            case "spawn_entity" -> SpawnEntity.CODEC;
            case "spawn_particles" -> SpawnParticles.CODEC;
            default -> throw new IllegalArgumentException("Unknown reaction effect type: " + type);
        };
    }

    /**
     * Роняет {@code countPerUnit * amount} предметов рядом с котлом.
     */
    record DropItem(Item item, int countPerUnit) implements ReactionEffect {
        public static final com.mojang.serialization.MapCodec<DropItem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(DropItem::item),
                Codec.INT.optionalFieldOf("count_per_unit", 1).forGetter(DropItem::countPerUnit)
        ).apply(instance, DropItem::new));

        @Override
        public String typeId() {
            return "drop_item";
        }

        @Override
        public void apply(Level level, BlockPos pos, int amount) {
            int total = countPerUnit * amount;
            if (total <= 0 || level.isClientSide()) return;
            net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(item, total));
        }
    }

    /**
     * Проигрывает звук один раз за событие, независимо от {@code amount}.
     */
    record PlaySound(SoundEvent sound) implements ReactionEffect {
        public static final com.mojang.serialization.MapCodec<PlaySound> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("sound").forGetter(PlaySound::sound)
        ).apply(instance, PlaySound::new));

        @Override
        public String typeId() {
            return "play_sound";
        }

        @Override
        public void apply(Level level, BlockPos pos, int amount) {
            if (amount <= 0 || level.isClientSide()) return;
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /**
     * Роняет предмет рядом с котлом (для последствий рецепта — не "в руки", а на землю).
     */
    record DropItemNear(Item item, int countPerUnit) implements ReactionEffect {
        public static final com.mojang.serialization.MapCodec<DropItemNear> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(DropItemNear::item),
                Codec.INT.optionalFieldOf("count_per_unit", 1).forGetter(DropItemNear::countPerUnit)
        ).apply(instance, DropItemNear::new));

        @Override
        public String typeId() {
            return "drop_item_near";
        }

        @Override
        public void apply(Level level, BlockPos pos, int amount) {
            int total = countPerUnit * amount;
            if (total <= 0 || level.isClientSide()) return;
            net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(item, total));
        }
    }

    /**
     * Спавнит сущность НАД котлом (та же позиция, что и проверка EntityRequirement "в блоке над").
     */
    record SpawnEntity(net.minecraft.world.entity.EntityType<?> entityType) implements ReactionEffect {
        public static final com.mojang.serialization.MapCodec<SpawnEntity> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity_type").forGetter(SpawnEntity::entityType)
        ).apply(instance, SpawnEntity::new));

        @Override
        public String typeId() {
            return "spawn_entity";
        }

        @Override
        public void apply(Level level, BlockPos pos, int amount) {
            if (level.isClientSide() || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
            net.minecraft.world.entity.Entity entity = entityType.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (entity == null) return;
            BlockPos abovePos = pos.above();
            entity.snapTo(
                    abovePos.getX() + 0.5,
                    abovePos.getY(),
                    abovePos.getZ() + 0.5,
                    0f,
                    0f
            );
            serverLevel.addFreshEntity(entity);
        }
    }

    record SpawnParticles(net.minecraft.core.particles.ParticleOptions particle, int count) implements ReactionEffect {
        public static final com.mojang.serialization.MapCodec<SpawnParticles> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ParticleCodec.CODEC.fieldOf("particle").forGetter(SpawnParticles::particle),
                Codec.INT.optionalFieldOf("count", 12).forGetter(SpawnParticles::count)
        ).apply(instance, SpawnParticles::new));

        @Override
        public String typeId() {
            return "spawn_particles";
        }

        @Override
        public void apply(Level level, BlockPos pos, int amount) {
            if (level.isClientSide() || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
            serverLevel.sendParticles(particle, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, count, 0.3, 0.2, 0.3, 0.05);
        }
    }
}