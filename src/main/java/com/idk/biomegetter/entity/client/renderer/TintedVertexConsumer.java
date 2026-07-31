package com.idk.biomegetter.entity.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.ARGB;


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
        this.delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        this.delegate.setColor(
                (int) (r * this.tintR),
                (int) (g * this.tintG),
                (int) (b * this.tintB),
                (int) (a * this.tintA)
        );
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        int r = (int) (ARGB.red(color) * this.tintR);
        int g = (int) (ARGB.green(color) * this.tintG);
        int b = (int) (ARGB.blue(color) * this.tintB);
        int a = (int) (ARGB.alpha(color) * this.tintA);
        this.delegate.setColor(ARGB.color(a, r, g, b));
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        this.delegate.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        this.delegate.setLineWidth(width);
        return this;
    }
}