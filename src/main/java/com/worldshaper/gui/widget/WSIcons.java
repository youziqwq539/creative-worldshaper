package com.worldshaper.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.worldshaper.WorldshaperMod;

/**
 * Icon references matching Create's AllIcons layout.
 * Icons are 16x16 in a 256x256 atlas (textures/gui/icons.png).
 * Each row has up to 16 icons, indexed by (col, row).
 */
public enum WSIcons {

    // Row 1 (y=1): confirm
    I_CONFIRM(0, 1),

    // Row 2 (y=2): tools and placement
    I_CENTERED(4, 2),
    I_ATTACHED(5, 2),
    I_INSERTED(6, 2),
    I_FILL(7, 2),
    I_PLACE(8, 2),
    I_REPLACE(9, 2),
    I_CLEAR(10, 2),
    I_OVERLAY(11, 2),
    I_FLATTEN(12, 2),

    // Row 3 (y=3): undo/redo
    I_UNDO(0, 3),
    I_REDO(1, 3),

    // Row 6 (y=6): patterns
    I_PATTERN_SOLID(0, 6),
    I_PATTERN_CHECKERED(1, 6),
    I_PATTERN_CHECKERED_INVERSED(2, 6),
    I_PATTERN_CHANCE_25(3, 6),

    // Row 7 (y=7): more patterns + connectivity
    I_PATTERN_CHANCE_50(0, 7),
    I_PATTERN_CHANCE_75(1, 7),
    I_FOLLOW_DIAGONAL(2, 7),
    I_FOLLOW_MATERIAL(3, 7);

    public static final ResourceLocation ICON_ATLAS =
        ResourceLocation.fromNamespaceAndPath(WorldshaperMod.MOD_ID, "textures/gui/icons.png");
    public static final int ATLAS_SIZE = 256;

    public final int iconX;
    public final int iconY;

    WSIcons(int col, int row) {
        this.iconX = col * 16;
        this.iconY = row * 16;
    }

    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(ICON_ATLAS, x, y, 0, iconX, iconY, 16, 16, ATLAS_SIZE, ATLAS_SIZE);
    }
}
