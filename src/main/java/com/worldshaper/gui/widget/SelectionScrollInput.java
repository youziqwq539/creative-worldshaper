package com.worldshaper.gui.widget;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Scrollable selection input widget for choosing from a list of options.
 * Matches Create's SelectionScrollInput behavior.
 */
public class SelectionScrollInput extends ScrollInput {

    protected List<? extends Component> options;

    public SelectionScrollInput(int x, int y, int w, int h) {
        super(x, y, w, h);
        options = new ArrayList<>();
        inverted();
    }

    public SelectionScrollInput forOptions(List<? extends Component> options) {
        this.options = options;
        this.max = options.size();
        format(options::get);
        updateTooltip();
        return this;
    }

    @Override
    protected void updateTooltip() {
        toolTip.clear();
        if (title == null)
            return;

        toolTip.add(title.plainCopy().withStyle(s -> s.withColor(HEADER_RGB)));

        int min = Math.min(this.max - 16, state - 7);
        int max = Math.max(this.min + 16, state + 8);
        min = Math.max(min, this.min);
        max = Math.min(max, this.max);

        if (this.min + 1 == min)
            min--;
        if (min > this.min) {
            toolTip.add(Component.literal("> ...").withStyle(ChatFormatting.GRAY));
        }

        if (this.max - 1 == max)
            max++;

        for (int i = min; i < max; i++) {
            if (i == state)
                toolTip.add(Component.empty()
                    .append("-> ")
                    .append(options.get(i))
                    .withStyle(ChatFormatting.WHITE));
            else
                toolTip.add(Component.empty()
                    .append("> ")
                    .append(options.get(i))
                    .withStyle(ChatFormatting.GRAY));
        }

        if (max < this.max) {
            toolTip.add(Component.literal("> ...").withStyle(ChatFormatting.GRAY));
        }

        if (hint != null)
            toolTip.add(hint.plainCopy().withStyle(s -> s.withColor(HINT_RGB)));

        toolTip.add(Component.translatable("worldshaper.gui.scrollInput.scrollToSelect")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}