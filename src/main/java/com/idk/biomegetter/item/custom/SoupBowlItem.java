package com.idk.biomegetter.item.custom;

import com.idk.biomegetter.block.custom.cauldron.data.spec_spices.SoupEatTrigger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Предмет готового супа. Питательность/эффекты/остаток-миска навешиваются динамически на
 * конкретный ItemStack (см. ModCauldronBlockEntity#toItemStack) — сам класс отличается от
 * обычного Item только одним: после доедания дополнительно применяет список
 * {@link SoupEatTrigger}, сохранённый внутри DataComponents.CUSTOM_DATA (ключ "EatTriggers") —
 * взрывы/отпугивание мобов/что угодно ещё, зарегистрированное специей через "eat_triggers" в
 * soup_spice/*.json.
 */
public class SoupBowlItem extends Item {

    public SoupBowlItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        List<SoupEatTrigger> triggers = readEatTriggers(stack);
        com.idk.biomegetter.BiomeGetter.LOGGER.info("SoupBowlItem.finishUsingItem: eatTriggers read = {}", triggers.size());
        ItemStack result = super.finishUsingItem(stack, level, entity); // применит FOOD/CONSUMABLE/USE_REMAINDER как обычно
        if (!triggers.isEmpty() && level instanceof ServerLevel serverLevel) {
            for (SoupEatTrigger trigger : triggers) {
                trigger.apply(serverLevel, entity);
            }
        }
        return result;
    }

    private static List<SoupEatTrigger> readEatTriggers(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return List.of();
        CompoundTag tag = customData.copyTag();
        if (!tag.contains("EatTriggers")) return List.of();
        Tag triggersTag = tag.get("EatTriggers");
        return SoupEatTrigger.CODEC.listOf().parse(net.minecraft.nbt.NbtOps.INSTANCE, triggersTag)
                .result().orElse(List.of());
    }
}