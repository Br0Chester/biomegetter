package com.idk.biomegetter.entity;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.entity.custom.UnicornEntity;
import com.idk.biomegetter.entity.custom.ally.AllySkeletonEntity;
import com.idk.biomegetter.entity.custom.ally.AllyWitherSkeletonEntity;
import com.idk.biomegetter.entity.custom.ally.AllyZombieEntity;
import com.idk.biomegetter.entity.custom.projectile.UnicornBoltEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;

public class ModEntities {

    public static final EntityType<UnicornEntity> UNICORN = register(
            "unicorn",
            EntityType.Builder.<UnicornEntity>of(UnicornEntity::new, MobCategory.MISC)
                    .sized(2.5f, 2.5f)
                    .fireImmune()
    );

    public static final EntityType<AllyZombieEntity> ALLY_ZOMBIE = register(
            "ally_zombie",
            EntityType.Builder.<AllyZombieEntity>of(AllyZombieEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
    );

    public static final EntityType<AllySkeletonEntity> ALLY_SKELETON = register(
            "ally_skeleton",
            EntityType.Builder.<AllySkeletonEntity>of(AllySkeletonEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.99f)
    );

    public static final EntityType<AllyWitherSkeletonEntity> ALLY_WITHER_SKELETON = register(
            "ally_wither_skeleton",
            EntityType.Builder.<AllyWitherSkeletonEntity>of(AllyWitherSkeletonEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.4f)
    );

    public static final EntityType<UnicornBoltEntity> UNICORN_BOLT = register(
            "unicorn_bolt",
            EntityType.Builder.<UnicornBoltEntity>of(UnicornBoltEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .noSave()
    );

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(UNICORN, UnicornEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ALLY_ZOMBIE, Zombie.createAttributes());
        FabricDefaultAttributeRegistry.register(ALLY_SKELETON, AbstractSkeleton.createAttributes());
        FabricDefaultAttributeRegistry.register(ALLY_WITHER_SKELETON, AbstractSkeleton.createAttributes());
    }

    public static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
    }

    public static void registerModEntityTypes() {
        BiomeGetter.LOGGER.info("Registering EntityTypes for " + BiomeGetter.MOD_ID);
    }

//    Я думаю, функцию можно сделать +- универсальной
//    public static void registerAttributes() {
//        FabricDefaultAttributeRegistry.register();
//    }
}
