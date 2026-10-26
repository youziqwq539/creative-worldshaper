package com.worldshaper.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.worldshaper.WSDataComponents;
import com.worldshaper.brush.Brush;
import com.worldshaper.brush.PlacementOptions;
import com.worldshaper.brush.PlacementPatterns;
import com.worldshaper.brush.TerrainBrushes;
import com.worldshaper.brush.TerrainTools;
import com.worldshaper.gui.widget.IconButton;
import com.worldshaper.gui.widget.Indicator;
import com.worldshaper.gui.widget.Label;
import com.worldshaper.gui.widget.ScrollInput;
import com.worldshaper.gui.widget.SelectionScrollInput;
import com.worldshaper.gui.widget.WSGuiTextures;
import com.worldshaper.gui.widget.WSIcons;
import com.worldshaper.item.WorldshaperItem;
import com.worldshaper.net.ConfigureWorldshaperPacket;
import com.worldshaper.net.UndoRedoPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.network.PacketDistributor;

public class WorldshaperScreen extends Screen {

    // Colors matching Create
    private static final int FONT_COLOR = 0x767676;
    private static final int TITLE_COLOR = 0x54214F;
    private static final int BRIGHT_COLOR = 0xFEFEFE;

    // Background texture dimensions
    private static final int BG_WIDTH = 234;
    private static final int BG_HEIGHT = 103;

    protected final ItemStack zapper;
    protected final InteractionHand hand;
    protected int guiLeft;
    protected int guiTop;

    // State
    protected TerrainBrushes currentBrush;
    protected int[] currentBrushParams = new int[]{1, 1, 1};
    protected boolean currentFollowDiagonals;
    protected boolean currentAcrossMaterials;
    protected TerrainTools currentTool;
    protected PlacementOptions currentPlacement;
    protected PlacementPatterns currentPattern;

    // Section labels
    protected final Component placementSection = Component.translatable("worldshaper.gui.placement");
    protected final Component toolSection = Component.translatable("worldshaper.gui.tool");
    protected final Component patternSection = Component.translatable("worldshaper.gui.patternSection");
    protected final List<Component> brushOptions;

    // Widgets
    protected ScrollInput brushInput;
    protected Label brushLabel;
    protected List<ScrollInput> brushParams = new ArrayList<>(3);
    protected List<Label> brushParamLabels = new ArrayList<>(3);
    protected List<IconButton> toolButtons;
    protected List<IconButton> placementButtons;
    protected List<IconButton> patternButtons = new ArrayList<>(6);
    protected IconButton followDiagonals;
    protected IconButton acrossMaterials;
    protected Indicator followDiagonalsIndicator;
    protected Indicator acrossMaterialsIndicator;
    protected IconButton confirmButton;

    public WorldshaperScreen(ItemStack zapper, InteractionHand hand) {
        super(zapper.getHoverName());
        this.zapper = zapper;
        this.hand = hand;

        brushOptions = new ArrayList<>();
        for (TerrainBrushes b : TerrainBrushes.values()) {
            brushOptions.add(Component.translatable("worldshaper.gui.brush." + b.getSerializedName()));
        }

        currentBrush = zapper.getOrDefault(WSDataComponents.SHAPER_BRUSH.get(), TerrainBrushes.Cuboid);
        if (zapper.has(WSDataComponents.SHAPER_BRUSH_PARAMS.get())) {
            BlockPos paramsData = zapper.get(WSDataComponents.SHAPER_BRUSH_PARAMS.get());
            currentBrushParams[0] = paramsData.getX();
            currentBrushParams[1] = paramsData.getY();
            currentBrushParams[2] = paramsData.getZ();
            if (currentBrushParams[1] == 0) {
                currentFollowDiagonals = true;
            }
            if (currentBrushParams[2] == 0) {
                currentAcrossMaterials = true;
            }
        }
        currentTool = zapper.getOrDefault(WSDataComponents.SHAPER_TOOL.get(), TerrainTools.Fill);
        currentPlacement = zapper.getOrDefault(WSDataComponents.SHAPER_PLACEMENT_OPTIONS.get(), PlacementOptions.Merged);
        currentPattern = zapper.getOrDefault(WSDataComponents.PLACEMENT_PATTERN.get(), PlacementPatterns.Solid);
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - BG_WIDTH) / 2;
        guiTop = (height - BG_HEIGHT) / 2;

        clearWidgets();
        brushParams.clear();
        brushParamLabels.clear();
        patternButtons.clear();

        int x = guiLeft;
        int y = guiTop;

        // Confirm button
        confirmButton = new IconButton(x + BG_WIDTH - 33, y + BG_HEIGHT - 24, WSIcons.I_CONFIRM);
        confirmButton.withCallback(() -> onClose());
        addRenderableWidget(confirmButton);

        // Undo/Redo buttons (bottom row, separate from other buttons)
        IconButton undoButton = new IconButton(x + 7, y + BG_HEIGHT + 5, WSIcons.I_UNDO);
        undoButton.withCallback(() -> {
            PacketDistributor.sendToServer(new UndoRedoPacket(hand, true));
        });
        undoButton.setToolTip(Component.translatable("worldshaper.gui.undo"));
        addRenderableWidget(undoButton);

        IconButton redoButton = new IconButton(x + 25, y + BG_HEIGHT + 5, WSIcons.I_REDO);
        redoButton.withCallback(() -> {
            PacketDistributor.sendToServer(new UndoRedoPacket(hand, false));
        });
        redoButton.setToolTip(Component.translatable("worldshaper.gui.redo"));
        addRenderableWidget(redoButton);

        // Brush label and selection scroll input
        brushLabel = new Label(x + 71, y + 25, CommonComponents.EMPTY).withShadow();
        brushInput = new SelectionScrollInput(x + 56, y + 20, 77, 18)
            .forOptions(brushOptions)
            .titled(Component.translatable("worldshaper.gui.brush"))
            .writingTo(brushLabel)
            .calling(brushIndex -> {
                currentBrush = TerrainBrushes.values()[brushIndex];
                initBrushParams(x, y);
            });
        brushInput.setState(currentBrush.ordinal());

        addRenderableWidget(brushLabel);
        addRenderableWidget(brushInput);

        // Pattern buttons (2 rows x 3 cols)
        patternButtons.clear();
        for (int row = 0; row <= 1; row++) {
            for (int col = 0; col <= 2; col++) {
                int id = patternButtons.size();
                if (id >= PlacementPatterns.values().length) break;
                PlacementPatterns pattern = PlacementPatterns.values()[id];
                WSIcons icon = getPatternIcon(pattern);
                IconButton patternButton = new IconButton(x + BG_WIDTH - 76 + col * 18, y + 21 + row * 18, icon);
                patternButton.withCallback(() -> {
                    patternButtons.forEach(b -> b.green = false);
                    patternButton.green = true;
                    currentPattern = pattern;
                });
                patternButton.setToolTip(Component.translatable("worldshaper.gui.pattern." + pattern.translationKey));
                patternButtons.add(patternButton);
            }
        }
        patternButtons.get(currentPattern.ordinal()).green = true;
        for (IconButton btn : patternButtons) addRenderableWidget(btn);

        // Brush params
        initBrushParams(x, y);
    }

    protected void initBrushParams(int x, int y) {
        Brush brush = currentBrush.get();

        // Remove old param widgets
        for (Label label : brushParamLabels) removeWidget(label);
        for (ScrollInput si : brushParams) removeWidget(si);
        brushParamLabels.clear();
        brushParams.clear();

        if (followDiagonals != null) {
            removeWidget(followDiagonals);
            removeWidget(followDiagonalsIndicator);
            followDiagonals = null;
            followDiagonalsIndicator = null;
        }
        if (acrossMaterials != null) {
            removeWidget(acrossMaterials);
            removeWidget(acrossMaterialsIndicator);
            acrossMaterials = null;
            acrossMaterialsIndicator = null;
        }
        if (toolButtons != null) {
            for (IconButton btn : toolButtons) removeWidget(btn);
            toolButtons.clear();
        }
        if (placementButtons != null) {
            for (IconButton btn : placementButtons) removeWidget(btn);
            placementButtons.clear();
        }

        // Brush parameter scroll inputs with labels
        for (int index = 0; index < 3; index++) {
            Label label = new Label(x + 65 + 20 * index, y + 45, CommonComponents.EMPTY).withShadow();

            final int finalIndex = index;
            ScrollInput input = new ScrollInput(x + 56 + 20 * index, y + 40, 18, 18)
                .withRange(brush.getMin(index), brush.getMax(index) + 1)
                .writingTo(label)
                .titled(brush.getParamLabel(index).plainCopy())
                .calling(state -> {
                    currentBrushParams[finalIndex] = state;
                    label.setX(x + 65 + 20 * finalIndex - font.width(label.text) / 2);
                });
            input.setState(currentBrushParams[index]);
            input.onChanged();

            if (index >= brush.amtParams) {
                input.visible = false;
                label.visible = false;
                input.active = false;
            }

            brushParamLabels.add(label);
            brushParams.add(input);
        }

        for (Label label : brushParamLabels) addRenderableWidget(label);
        for (ScrollInput input : brushParams) addRenderableWidget(input);

        // Connectivity options (for DynamicBrush)
        if (brush.hasConnectivityOptions()) {
            int x1 = x + 7 + 4 * 18;
            int y1 = y + 79;
            followDiagonalsIndicator = new Indicator(x1, y1 - 6, CommonComponents.EMPTY);
            followDiagonals = new IconButton(x1, y1, WSIcons.I_FOLLOW_DIAGONAL);
            x1 += 18;
            acrossMaterialsIndicator = new Indicator(x1, y1 - 6, CommonComponents.EMPTY);
            acrossMaterials = new IconButton(x1, y1, WSIcons.I_FOLLOW_MATERIAL);

            followDiagonals.withCallback(() -> {
                followDiagonalsIndicator.state = followDiagonalsIndicator.state == Indicator.State.OFF ? Indicator.State.ON : Indicator.State.OFF;
                currentFollowDiagonals = !currentFollowDiagonals;
            });
            followDiagonals.setToolTip(Component.translatable("worldshaper.gui.searchDiagonal"));
            acrossMaterials.withCallback(() -> {
                acrossMaterialsIndicator.state = acrossMaterialsIndicator.state == Indicator.State.OFF ? Indicator.State.ON : Indicator.State.OFF;
                currentAcrossMaterials = !currentAcrossMaterials;
            });
            acrossMaterials.setToolTip(Component.translatable("worldshaper.gui.searchFuzzy"));

            addRenderableWidget(followDiagonals);
            addRenderableWidget(followDiagonalsIndicator);
            addRenderableWidget(acrossMaterials);
            addRenderableWidget(acrossMaterialsIndicator);

            if (currentFollowDiagonals)
                followDiagonalsIndicator.state = Indicator.State.ON;
            if (currentAcrossMaterials)
                acrossMaterialsIndicator.state = Indicator.State.ON;
        }

        // Tool buttons
        TerrainTools[] toolValues = brush.getSupportedTools();
        toolButtons = new ArrayList<>(toolValues.length);
        for (int id = 0; id < toolValues.length; id++) {
            TerrainTools tool = toolValues[id];
            IconButton toolButton = new IconButton(x + 7 + id * 18, y + 79, getToolIcon(tool));
            toolButton.withCallback(() -> {
                toolButtons.forEach(b -> b.green = false);
                toolButton.green = true;
                currentTool = tool;
            });
            toolButton.setToolTip(Component.translatable("worldshaper.gui.tool." + tool.translationKey));
            toolButtons.add(toolButton);
        }

        int toolIndex = -1;
        for (int i = 0; i < toolValues.length; i++)
            if (currentTool == toolValues[i]) toolIndex = i;
        if (toolIndex == -1) { currentTool = toolValues[0]; toolIndex = 0; }
        toolButtons.get(toolIndex).green = true;
        for (IconButton btn : toolButtons) addRenderableWidget(btn);

        // Placement options
        if (brush.hasPlacementOptions()) {
            PlacementOptions[] placementValues = PlacementOptions.values();
            placementButtons = new ArrayList<>(placementValues.length);
            for (int id = 0; id < placementValues.length; id++) {
                PlacementOptions option = placementValues[id];
                IconButton placementButton = new IconButton(x + 136 + id * 18, y + 79, getPlacementIcon(option));
                placementButton.withCallback(() -> {
                    placementButtons.forEach(b -> b.green = false);
                    placementButton.green = true;
                    currentPlacement = option;
                });
                placementButton.setToolTip(Component.translatable("worldshaper.gui.placement." + option.translationKey));
                placementButtons.add(placementButton);
            }
            placementButtons.get(currentPlacement.ordinal()).green = true;
            for (IconButton btn : placementButtons) addRenderableWidget(btn);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Don't render the default blur background - we draw our own
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = guiLeft;
        int y = guiTop;

        // Render background texture
        WSGuiTextures.TERRAINZAPPER_BG.render(graphics, x, y);

        // Title
        Component title = zapper.getHoverName();
        graphics.drawString(font, title, x + (BG_WIDTH - font.width(title)) / 2, y + 4, TITLE_COLOR, false);

        // Draw on background
        drawOnBackground(graphics, x, y);

        // Render block preview
        renderBlock(graphics, x, y);

        // Render zapper item
        renderZapper(graphics, x, y);

        // Render widgets
        super.render(graphics, mouseX, mouseY, partialTick);

        // Tooltips
        renderTooltips(graphics, mouseX, mouseY);
    }

    protected void drawOnBackground(GuiGraphics graphics, int x, int y) {
        Brush brush = currentBrush.get();

        // Inactive param slots
        for (int index = 2; index >= brush.amtParams; index--)
            WSGuiTextures.TERRAINZAPPER_INACTIVE_PARAM.render(graphics, x + 56 + 20 * index, y + 40);

        // Section labels
        graphics.drawString(font, toolSection, x + 7, y + 69, FONT_COLOR, false);
        if (brush.hasPlacementOptions())
            graphics.drawString(font, placementSection, x + 136, y + 69, FONT_COLOR, false);

        // Pattern buttons area - no label needed
    }

    protected void renderBlock(GuiGraphics graphics, int x, int y) {
        BlockState state = zapper.getOrDefault(WSDataComponents.SHAPER_BLOCK_USED.get(), Blocks.AIR.defaultBlockState());
        if (state.isAir()) return;

        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(x + 32, y + 42, 120);
        ms.scale(20, 20, 20);

        Minecraft mc = Minecraft.getInstance();
        try {
            ItemStack blockItem = new ItemStack(state.getBlock());
            if (!blockItem.isEmpty()) {
                BakedModel model = mc.getItemRenderer().getModel(blockItem, mc.level, null, 0);
                mc.getItemRenderer().render(blockItem, ItemDisplayContext.GUI, false, ms,
                    graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
                graphics.flush();
            }
        } catch (Exception ignored) {}
        ms.popPose();
    }

    protected void renderZapper(GuiGraphics graphics, int x, int y) {
        PoseStack ms = graphics.pose();
        ms.pushPose();
        Minecraft mc = Minecraft.getInstance();
        try {
            ms.translate(x + BG_WIDTH, y + BG_HEIGHT - 48, -200);
            ms.scale(4, 4, 4);
            BakedModel model = mc.getItemRenderer().getModel(zapper, mc.level, null, 0);
            mc.getItemRenderer().render(zapper, ItemDisplayContext.GUI, false, ms,
                graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            graphics.flush();
        } catch (Exception ignored) {}
        ms.popPose();
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (var widget : children()) {
            if (widget instanceof com.worldshaper.gui.widget.WSAbstractWidget wsWidget) {
                if (wsWidget.isMouseOver(mouseX, mouseY) && !wsWidget.getToolTip().isEmpty()) {
                    graphics.renderTooltip(font, wsWidget.getToolTip(),
                        java.util.Optional.empty(), mouseX, mouseY);
                    return;
                }
            }
        }
    }

    @Override
    public void removed() {
        super.removed();
        int brushParamX = currentBrushParams[0];
        int brushParamY = followDiagonalsIndicator != null ? followDiagonalsIndicator.state == Indicator.State.ON ? 0 : 1
            : currentBrushParams[1];
        int brushParamZ = acrossMaterialsIndicator != null ? acrossMaterialsIndicator.state == Indicator.State.ON ? 0 : 1
            : currentBrushParams[2];

        ConfigureWorldshaperPacket packet = new ConfigureWorldshaperPacket(
            hand, currentPattern, currentBrush, brushParamX, brushParamY, brushParamZ, currentTool, currentPlacement);
        PacketDistributor.sendToServer(packet);
        WorldshaperItem.configureSettings(zapper, currentPattern, currentBrush,
            brushParamX, brushParamY, brushParamZ, currentTool, currentPlacement);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Icon mapping helpers
    private static WSIcons getToolIcon(TerrainTools tool) {
        return switch (tool) {
            case Fill -> WSIcons.I_FILL;
            case Place -> WSIcons.I_PLACE;
            case Replace -> WSIcons.I_REPLACE;
            case Clear -> WSIcons.I_CLEAR;
            case Overlay -> WSIcons.I_OVERLAY;
            case Flatten -> WSIcons.I_FLATTEN;
        };
    }

    private static WSIcons getPlacementIcon(PlacementOptions option) {
        return switch (option) {
            case Merged -> WSIcons.I_CENTERED;
            case Attached -> WSIcons.I_ATTACHED;
            case Inserted -> WSIcons.I_INSERTED;
        };
    }

    private static WSIcons getPatternIcon(PlacementPatterns pattern) {
        return switch (pattern) {
            case Solid -> WSIcons.I_PATTERN_SOLID;
            case Checkered -> WSIcons.I_PATTERN_CHECKERED;
            case InverseCheckered -> WSIcons.I_PATTERN_CHECKERED_INVERSED;
            case Chance25 -> WSIcons.I_PATTERN_CHANCE_25;
            case Chance50 -> WSIcons.I_PATTERN_CHANCE_50;
            case Chance75 -> WSIcons.I_PATTERN_CHANCE_75;
        };
    }
}
