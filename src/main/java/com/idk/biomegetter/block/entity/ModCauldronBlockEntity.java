package com.idk.biomegetter.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Общее состояние наших котлов: осведомлённость об окружении (кэш, а не live-сканирование)
 * и таймер испарения при кипении. Конкретные варианты содержимого (вода/молоко/...)
 * наследуются от этого класса при необходимости специфики.
 */
public class ModCauldronBlockEntity extends BlockEntity {

    public static final int EVAPORATION_INTERVAL_TICKS = 600; // 30 секунд
    public static final int MELT_INTERVAL_TICKS = 600; // 30 секунд

    private boolean powderSnowAbove;
    private int meltTimer = MELT_INTERVAL_TICKS;

    public enum Content implements StringRepresentable {
        EMPTY("empty"), WATER("water"), LAVA("lava"), MILK("milk"), JUICE("juice");

        private final String name;

        Content(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    private boolean hasBlockAbove;
    private boolean heatedBelow;
    private int evaporationTimer = EVAPORATION_INTERVAL_TICKS;

    public ModCauldronBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean hasBlockAbove() {
        return this.hasBlockAbove;
    }

    public void setHasBlockAbove(boolean value) {
        this.hasBlockAbove = value;
    }

    public boolean isHeatedBelow() {
        return this.heatedBelow;
    }

    public void setHeatedBelow(boolean value) {
        this.heatedBelow = value;
    }

    /**
     * @return true, если 30-секундный таймер испарения "натикал" в этом вызове (нужно снять 1 уровень)
     */
    public boolean tickEvaporation() {
        if (--this.evaporationTimer <= 0) {
            this.evaporationTimer = EVAPORATION_INTERVAL_TICKS;
            return true;
        }
        return false;
    }

    public void resetEvaporationTimer() {
        this.evaporationTimer = EVAPORATION_INTERVAL_TICKS;
    }

    public boolean isPowderSnowAbove() {
        return this.powderSnowAbove;
    }

    public void setPowderSnowAbove(boolean value) {
        this.powderSnowAbove = value;
    }

    /**
     * @return true, если 30-секундный таймер таяния "натикал" в этом вызове
     */
    public boolean tickMelt() {
        if (--this.meltTimer <= 0) {
            this.meltTimer = MELT_INTERVAL_TICKS;
            return true;
        }
        return false;
    }

//    @Override
//    protected void saveAdditional(ValueOutput output) {
//        super.saveAdditional(output);
//        output.putBoolean("HasBlockAbove", this.hasBlockAbove);
//        output.putBoolean("HeatedBelow", this.heatedBelow);
//        output.putInt("EvaporationTimer", this.evaporationTimer);
//        output.putString("Content", this.content.name());
//    }
//
//    @Override
//    protected void loadAdditional(ValueInput input) {
//        super.loadAdditional(input);
//        this.hasBlockAbove = input.getBooleanOr("HasBlockAbove", false);
//        this.heatedBelow = input.getBooleanOr("HeatedBelow", false);
//        this.evaporationTimer = input.getIntOr("EvaporationTimer", EVAPORATION_INTERVAL_TICKS);
//        this.content = Content.valueOf(input.getStringOr("Content", Content.EMPTY.name()));
//    }
}