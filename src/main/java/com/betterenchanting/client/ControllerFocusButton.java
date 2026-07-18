package com.betterenchanting.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/** An invisible focus target that gives custom-drawn controls native keyboard/controller navigation. */
final class ControllerFocusButton extends AbstractButton {
    private static final int FOCUS_BORDER = 0xFFFFFFFF;
    private final Runnable action;

    ControllerFocusButton(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override
    public void onPress() {
        this.action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.isFocused()) {
            graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), FOCUS_BORDER);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
