package com.worldshaper.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.worldshaper.WorldshaperMod;

/**
 * GUI texture references matching Create's AllGuiTextures layout.
 * All textures are from textures/gui/widgets.png (256x256).
 */
public enum WSGuiTextures {

    // Button backgrounds (18x18)
    BUTTON(0, 0, 18, 18),
    BUTTON_HOVER(18, 0, 18, 18),
    BUTTON_DOWN(36, 0, 18, 18),
    BUTTON_GREEN(72, 0, 18, 18),
    BUTTON_DISABLED(90, 0, 18, 18),

    // Indicator (18x6) - below buttons to show selection state
    INDICATOR(0, 18, 18, 6),
    INDICATOR_WHITE(18, 18, 18, 6),
    INDICATOR_GREEN(36, 18, 18, 6),
    INDICATOR_YELLOW(54, 18, 18, 6),
    INDICATOR_RED(72, 18, 18, 6),

    // Terrainzapper background (from curiosities.png)
    TERRAINZAPPER_BG("curiosities", 0, 0, 234, 103),
    TERRAINZAPPER_INACTIVE_PARAM("curiosities", 238, 0, 18, 18);

    public static final ResourceLocation WIDGETS_ATLAS =
        ResourceLocation.fromNamespaceAndPath(WorldshaperMod.MOD_ID, "textures/gui/widgets.png");
    public static final ResourceLocation CURIOSITIES_ATLAS =
        ResourceLocation.fromNamespaceAndPath(WorldshaperMod.MOD_ID, "textures/gui/curiosities.png");

    public final int startX, startY, width, height;
    public final ResourceLocation atlas;

    WSGuiTextures(int startX, int startY, int width, int height) {
        this("widgets", startX, startY, width, height);
    }

    WSGuiTextures(String file, int startX, int startY, int width, int height) {
        this.atlas = ResourceLocation.fromNamespaceAndPath(
            com.worldshaper.WorldshaperMod.MOD_ID, "textures/gui/" + file + ".png");
        this.startX = startX;
        this.startY = startY;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(atlas, x, y, startX, startY, width, height);
    }
}
