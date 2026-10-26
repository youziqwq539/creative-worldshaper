package com.worldshaper.gui.widget;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Indicator widget for showing state (ON/OFF/RED/YELLOW/GREEN).
 * Matches Create's Indicator widget behavior.
 */
public class Indicator extends WSAbstractWidget {

    public State state;

    public Indicator(int x, int y, Component tooltip) {
        super(x, y, WSGuiTextures.INDICATOR.width, WSGuiTextures.INDICATOR.height, Component.empty());
        this.toolTip = tooltip == null || tooltip.getString().isEmpty() ? ImmutableList.of() : ImmutableList.of(tooltip);
        this.state = State.OFF;
    }

    @Override
    protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible)
            return;

        WSGuiTextures toDraw;
        switch (state) {
            case ON: toDraw = WSGuiTextures.INDICATOR_WHITE; break;
            case OFF: toDraw = WSGuiTextures.INDICATOR; break;
            case RED: toDraw = WSGuiTextures.INDICATOR_RED; break;
            case YELLOW: toDraw = WSGuiTextures.INDICATOR_YELLOW; break;
            case GREEN: toDraw = WSGuiTextures.INDICATOR_GREEN; break;
            default: toDraw = WSGuiTextures.INDICATOR; break;
        }
        toDraw.render(graphics, getX(), getY());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Indicators don't respond to clicks
        return false;
    }

    public enum State {
        OFF, ON,
        RED, YELLOW, GREEN;
    }
}