package com.idk.biomegetter.block.custom.cauldron;

import com.idk.biomegetter.block.entity.ModCauldronBlockEntity.Content;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public final class CauldronContentTypes {

    private static final Map<Content, CauldronContentType> REGISTRY = new EnumMap<>(Content.class);

    public static final CauldronContentType WATER = register(new CauldronContentType(
            Content.WATER, Items.WATER_BUCKET,
            id("water_cauldron_level1"), id("water_cauldron_level2"), id("water_cauldron_full"),
            -1, true, true, true, 2.0F,
            SoundEvents.BUCKET_EMPTY, SoundEvents.BUCKET_FILL
    ));

    public static final CauldronContentType LAVA = register(new CauldronContentType(
            Content.LAVA, Items.LAVA_BUCKET,
            id("lava_cauldron"), id("lava_cauldron"), id("lava_cauldron"), // у лавы нет уровней — всегда "полная"
            -1, false, true, false, 4.0F,
            SoundEvents.BUCKET_EMPTY_LAVA, SoundEvents.BUCKET_FILL_LAVA
    ));

    public static final CauldronContentType MILK = register(new CauldronContentType(
            Content.MILK, Items.MILK_BUCKET,
            id("water_cauldron_level1"), id("water_cauldron_level2"), id("water_cauldron_full"), // геометрия как у воды
            0xFFFFFF, true, false, false, 0.0F,
            SoundEvents.BUCKET_EMPTY, SoundEvents.BUCKET_FILL
    ));

    public static final CauldronContentType JUICE = register(new CauldronContentType(
            Content.JUICE, null, // наполняется только ягодами, не ведром
            id("water_cauldron_level1"), id("water_cauldron_level2"), id("water_cauldron_full"),
            0xB0202A, true, false, false, 0.0F,
            SoundEvents.BUCKET_EMPTY, SoundEvents.BUCKET_FILL
    ));

    private static CauldronContentType register(CauldronContentType type) {
        REGISTRY.put(type.id(), type);
        return type;
    }

    public static CauldronContentType get(Content content) {
        return REGISTRY.get(content);
    }

    public static Collection<CauldronContentType> all() {
        return REGISTRY.values();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", "block/" + path);
    }
}