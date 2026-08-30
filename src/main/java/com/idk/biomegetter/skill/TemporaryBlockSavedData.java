package com.idk.biomegetter.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Персистентное хранилище отложенных откатов блоков.
 */
public class TemporaryBlockSavedData extends SavedData {

    public record Entry(BlockPos pos, BlockState originalState, int ticksLeft) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                // полный BlockState (со свойствами). Если вдруг не заведётся — см. fallback ниже
                BlockState.CODEC.fieldOf("state").forGetter(Entry::originalState),
                Codec.INT.fieldOf("ticksLeft").forGetter(Entry::ticksLeft)
        ).apply(instance, Entry::new));
    }

    public static final Codec<TemporaryBlockSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(d -> d.entries)
    ).apply(instance, TemporaryBlockSavedData::new));

    public static final SavedDataType<TemporaryBlockSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("biomegetter", "temporary_blocks"),
            TemporaryBlockSavedData::new,
            CODEC,
            DataFixTypes.LEVEL // или другой подходящий из enum — смотри автодополнение
    );

    private final List<Entry> entries;

    public TemporaryBlockSavedData() {
        this(new ArrayList<>());
    }

    public TemporaryBlockSavedData(List<Entry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    public static TemporaryBlockSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Entry> entries() {
        return entries;
    }

    public void add(Entry entry) {
        entries.add(entry);
        setDirty();
    }

    public void replaceAll(List<Entry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        setDirty();
    }
}