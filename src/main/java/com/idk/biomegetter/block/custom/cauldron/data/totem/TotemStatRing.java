package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.idk.biomegetter.block.custom.cauldron.data.RitualTemplate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Кольцо, изменяющее статы скилла — data/biomegetter/template_totem_stat_ring/*.json.
 * {@code level} — уровень кольца (суммарный уровень использованных за варку колец не может
 * превышать уровень тотема). {@code element} — элемент рисунка, сверяется с элементами
 * ингредиентов в котле через ElementMatrixLoader. {@code modifiers} — список дельт по статам
 * (пропуск отсутствующих у скилла статов — штатно, не ошибка).
 */
public record TotemStatRing(int level, List<TotemStatModifier> modifiers, RitualTemplate template) {
    public static final Codec<TotemStatRing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(TotemStatRing::level),
            TotemStatModifier.CODEC.listOf().fieldOf("modifiers").forGetter(TotemStatRing::modifiers),
            RitualTemplate.CODEC.fieldOf("template").forGetter(TotemStatRing::template)
    ).apply(instance, TotemStatRing::new));

    /**
     * Проверяет однородность элемента кольца по факту того, чем оно физически построено, и
     * возвращает единственный общий элемент — ЛИБО кольцо считается НЕВАЛИДНЫМ (не учитывается
     * вообще, будто не построено), если: (а) хотя бы одна позиция состоит из блока, не
     * классифицированного ни в одном block_element-файле, (б) позиции в сумме указывают на
     * более одного РАЗНОГО элемента одновременно, (в) блок сам числится сразу в нескольких
     * элементах (fire+earth) — такая "смешанная" клетка тоже считается неоднозначной и рушит
     * однородность всего кольца.
     */
    public java.util.Optional<String> resolveElement(net.minecraft.world.level.Level level, java.util.List<net.minecraft.core.BlockPos> matchedPositions) {
        java.util.Set<String> allElements = new java.util.HashSet<>();
        for (net.minecraft.core.BlockPos pos : matchedPositions) {
            java.util.Set<String> cellElements = BlockElementLoader.elementsOf(level.getBlockState(pos));
            if (cellElements.isEmpty())
                return java.util.Optional.empty(); // неклассифицированный блок — кольцо невалидно
            allElements.addAll(cellElements);
            if (allElements.size() > 1) return java.util.Optional.empty(); // ранний выход — уже смешение элементов
        }
        return allElements.size() == 1 ? java.util.Optional.of(allElements.iterator().next()) : java.util.Optional.empty();
    }
}