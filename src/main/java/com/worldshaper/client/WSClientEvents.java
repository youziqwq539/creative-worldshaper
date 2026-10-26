package com.worldshaper.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.worldshaper.item.WorldshaperItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(Dist.CLIENT)
public class WSClientEvents {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            WorldshaperRenderHandler.INSTANCE.tick();
            WorldshaperRenderHandler.updateTickCounter(
                WorldshaperRenderHandler.getTickCounter() + 1,
                mc.getTimer().getGameTimeDeltaPartialTick(true));
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack heldItem = event.getItemStack();
        if (!WorldshaperItem.isWorldshaper(heldItem))
            return;

        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null) return;

        PlayerRenderer playerrenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        ItemInHandRenderer firstPersonRenderer = mc.getEntityRenderDispatcher().getItemInHandRenderer();

        PoseStack ms = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        int light = event.getPackedLight();
        float pt = event.getPartialTick();

        boolean rightHand = event.getHand() == InteractionHand.MAIN_HAND ^ mc.player.getMainArm() == HumanoidArm.LEFT;
        float recoil = rightHand
            ? Mth.lerp(pt, WorldshaperRenderHandler.INSTANCE.lastRightHandAnimation, WorldshaperRenderHandler.INSTANCE.rightHandAnimation)
            : Mth.lerp(pt, WorldshaperRenderHandler.INSTANCE.lastLeftHandAnimation, WorldshaperRenderHandler.INSTANCE.leftHandAnimation);
        float equipProgress = event.getEquipProgress();

        if (rightHand && (WorldshaperRenderHandler.INSTANCE.rightHandAnimation > .01f || WorldshaperRenderHandler.INSTANCE.dontReequipRight))
            equipProgress = 0;
        if (!rightHand && (WorldshaperRenderHandler.INSTANCE.leftHandAnimation > .01f || WorldshaperRenderHandler.INSTANCE.dontReequipLeft))
            equipProgress = 0;

        // Render arm
        ms.pushPose();
        RenderSystem.setShaderTexture(0, player.getSkin().texture());

        float flip = rightHand ? 1.0F : -1.0F;
        float f1 = Mth.sqrt(event.getSwingProgress());
        float f2 = -0.3F * Mth.sin(f1 * (float) Math.PI);
        float f3 = 0.4F * Mth.sin(f1 * ((float) Math.PI * 2F));
        float f4 = -0.4F * Mth.sin(event.getSwingProgress() * (float) Math.PI);
        float f5 = Mth.sin(event.getSwingProgress() * event.getSwingProgress() * (float) Math.PI);
        float f6 = Mth.sin(f1 * (float) Math.PI);

        ms.translate(flip * (f2 + 0.64F - .1f), f3 + -0.4F + equipProgress * -0.6F, f4 + -0.72F + .3f + recoil);
        ms.mulPose(Axis.YP.rotationDegrees(flip * 75.0F));
        ms.mulPose(Axis.YP.rotationDegrees(flip * f6 * 70.0F));
        ms.mulPose(Axis.ZP.rotationDegrees(flip * f5 * -20.0F));
        ms.translate(flip * -1.0F, 3.6F, 3.5F);
        ms.mulPose(Axis.ZP.rotationDegrees(flip * 120.0F));
        ms.mulPose(Axis.XP.rotationDegrees(200.0F));
        ms.mulPose(Axis.YP.rotationDegrees(flip * -135.0F));
        ms.translate(flip * 5.6F, 0.0F, 0.0F);
        ms.mulPose(Axis.YP.rotationDegrees(flip * 40.0F));
        if (rightHand)
            playerrenderer.renderRightHand(ms, buffer, light, player);
        else
            playerrenderer.renderLeftHand(ms, buffer, light, player);
        ms.popPose();

        // Render gadget
        ms.pushPose();
        ms.translate(flip * (f2 + 0.64F - .1f), f3 + -0.4F + equipProgress * -0.6F, f4 + -0.72F - 0.1f + recoil);
        ms.mulPose(Axis.YP.rotationDegrees(flip * f6 * 70.0F));
        ms.mulPose(Axis.ZP.rotationDegrees(flip * f5 * -20.0F));
        // Transform tool position
        ms.translate(flip * -0.1f, 0.1f, -0.4f);
        ms.mulPose(Axis.YP.rotationDegrees(flip * 5.0F));

        firstPersonRenderer.renderItem(mc.player, heldItem,
            rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            !rightHand, ms, buffer, light);
        ms.popPose();

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            WorldshaperRenderHandler.INSTANCE.renderLevel(event.getPoseStack(), event.getPartialTick().getGameTimeDeltaPartialTick(true));
        }
    }
}
