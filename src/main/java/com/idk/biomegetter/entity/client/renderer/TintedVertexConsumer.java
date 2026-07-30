package com.idk.biomegetter.entity.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Обёртка над VertexConsumer, умножающая цвет и альфу каждой отправленной вершины
 * на фиксированный тон. Используется для эффекта "полупрозрачный призрачный союзник".
 * Всё остальное (позиция, UV, нормали, свет) передаётся как есть.
 */
public class TintedVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float tintR;
    private final float tintG;
    private final float tintB;
    private final float tintA;

    public TintedVertexConsumer(VertexConsumer delegate, float tintR, float tintG, float tintB, float tintA) {
        this.delegate = delegate;
        this.tintR = tintR;
        this.tintG = tintG;
        this.tintB = tintB;
        this.tintA = tintA;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        return this.delegate.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        return this.delegate.setColor(
                (int) (r * this.tintR),
                (int) (g * this.tintG),
                (int) (b * this.tintB),
                (int) (a * this.tintA)
        );
    }

    @Override
    public VertexConsumer setColor(int color) {
        return null;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return this.delegate.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this.delegate.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return this.delegate.setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        return this.delegate.setNormal(x, y, z);
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        return null;
    }
}