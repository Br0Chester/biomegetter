package com.idk.biomegetter.entity;

import com.idk.biomegetter.BiomeGetter;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class ModAttributes {

    public static final Holder<Attribute> MAX_MANA = register(
            "max_mana",
            new RangedAttribute("attribute.biomegetter.max_mana", 100.0, 0.0, 1024.0).setSyncable(true)
    );

    public static final Holder<Attribute> MANA_REGENERATION = register(
            "mana_regeneration",
            // значение в единицах маны В СЕКУНДУ, не за тик — переводим внутри ManaPool
            new RangedAttribute("attribute.biomegetter.mana_regeneration", 5.0, 0.0, 100.0).setSyncable(true)
    );

    private static Holder<Attribute> register(String name, Attribute attribute) {
        return Registry.registerForHolder(
                BuiltInRegistries.ATTRIBUTE,
                Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, name),
                attribute
        );
    }
}