package com.idk.biomegetter.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$2")
public class GrindstoneMenuSlot0Mixin {

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void biomegetter$allowWheat(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (itemStack.is(Items.WHEAT)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}