package com.idk.biomegetter.block.entity;

import com.idk.biomegetter.block.custom.cauldron.data.PressableSolid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

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

    @Nullable
    private Identifier juiceType;
    private int juiceLevel;
    @Nullable
    private Identifier solidType;
    private int solidLevel;

    @Nullable
    public Identifier getJuiceType() {
        return this.juiceType;
    }

    public int getJuiceLevel() {
        return this.juiceLevel;
    }

    @Nullable
    public Identifier getSolidType() {
        return this.solidType;
    }

    public enum Content implements StringRepresentable {
        EMPTY("empty"), WATER("water"), LAVA("lava"), POWDER_SNOW("powder_snow"), MILK("milk"), JUICE("juice");

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

    public int getSolidLevel() {
        return this.solidLevel;
    }

    public boolean canAddSolid(Identifier candidateJuice) {
        if (this.solidLevel >= 3) return false;
        return this.juiceType == null || this.juiceType.equals(candidateJuice);
    }

    public void addSolid(PressableSolid solid) {
        if (this.juiceType == null) {
            this.juiceType = solid.producesJuice();
        }
        this.solidType = solid.solidTexture();
        this.solidLevel = Math.min(3, this.solidLevel + 1);
        this.setChanged();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(worldPosition);
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /**
     * @return true, если осадок только что полностью выдавлен (пора выдать предмет-остаток)
     */
    public boolean pressSolidOnce() {
        if (this.solidLevel <= 0) return false;
        this.solidLevel--;
        boolean finished = this.solidLevel == 0;
        if (finished) {
            this.juiceLevel = Math.min(3, this.juiceLevel + 1);
        }
        this.setChanged();

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(worldPosition);
            // или
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        return finished;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries); // или руками putInt/putString
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("HasBlockAbove", this.hasBlockAbove);
        output.putBoolean("HeatedBelow", this.heatedBelow);
        output.putInt("EvaporationTimer", this.evaporationTimer);
        output.putBoolean("PowderSnowAbove", this.powderSnowAbove);
        output.putInt("MeltTimer", this.meltTimer);

        output.putInt("JuiceLevel", this.juiceLevel);
        output.putInt("SolidLevel", this.solidLevel);
        if (this.juiceType != null) {
            output.putString("JuiceType", this.juiceType.toString());
        }
        if (this.solidType != null) {
            output.putString("SolidType", this.solidType.toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.hasBlockAbove = input.getBooleanOr("HasBlockAbove", false);
        this.heatedBelow = input.getBooleanOr("HeatedBelow", false);
        this.evaporationTimer = input.getIntOr("EvaporationTimer", EVAPORATION_INTERVAL_TICKS);
        this.powderSnowAbove = input.getBooleanOr("PowderSnowAbove", false);
        this.meltTimer = input.getIntOr("MeltTimer", MELT_INTERVAL_TICKS);

        this.juiceLevel = input.getIntOr("JuiceLevel", 0);
        this.solidLevel = input.getIntOr("SolidLevel", 0);
        this.juiceType = input.getString("JuiceType").map(Identifier::parse).orElse(null);
        this.solidType = input.getString("SolidType").map(Identifier::parse).orElse(null);
    }
}