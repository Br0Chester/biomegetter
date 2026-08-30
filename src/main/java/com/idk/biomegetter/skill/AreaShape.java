package com.idk.biomegetter.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Форма зоны, применяемая ВОКРУГ каждой цели, найденной SkillDelivery — превращает "один
 * найденный target" в "список всех, кого задело". ОТКРЫТЫЙ реестр. Параметры формы читаются
 * ИЗ STATS скилла по ключу (не хардкод) — так кольца автоматически "прокачивают" зону без
 * единой новой строчки в резолвере колец (см. согласованный принцип). durationTicks в этом
 * подэтапе НЕ реализован (persistent-зоны — следующий подэтап, требуют глобального тикера) —
 * поле присутствует в схеме для совместимости на будущее, но игнорируется сейчас (см.
 * SkillComponentRunner).
 */
public interface AreaShape {

    Map<String, MapCodec<? extends AreaShape>> REGISTRY = new HashMap<>();

    Codec<AreaShape> CODEC = Codec.STRING.dispatch("type", AreaShape::typeId, type -> {
        MapCodec<? extends AreaShape> codec = REGISTRY.get(type);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown area shape type: " + type
                    + " (registered types: " + REGISTRY.keySet() + ")");
        }
        return codec;
    });

    static void register(String type, MapCodec<? extends AreaShape> codec) {
        REGISTRY.put(type, codec);
    }

    String typeId();

    List<SkillTarget> findAffected(SkillCastContext context, SkillTarget origin);

    /**
     * Общая точка-центр для origin любого типа (Entity — его позиция, Block — центр клетки).
     */
    private static net.minecraft.world.phys.Vec3 centerOf(SkillTarget origin) {
        if (origin instanceof SkillTarget.EntityTarget entityTarget) return entityTarget.entity().position();
        if (origin instanceof SkillTarget.BlockTarget blockTarget)
            return net.minecraft.world.phys.Vec3.atCenterOf(blockTarget.pos());
        return net.minecraft.world.phys.Vec3.ZERO;
    }

    /**
     * Сфера, ищущая ЖИВЫХ СУЩЕСТВ вокруг origin (годится и для EntityTarget, и для BlockTarget как центра).
     */
    record Sphere(String radiusStat, float defaultRadius) implements AreaShape {
        public static final MapCodec<Sphere> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("radius_stat", "area_radius").forGetter(Sphere::radiusStat),
                Codec.FLOAT.optionalFieldOf("default_radius", 4.0f).forGetter(Sphere::defaultRadius)
        ).apply(instance, Sphere::new));

        @Override
        public String typeId() {
            return "sphere";
        }

        @Override
        public List<SkillTarget> findAffected(SkillCastContext context, SkillTarget origin) {
            float radius = context.stat(radiusStat, defaultRadius);
            var box = net.minecraft.world.phys.AABB.ofSize(centerOf(origin), radius * 2, radius * 2, radius * 2);
            return context.level().getEntitiesOfClass(LivingEntity.class, box).stream()
                    .<SkillTarget>map(SkillTarget.EntityTarget::new).toList();
        }
    }

    record DiskHorizontal(String radiusStat, float defaultRadius, String heightStat,
                          float defaultHeight) implements AreaShape {
        public static final MapCodec<DiskHorizontal> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("radius_stat", "area_radius").forGetter(DiskHorizontal::radiusStat),
                Codec.FLOAT.optionalFieldOf("default_radius", 4.0f).forGetter(DiskHorizontal::defaultRadius),
                Codec.STRING.optionalFieldOf("height_stat", "area_height").forGetter(DiskHorizontal::heightStat),
                Codec.FLOAT.optionalFieldOf("default_height", 2.0f).forGetter(DiskHorizontal::defaultHeight)
        ).apply(instance, DiskHorizontal::new));

        @Override
        public String typeId() {
            return "disk_horizontal";
        }

        @Override
        public List<SkillTarget> findAffected(SkillCastContext context, SkillTarget origin) {
            float radius = context.stat(radiusStat, defaultRadius);
            float height = context.stat(heightStat, defaultHeight);
            var center = centerOf(origin);
            var box = net.minecraft.world.phys.AABB.ofSize(center, radius * 2, height * 2, radius * 2);
            return context.level().getEntitiesOfClass(LivingEntity.class, box).stream()
                    .<SkillTarget>map(SkillTarget.EntityTarget::new).toList();
        }
    }

    /**
     * {@code hollow} (default false) — если true, заполняются только блоки НА ПОВЕРХНОСТИ
     * полусферы (толщиной ~1 блок), внутренний объём остаётся нетронутым — "скорлупа", не шар.
     * Реализовано простым порогом: клетка считается поверхностью, если хотя бы один из её
     * прямых соседей (6-connectivity) уже НЕ входит в полусферу (снаружи радиуса ИЛИ ниже
     * основания) — этого достаточно для визуально сплошной оболочки без сложной геометрии.
     */
    record BlockHemisphere(String radiusStat, float defaultRadius, boolean hollow) implements AreaShape {
        public static final MapCodec<BlockHemisphere> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("radius_stat", "area_radius").forGetter(BlockHemisphere::radiusStat),
                Codec.FLOAT.optionalFieldOf("default_radius", 3.0f).forGetter(BlockHemisphere::defaultRadius),
                Codec.BOOL.optionalFieldOf("hollow", false).forGetter(BlockHemisphere::hollow)
        ).apply(instance, BlockHemisphere::new));

        @Override
        public String typeId() {
            return "block_hemisphere";
        }

        @Override
        public List<SkillTarget> findAffected(SkillCastContext context, SkillTarget origin) {
            if (!(origin instanceof SkillTarget.BlockTarget blockOrigin)) return List.of();
            int radius = Math.round(context.stat(radiusStat, defaultRadius));
            net.minecraft.core.BlockPos center = blockOrigin.pos();
            int radiusSq = radius * radius;

            List<SkillTarget> result = new java.util.ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = 0; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx * dx + dy * dy + dz * dz > radiusSq) continue;
                        if (hollow && !isOnSurface(dx, dy, dz, radius, radiusSq)) continue;
                        result.add(new SkillTarget.BlockTarget(center.offset(dx, dy, dz)));
                    }
                }
            }
            return result;
        }

        private static boolean isOnSurface(int dx, int dy, int dz, int radius, int radiusSq) {
            int[][] neighbors = {{dx + 1, dy, dz}, {dx - 1, dy, dz}, {dx, dy + 1, dz}, {dx, dy - 1, dz}, {dx, dy, dz + 1}, {dx, dy, dz - 1}};
            for (int[] n : neighbors) {
                int nx = n[0], ny = n[1], nz = n[2];
                boolean outsideRadius = nx * nx + ny * ny + nz * nz > radiusSq;
                boolean belowBase = ny < 0;
                if (outsideRadius || belowBase) return true;
            }
            return false;
        }
    }

    static void registerAll() {
        register("sphere", Sphere.CODEC);
        register("disk_horizontal", DiskHorizontal.CODEC);
        register("block_hemisphere", BlockHemisphere.CODEC);
    }
}