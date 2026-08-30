package com.idk.biomegetter.block.custom.cauldron.data.totem;

import com.idk.biomegetter.BiomeGetter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Загружает направленные связи элементов (data/biomegetter/element_matrix/*.json) —
 * каждый файл описывает произвольное число {@link ElementRelation}-записей (список в файле,
 * либо файл на одну запись — оба варианта поддержаны через {@code listOf().codec()}, см. ниже).
 */
public class ElementMatrixLoader extends SimpleJsonResourceReloadListener<java.util.List<ElementRelation>>
        implements IdentifiableResourceReloadListener {

    private static Map<String, Map<String, Integer>> MATRIX = Map.of();

    public ElementMatrixLoader() {
        super(ElementRelation.CODEC.listOf(), FileToIdConverter.json("element_matrix"));
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath(BiomeGetter.MOD_ID, "element_matrix_loader");
    }

    @Override
    protected void apply(Map<Identifier, java.util.List<ElementRelation>> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, Map<String, Integer>> matrix = new HashMap<>();
        for (java.util.List<ElementRelation> relations : data.values()) {
            for (ElementRelation rel : relations) {
                matrix.computeIfAbsent(rel.source(), k -> new HashMap<>()).put(rel.target(), rel.relation());
            }
        }
        MATRIX = matrix;
        BiomeGetter.LOGGER.info("Loaded element matrix: {} source elements", matrix.size());
        validateSymmetry(matrix);
    }

    /**
     * По вашему замыслу связи должны быть зеркальны (source→target и target→source равны).
     * Несимметричные пары не являются ошибкой загрузки — просто предупреждаем в лог, чтобы
     * авторы датапака могли исправить опечатку, не роняя игру.
     */
    private static void validateSymmetry(Map<String, Map<String, Integer>> matrix) {
        Set<String> checked = new java.util.HashSet<>();
        for (var sourceEntry : matrix.entrySet()) {
            for (var targetEntry : sourceEntry.getValue().entrySet()) {
                String source = sourceEntry.getKey();
                String target = targetEntry.getKey();
                String pairKey = source.compareTo(target) < 0 ? source + "|" + target : target + "|" + source;
                if (!checked.add(pairKey)) continue;

                int forward = targetEntry.getValue();
                int backward = matrix.getOrDefault(target, Map.of()).getOrDefault(source, 0);
                if (forward != backward) {
                    BiomeGetter.LOGGER.warn("Element matrix asymmetry: {} -> {} = {}, but {} -> {} = {}",
                            source, target, forward, target, source, backward);
                }
            }
        }
    }

    /**
     * Считает суммарный модификатор рисунка элемента {@code ringElement} против набора
     * элементов, присутствующих в котле — реализует формулу:
     * сумма>0 → множитель=сумма (прямое применение); сумма==0 → аннулировано (0);
     * сумма<0 → множитель=|сумма|, эффект инвертируется (см. вызывающий код на предмет
     * интерпретации знака).
     *
     * @return целое число: положительное = прямой множитель, 0 = аннулировано,
     * отрицательное = инвертированный множитель (|значение|)
     */
    public static int computeModifier(Set<String> cauldronElements, String ringElement) {
        int sum = 0;
        for (String cauldronElement : cauldronElements) {
            sum += MATRIX.getOrDefault(cauldronElement, Map.of()).getOrDefault(ringElement, 0);
        }
        return sum;
    }
}