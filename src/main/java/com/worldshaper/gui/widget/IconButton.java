package com.worldshaper.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.CommonComponents;

import org.lwjgl.glfw.GLFW;

public class IconButton extends WSAbstractWidget {

    protected WSIcons icon;
    public boolean green;

    public IconButton(int x, int y, WSIcons icon) {
        super(x, y, 18, 18, CommonComponents.EMPTY);
        this.icon = icon;
    }

    public IconButton(int x, int y, int w, int h, WSIcons icon) {
        super(x, y, w, h, CommonComponents.EMPTY);
        this.icon = icon;
    }

    @Override
    protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        boolean mouseDown = GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        WSGuiTextures button = !active ? WSGuiTextures.BUTTON_DISABLED
            : isHovered && mouseDown ? WSGuiTextures.BUTTON_DOWN
                : isHovered ? WSGuiTextures.BUTTON_HOVER
                    : green ? WSGuiTextures.BUTTON_GREEN : WSGuiTextures.BUTTON;

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        button.render(graphics, getX(), getY());
        if (icon != null)
            icon.render(graphics, getX() + 1, getY() + 1);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (callback != null) callback.run();
    }

    public void setIcon(WSIcons icon) {
        this.icon = icon;
    }
}
