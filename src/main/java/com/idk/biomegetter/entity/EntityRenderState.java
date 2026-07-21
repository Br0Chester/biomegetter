package com.idk.biomegetter.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class EntityRenderState extends LivingEntityRenderState {
    public final AnimationState idleAnimationState = new AnimationState();
}
