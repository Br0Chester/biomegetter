package com.idk.biomegetter.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Общее состояние наших котлов: осведомлённость об окружении (кэш, а не live-сканирование)
 * и таймер испарения при кипении. Конкретные варианты содержимого (вода/молоко/...)
 * наследуются от этого класса при необходимости специфики.
 */
public class ModCauldronBlockEntity extends BlockEntity {

    public static final int EVAPORATION_INTERVAL_TICKS = 600; // 30 секунд

    public enum Content {
        EMPTY, WATER, LAVA, MILK, JUICE
    }

    private boolean hasBlockAbove;
    private boolean heatedBelow;
    private int evaporationTimer = EVAPORATION_INTERVAL_TICKS;
    private Content content = Content.EMPTY;

    public Content getContent() {
        return this.content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

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

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("HasBlockAbove", this.hasBlockAbove);
        output.putBoolean("HeatedBelow", this.heatedBelow);
        output.putInt("EvaporationTimer", this.evaporationTimer);
        output.putString("Content", this.content.name());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.hasBlockAbove = input.getBooleanOr("HasBlockAbove", false);
        this.heatedBelow = input.getBooleanOr("HeatedBelow", false);
        this.evaporationTimer = input.getIntOr("EvaporationTimer", EVAPORATION_INTERVAL_TICKS);
        this.content = Content.valueOf(input.getStringOr("Content", Content.EMPTY.name()));
    }
}