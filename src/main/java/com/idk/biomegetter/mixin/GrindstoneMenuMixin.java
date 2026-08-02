package com.idk.biomegetter.mixin;

import com.idk.biomegetter.item.ModItems;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GrindstoneMenu.class)
public class GrindstoneMenuMixin {

    /**
     * Добавляет рецепт "пшеница -> мука" (1 к 1) через точильный камень,
     * не трогая ванильное поведение слияния прочности/зачарований —
     * срабатывает только когда во втором слоте (ADDITIONAL_SLOT) пусто,
     * а в первом (INPUT_SLOT) лежит пшеница, то есть ванильный сценарий
     * ремонта двух предметов этот случай в принципе не покрывает.
     */
    @Inject(method = "computeResult", at = @At("HEAD"), cancellable = true)
    private void biomegetter$grindWheatIntoMeal(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> cir) {
        if (input.is(Items.WHEAT) && additional.isEmpty()) {
            cir.setReturnValue(new ItemStack(ModItems.MEAL, input.getCount()));
            cir.cancel();
        }
    }
}