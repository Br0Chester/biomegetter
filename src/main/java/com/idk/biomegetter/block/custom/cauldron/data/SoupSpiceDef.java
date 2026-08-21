package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.block.custom.cauldron.data.spec_spices.BuffTransform;
import com.idk.biomegetter.block.custom.cauldron.data.spec_spices.SoupEatTrigger;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;

/**
 * Регистрация приправы (специи) для супа — data/biomegetter/soup_spice/*.json. В отличие от
 * обычных ингредиентов, приправа НИКОГДА не кладётся в твёрдый/жидкостный стек — она
 * применяется кликом предмета {@code inputItem} по котлу (см. ModCauldronBlock, пункт
 * "специя"), доступна 1 раз за варку. Может иметь как собственные effects/sustenance, так и
 * modifiers, влияющие на уже накопленные эффекты других ингредиентов (см.
 * IngredientEffectDef.EffectModifier).
 */
public record SoupSpiceDef(
        Item inputItem,
        List<IngredientEffectDef.EffectEntry> effects,
        int sustenance,
        Optional<String> category,
        List<IngredientEffectDef.EffectModifier> modifiers,
        List<BuffTransform> buffTransforms,
        List<SoupEatTrigger> eatTriggers
) {
    public static final Codec<SoupSpiceDef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("input_item").forGetter(SoupSpiceDef::inputItem),
            IngredientEffectDef.EffectEntry.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(SoupSpiceDef::effects),
            Codec.INT.optionalFieldOf("sustenance", 0).forGetter(SoupSpiceDef::sustenance),
            Codec.STRING.optionalFieldOf("category").forGetter(SoupSpiceDef::category),
            IngredientEffectDef.EffectModifier.CODEC.listOf().optionalFieldOf("modifiers", List.of()).forGetter(SoupSpiceDef::modifiers),
            BuffTransform.CODEC.listOf().optionalFieldOf("buff_transforms", List.of()).forGetter(SoupSpiceDef::buffTransforms),
            SoupEatTrigger.CODEC.listOf().optionalFieldOf("eat_triggers", List.of()).forGetter(SoupSpiceDef::eatTriggers)
    ).apply(instance, SoupSpiceDef::new));
}