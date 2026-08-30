package com.idk.biomegetter.block.custom.cauldron.data;

import com.idk.biomegetter.block.custom.cauldron.data.totem.TotemStatRingResolver;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Ритуальная структура блоков вокруг котла — data/biomegetter/template_rituals/*.json.
 * Поддерживает КОМБИНИРОВАНИЕ нескольких шаблонов (см. {@link #matchComposite}) — несколько
 * фигур накладываются друг на друга в заданном порядке (последняя в списке побеждает при
 * совпадении позиций), после чего объединённая структура целиком проверяется во всех 8
 * ориентациях (4 поворота × зеркало), КАК ОДНО ЦЕЛОЕ — то есть все части комбинированной
 * структуры развёрнуты синхронно друг с другом, а не независимо. Это делает проверку
 * комбинированного ритуала не дороже одиночного (O(8) вне зависимости от числа шаблонов).
 */
public record RitualTemplate(java.util.List<RitualLayer> layers, Map<String, RitualKey> keys) {
    public static final Codec<RitualTemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RitualLayer.CODEC.listOf().fieldOf("layers").forGetter(RitualTemplate::layers),
            Codec.unboundedMap(Codec.STRING, RitualKey.CODEC).fieldOf("keys").forGetter(RitualTemplate::keys)
    ).apply(instance, RitualTemplate::new));

    /**
     * Одна "каноническая" (ещё не повёрнутая) ячейка шаблона — координаты относительно точки
     * привязки котла плюс уже разрешённое требование (символ уже заменён на реальный RitualKey
     * из СВОЕГО keys этого конкретного шаблона — коллизий имён символов между разными
     * шаблонами при этом не возникает).
     */
    public record CanonicalCell(int dRow, int dCol, int yOffset, RitualKey key) {
    }

    /**
     * Ключ ячейки в объединённой карте (без ссылки на конкретный требуемый блок) — используется
     * ТОЛЬКО для определения "это та же самая позиция", чтобы более поздний шаблон в списке мог
     * перезаписать требование более раннего.
     */
    private record CellCoord(int dRow, int dCol, int yOffset) {
    }

    /**
     * @return канонические ячейки этого шаблона, либо пусто (empty), если у шаблона нет точки
     * привязки котла (ошибка датапака) ИЛИ в паттерне встретился неизвестный символ.
     */
    public java.util.Optional<java.util.List<CanonicalCell>> toCanonicalCells() {
        int[] anchor = findAnchor();
        if (anchor == null) return java.util.Optional.empty();

        java.util.List<CanonicalCell> result = new java.util.ArrayList<>();
        for (RitualLayer layer : layers) {
            java.util.List<String> pattern = layer.pattern();
            for (int row = 0; row < pattern.size(); row++) {
                String line = pattern.get(row);
                for (int col = 0; col < line.length(); col++) {
                    if (row == anchor[0] && col == anchor[1]) continue; // клетка самого котла

                    char symbol = line.charAt(col);
                    if (symbol == '_') continue;

                    RitualKey key = keys.get(String.valueOf(symbol));
                    if (key == null) return java.util.Optional.empty(); // неизвестный символ — весь шаблон невалиден

                    result.add(new CanonicalCell(row - anchor[0], col - anchor[1], layer.yOffset(), key));
                }
            }
        }
        return java.util.Optional.of(result);
    }

    private int[] findAnchor() {
        for (RitualLayer layer : layers) {
            if (layer.cauldronRow().isPresent() && layer.cauldronCol().isPresent()) {
                return new int[]{layer.cauldronRow().get(), layer.cauldronCol().get()};
            }
        }
        return null;
    }

    /**
     * Одиночная проверка (обёртка над {@link #matchComposite} со списком из одного элемента) —
     * сохранена для мест, где комбинирование не нужно (например {@code area} в
     * BlockConsumptionRule).
     */
    public java.util.Optional<java.util.List<BlockPos>> match(Level level, BlockPos cauldronPos) {
        return matchComposite(java.util.List.of(this), level, cauldronPos);
    }

    /**
     * Накладывает список шаблонов друг на друга (в порядке списка — ПОСЛЕДНИЙ побеждает при
     * совпадении позиции), затем проверяет получившуюся объединённую структуру во всех 8
     * ориентациях КАК ОДНО ЦЕЛОЕ.
     *
     * @return позиции, реально проверенные и совпавшие в мире — при первой подошедшей
     * ориентации; empty — ни одна ориентация не подошла (или список пуст/содержит
     * невалидный шаблон).
     */
    public static java.util.Optional<java.util.List<BlockPos>> matchComposite(
            java.util.List<RitualTemplate> templates, Level level, BlockPos cauldronPos
    ) {
        Map<CellCoord, RitualKey> merged = new java.util.LinkedHashMap<>();
        for (RitualTemplate template : templates) {
            var cellsOpt = template.toCanonicalCells();
            if (cellsOpt.isEmpty()) return java.util.Optional.empty(); // невалидный шаблон — весь composite невалиден
            for (CanonicalCell cell : cellsOpt.get()) {
                merged.put(new CellCoord(cell.dRow(), cell.dCol(), cell.yOffset()), cell.key());
            }
        }
        if (merged.isEmpty()) return java.util.Optional.empty();

        for (boolean mirror : new boolean[]{false, true}) {
            for (int rotation = 0; rotation < 4; rotation++) {
                java.util.List<BlockPos> positions = tryMergedOrientation(merged, level, cauldronPos, mirror, rotation);
                if (positions != null) return java.util.Optional.of(positions);
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * Как {@link #match}, но позиции из {@code claimed} при проверке считаются "подходят
     * всегда" (не требуют совпадения с реальным блоком в мире) — используется
     * {@link TotemStatRingResolver} для разрешения "каши": уже закреплённые за более крупными
     * кольцами клетки не мешают более мелким кандидатам занимать оставшееся место рядом.
     */
    public java.util.Optional<java.util.List<BlockPos>> matchIgnoringClaimed(
            Level level, BlockPos cauldronPos, java.util.Set<BlockPos> claimed
    ) {
        int[] anchor = findAnchor();
        if (anchor == null) return java.util.Optional.empty();

        for (boolean mirror : new boolean[]{false, true}) {
            for (int rotation = 0; rotation < 4; rotation++) {
                java.util.List<BlockPos> positions = tryOrientationIgnoringClaimed(level, cauldronPos, anchor[0], anchor[1], mirror, rotation, claimed);
                if (positions != null) return java.util.Optional.of(positions);
            }
        }
        return java.util.Optional.empty();
    }

    private java.util.List<BlockPos> tryOrientationIgnoringClaimed(
            Level level, BlockPos cauldronPos, int anchorRow, int anchorCol, boolean mirror, int rotation,
            java.util.Set<BlockPos> claimed
    ) {
        java.util.List<BlockPos> matched = new java.util.ArrayList<>();
        for (RitualLayer layer : layers) {
            java.util.List<String> pattern = layer.pattern();
            for (int row = 0; row < pattern.size(); row++) {
                String line = pattern.get(row);
                for (int col = 0; col < line.length(); col++) {
                    if (row == anchorRow && col == anchorCol) continue;

                    char symbol = line.charAt(col);
                    if (symbol == '_') continue;

                    RitualKey key = keys.get(String.valueOf(symbol));
                    if (key == null) return null;

                    int dRow = row - anchorRow;
                    int dCol = col - anchorCol;
                    int[] world = transform(dRow, dCol, mirror, rotation);
                    BlockPos checkPos = cauldronPos.offset(world[0], layer.yOffset(), world[1]);

                    if (!claimed.contains(checkPos)) {
                        BlockState state = level.getBlockState(checkPos);
                        if (!key.matches(state)) return null;
                    }

                    matched.add(checkPos);
                }
            }
        }
        return matched;
    }

    private static java.util.List<BlockPos> tryMergedOrientation(
            Map<CellCoord, RitualKey> merged, Level level, BlockPos cauldronPos, boolean mirror, int rotation
    ) {
        java.util.List<BlockPos> matched = new java.util.ArrayList<>();
        for (Map.Entry<CellCoord, RitualKey> entry : merged.entrySet()) {
            CellCoord coord = entry.getKey();
            int[] world = transform(coord.dRow(), coord.dCol(), mirror, rotation);
            BlockPos checkPos = cauldronPos.offset(world[0], coord.yOffset(), world[1]);
            BlockState state = level.getBlockState(checkPos);
            if (!entry.getValue().matches(state)) return null;
            matched.add(checkPos);
        }
        return matched;
    }

    private static int[] transform(int dRow, int dCol, boolean mirror, int rotation) {
        int dx = mirror ? -dCol : dCol;
        int dz = dRow;
        for (int i = 0; i < rotation; i++) {
            int newDx = -dz;
            int newDz = dx;
            dx = newDx;
            dz = newDz;
        }
        return new int[]{dx, dz};
    }
}