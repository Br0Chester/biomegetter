package com.idk.biomegetter.item.custom;

import com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemActiveSkillLoader;
import com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemPassiveSkillLoader;
import com.idk.biomegetter.block.custom.cauldron.data.totem.TotemSkill;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Предмет тотема — не стакается (прочность!), пассивка тикает каждый серверный тик, пока
 * тотем физически в одной из рук (см. inventoryTick — сигнатура version-dependent, самое
 * вероятное место расхождения с вашей версией API), активка — по ПКМ (обе руки; ЛКМ для
 * основной руки — отдельная будущая задача, см. пояснение в чате).
 */
public class TotemItem extends Item {

    public TotemItem(Properties properties) {
        // ВАЖНО: stacksTo(1) обязателен — предметы с прочностью не должны стакаться.
        super(properties.stacksTo(1));
    }

    // ---- Чтение/запись данных тотема из CUSTOM_DATA ----

    public static ItemStack build(
            String material, int level,
            Identifier passiveSkillId, Map<String, Float> passiveStats,
            Identifier activeSkillId, Map<String, Float> activeStats,
            int durability, int maxDurability
    ) {
        ItemStack stack = new ItemStack(com.idk.biomegetter.item.ModItems.TOTEM);
        CompoundTag tag = new CompoundTag();
        tag.putString("Material", material);
        tag.putInt("Level", level);
        tag.putString("PassiveSkill", passiveSkillId.toString());
        tag.putString("ActiveSkill", activeSkillId.toString());
        tag.put("PassiveStats", statsToTag(passiveStats));
        tag.put("ActiveStats", statsToTag(activeStats));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.MAX_DAMAGE, maxDurability);
        stack.set(DataComponents.DAMAGE, maxDurability - durability); // используем ванильный durability-бар "бесплатно"
        return stack;
    }

    private static CompoundTag statsToTag(Map<String, Float> stats) {
        CompoundTag tag = new CompoundTag();
        stats.forEach((k, v) -> tag.put(k, FloatTag.valueOf(v)));
        return tag;
    }

    private static Map<String, Float> statsFromTag(CompoundTag tag) {
        Map<String, Float> result = new HashMap<>();
        for (String key : tag.keySet()) {
            tag.getFloat(key).ifPresent(v -> result.put(key, v));
        }
        return result;
    }

    private static java.util.Optional<CompoundTag> data(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? java.util.Optional.empty() : java.util.Optional.of(customData.copyTag());
    }

    public static java.util.Optional<Identifier> getPassiveSkillId(ItemStack stack) {
        return data(stack).flatMap(tag -> tag.getString("PassiveSkill")).map(Identifier::parse);
    }

    public static java.util.Optional<Identifier> getActiveSkillId(ItemStack stack) {
        return data(stack).flatMap(tag -> tag.getString("ActiveSkill")).map(Identifier::parse);
    }

    public static Map<String, Float> getPassiveStats(ItemStack stack) {
        return data(stack).map(tag -> statsFromTag(tag.getCompoundOrEmpty("PassiveStats"))).orElse(Map.of());
    }

    public static Map<String, Float> getActiveStats(ItemStack stack) {
        return data(stack).map(tag -> statsFromTag(tag.getCompoundOrEmpty("ActiveStats"))).orElse(Map.of());
    }

    public static String getMaterial(ItemStack stack) {
        return data(stack).flatMap(tag -> tag.getString("Material")).orElse("organic");
    }

    // ---- Пассивка: тикает, пока тотем в любой из рук ----

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player)) return;

        boolean inHand = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (!inHand) return;

        // ВАЖНО: вызывается для КАЖДОЙ руки отдельно (mainhand и offhand — два независимых
        // ItemStack), поэтому reconcile() здесь НЕ вызывается — только markActive для эффектов
        // ЭТОГО конкретного стека. Финальный reconcile за весь тик сущности делает отдельный
        // централизованный проход, см. PlayerPassiveBuffCoordinator.
        getPassiveSkillId(stack).ifPresent(skillId -> {
            TotemSkill skill = com.idk.biomegetter.block.custom.cauldron.data.totem.CauldronTotemPassiveSkillLoader.get(skillId);
            if (skill == null) return;
            Map<String, Float> stats = getPassiveStats(stack);
            var context = new com.idk.biomegetter.skill.SkillCastContext(level, player, stats, currentDurability(stack));
            com.idk.biomegetter.skill.SkillComponentRunner.cast(skill.components(), context);

            if (level.getRandom().nextFloat() < skill.durabilityCostChance()) {
                int cost = scaledDurabilityCost(skill.durabilityCost(), getMaterial(stack), true);
                applyDurabilityCost(stack, player, cost);
            }
        });
    }

    // ---- Активка: ПКМ (обе руки). ЛКМ для основной руки — не реализовано, см. чат. ----

    /**
     * ПКМ активирует активку — рукой-независимо (как у щита), т.е. срабатывает вне зависимости
     * от того, в какой руке физически держат тотем. Если И основная, И дополнительная рука
     * заняты тотемами одновременно — применяются ОБА скилла за один клик (см. согласованное
     * "если 2 руки заняты тотемами — используем оба сразу"). Ваниль вызывает use() один раз на
     * ту руку, что реально среагировала на клик — второй тотем (если он в другой руке)
     * применяется явно отсюда же, а не через второй вызов use().
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        activateSkill(serverLevel, player, player.getItemInHand(hand));

        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack otherStack = player.getItemInHand(otherHand);
        if (otherStack.getItem() instanceof TotemItem) {
            activateSkill(serverLevel, player, otherStack);
        }

        return InteractionResult.SUCCESS;
    }

    private static void activateSkill(ServerLevel serverLevel, Player player, ItemStack stack) {
        getActiveSkillId(stack).ifPresent(skillId -> {
            var skill = CauldronTotemActiveSkillLoader.get(skillId);
            if (skill == null) return;
            Map<String, Float> stats = getActiveStats(stack);
            var context = new com.idk.biomegetter.skill.SkillCastContext(serverLevel, player, stats, currentDurability(stack));
            com.idk.biomegetter.skill.SkillComponentRunner.cast(skill.components(), context);

            if (serverLevel.getRandom().nextFloat() < skill.durabilityCostChance()) {
                int cost = scaledDurabilityCost(skill.durabilityCost(), getMaterial(stack), false);
                applyDurabilityCost(stack, player, cost);
            }
        });
    }

    private static int currentDurability(ItemStack stack) {
        Integer maxDamage = stack.get(DataComponents.MAX_DAMAGE);
        Integer damage = stack.get(DataComponents.DAMAGE);
        if (maxDamage == null) return 0;
        return maxDamage - (damage == null ? 0 : damage);
    }

    /**
     * organic дешевле на пассивку, metal дешевле на активку (см. согласованное правило).
     */
    private static int scaledDurabilityCost(int baseCost, String material, boolean isPassive) {
        boolean discounted = (isPassive && material.equals("organic")) || (!isPassive && material.equals("metal"));
        return discounted ? Math.max(1, Math.round(baseCost * 0.5f)) : baseCost;
    }

    private static void applyDurabilityCost(ItemStack stack, Player player, int cost) {
        stack.hurtAndBreak(cost, player, EquipmentSlot.MAINHAND); // ломается (исчезает) при достижении maxDamage — штатное ванильное поведение
    }


    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        getPassiveSkillId(stack).ifPresent(id -> {
            var skill = CauldronTotemPassiveSkillLoader.get(id);
            if (skill != null)
                tooltip.accept(Component.translatable("totem.biomegetter.passive_label")
                        .append(Component.translatable(skill.nameKey())));
        });
        getActiveSkillId(stack).ifPresent(id -> {
            var skill = CauldronTotemActiveSkillLoader.get(id);
            if (skill != null) tooltip.accept(Component.translatable("totem.biomegetter.active_label")
                    .append(Component.translatable(skill.nameKey())));
        });
    }
}