package com.idk.biomegetter.entity;

import com.idk.biomegetter.BiomeGetter;
import com.idk.biomegetter.entity.custom.UnicornEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    public static final EntityType<UnicornEntity> UNICORN = register(
            "unicorn",
            EntityType.Builder.<UnicornEntity>of(UnicornEntity::new, MobCategory.MISC)
                    .sized(2.5f, 2.5f)
                    .fireImmune()
    );

    public static void registerAttributes() {
        FabricDefaultAttributeRegistry.register(UNICORN, UnicornEntity.createAttributes());
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
