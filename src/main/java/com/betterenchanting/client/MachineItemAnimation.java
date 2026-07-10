package com.betterenchanting.client;

import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

final class MachineItemAnimation {
    private final ItemStack[] displayed;
    private final float[] reveal;
    private double lastRenderTime = Double.NaN;

    MachineItemAnimation(int slots) {
        this.displayed = new ItemStack[slots];
        this.reveal = new float[slots];
        for (int slot = 0; slot < slots; slot++) {
            this.displayed[slot] = ItemStack.EMPTY;
        }
    }

    double advance(Container container, double renderTime) {
        double delta = Double.isFinite(this.lastRenderTime)
                ? Mth.clamp(renderTime - this.lastRenderTime, 0.0D, 2.0D)
                : 0.05D;
        this.lastRenderTime = renderTime;
        float blend = smoothing(delta, 0.52D);
        for (int slot = 0; slot < this.displayed.length; slot++) {
            ItemStack current = container.getItem(slot);
            if (!current.isEmpty()) {
                boolean changed = this.displayed[slot].isEmpty()
                        || !ItemStack.isSameItemSameComponents(this.displayed[slot], current);
                this.displayed[slot] = current.copy();
                if (changed) {
                    this.reveal[slot] = Math.min(this.reveal[slot], 0.12F);
                }
                this.reveal[slot] = Mth.lerp(blend, this.reveal[slot], 1.0F);
            } else {
                this.reveal[slot] = Mth.lerp(blend, this.reveal[slot], 0.0F);
                if (this.reveal[slot] < 0.015F) {
                    this.reveal[slot] = 0.0F;
                    this.displayed[slot] = ItemStack.EMPTY;
                }
            }
        }
        return delta;
    }

    ItemStack displayed(int slot) {
        return this.displayed[slot];
    }

    float reveal(int slot) {
        return smoothStep(this.reveal[slot]);
    }

    static float smoothing(double deltaTicks, double responsePerTick) {
        return (float) (1.0D - Math.exp(-Math.max(0.0D, deltaTicks) * responsePerTick));
    }

    static float smoothStep(float value) {
        float clamped = Mth.clamp(value, 0.0F, 1.0F);
        return clamped * clamped * (3.0F - 2.0F * clamped);
    }
}
