package com.worldshaper.gui.widget;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public abstract class WSAbstractWidget extends AbstractWidget {

    protected List<Component> toolTip = new ArrayList<>();
    protected boolean isHovered;
    protected Runnable callback;

    public WSAbstractWidget(int x, int y, int w, int h, Component message) {
        super(x, y, w, h, message);
    }

    public WSAbstractWidget withCallback(Runnable callback) {
        this.callback = callback;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        isHovered = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + getHeight();
        doRender(graphics, mouseX, mouseY, partialTick);
    }

    protected abstract void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    public List<Component> getToolTip() {
        return toolTip;
    }

    public void setToolTip(Component text) {
        toolTip.clear();
        toolTip.add(text);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && mouseX >= getX() && mouseY >= getY() && mouseX < getX() + getWidth() && mouseY < getY() + getHeight();
    }

    protected Minecraft mc() {
        return Minecraft.getInstance();
    }
}
