package com.idk.biomegetter.fluid;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;

public class ConfigurableFluidBlock extends LiquidBlock {
    public ConfigurableFluidBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }
}