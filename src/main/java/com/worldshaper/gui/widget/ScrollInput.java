package com.worldshaper.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Scrollable input widget for numeric parameter selection.
 * Matches Create's ScrollInput behavior.
 */
public class ScrollInput extends WSAbstractWidget {

    protected Consumer<Integer> onScroll;
    protected int state;
    protected Component title;
    protected Component hint;
    protected final Component scrollToModify = Component.translatable("worldshaper.gui.scrollInput.scrollToModify");
    protected final Component shiftScrollsFaster = Component.translatable("worldshaper.gui.scrollInput.shiftScrollsFaster");
    protected Label displayLabel;
    protected boolean inverted;
    protected boolean soundPlayed;
    protected Function<Integer, Component> formatter;

    protected int min, max;
    protected int shiftStep;

    public static final int HEADER_RGB = 0xFFFFFF;
    public static final int HINT_RGB = 0x999999;

    public ScrollInput(int x, int y, int w, int h) {
        super(x, y, w, h, CommonComponents.EMPTY);
        state = 0;
        min = 0;
        max = 1;
        shiftStep = 5;
        formatter = i -> Component.literal(String.valueOf(i));
        soundPlayed = false;
    }

    public ScrollInput inverted() {
        inverted = true;
        return this;
    }

    public ScrollInput withRange(int min, int max) {
        this.min = min;
        this.max = max;
        return this;
    }

    public ScrollInput calling(Consumer<Integer> onScroll) {
        this.onScroll = onScroll;
        return this;
    }

    public ScrollInput format(Function<Integer, Component> formatter) {
        this.formatter = formatter;
        return this;
    }

    public ScrollInput titled(Component title) {
        this.title = title;
        updateTooltip();
        return this;
    }

    public ScrollInput addHint(MutableComponent hint) {
        this.hint = hint;
        updateTooltip();
        return this;
    }

    public ScrollInput writingTo(Label label) {
        this.displayLabel = label;
        if (label != null)
            writeToLabel();
        return this;
    }

    public ScrollInput withShiftStep(int step) {
        shiftStep = step;
        return this;
    }

    public int getState() {
        return state;
    }

    public ScrollInput setState(int state) {
        this.state = state;
        clampState();
        updateTooltip();
        if (displayLabel != null)
            writeToLabel();
        return this;
    }

    @Override
    protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Transparent background - the value is rendered by the parent screen or label
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inverted)
            scrollY *= -1;

        int priorState = state;
        boolean shifted = Screen.hasShiftDown();
        int step = (int) Math.signum(scrollY);
        if (shifted) step *= shiftStep;

        state += step;
        if (shifted)
            state -= state % shiftStep;

        clampState();

        if (priorState != state) {
            onChanged();
        }

        return priorState != state;
    }

    protected void clampState() {
        if (state >= max) state = max - 1;
        if (state < min) state = min;
    }

    public void onChanged() {
        if (displayLabel != null)
            writeToLabel();
        if (onScroll != null)
            onScroll.accept(state);
        updateTooltip();
    }

    protected void writeToLabel() {
        displayLabel.text = formatter.apply(state);
        // Update label position to center the text
        displayLabel.setX(getX() + 14 - Minecraft.getInstance().font.width(displayLabel.text) / 2);
    }

    protected void updateTooltip() {
        toolTip.clear();
        if (title == null)
            return;

        toolTip.add(title.plainCopy().withStyle(s -> s.withColor(HEADER_RGB)));
        if (hint != null)
            toolTip.add(hint.plainCopy().withStyle(s -> s.withColor(HINT_RGB)));
        toolTip.add(scrollToModify.plainCopy().withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
        toolTip.add(shiftScrollsFaster.plainCopy().withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
    }

    @Override
    public List<Component> getToolTip() {
        return toolTip;
    }
}
