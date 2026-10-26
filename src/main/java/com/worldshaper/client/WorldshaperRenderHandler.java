package com.worldshaper.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import com.worldshaper.WSDataComponents;
import com.worldshaper.WSSoundEvents;
import com.worldshaper.brush.Brush;
import com.worldshaper.brush.PlacementOptions;
import com.worldshaper.brush.TerrainBrushes;
import com.worldshaper.brush.TerrainTools;
import com.worldshaper.item.WorldshaperItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class WorldshaperRenderHandler {

    public static final WorldshaperRenderHandler INSTANCE = new WorldshaperRenderHandler();

    // Animation state (from ShootableGadgetRenderHandler)
    protected float leftHandAnimation;
    protected float rightHandAnimation;
    protected float lastLeftHandAnimation;
    protected float lastRightHandAnimation;
    protected boolean dontReequipLeft;
    protected boolean dontReequipRight;

    // Laser beams
    public List<LaserBeam> cachedBeams = new LinkedList<>();

    // Tick counter for item renderer animation
    private static int tickCounter = 0;
    private static float partialTicks = 0;

    public void tick() {
        // Animation decay
        lastLeftHandAnimation = leftHandAnimation;
        lastRightHandAnimation = rightHandAnimation;
        leftHandAnimation *= 0.8f;
        rightHandAnimation *= 0.8f;

        // Beams
        if (cachedBeams == null)
            cachedBeams = new LinkedList<>();
        cachedBeams.removeIf(b -> b.intensity < .1f);

        // Brush outline
        gatherSelectedBlocks();
    }

    public float getAnimation(boolean rightHand, float partialTicks) {
        return Mth.lerp(partialTicks, rightHand ? lastRightHandAnimation : lastLeftHandAnimation,
            rightHand ? rightHandAnimation : leftHandAnimation);
    }

    public void shoot(InteractionHand hand, Vec3 location) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean rightHand = hand == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
        if (rightHand) {
            rightHandAnimation = .2f;
            dontReequipRight = false;
        } else {
            leftHandAnimation = .2f;
            dontReequipLeft = false;
        }
        playSound(hand, location);
    }

    public void playSound(InteractionHand hand, Vec3 position) {
        float pitch = hand == InteractionHand.MAIN_HAND ? 0.1f : 0.9f;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null)
            mc.level.playLocalSound(position.x, position.y, position.z, WSSoundEvents.WORLDSHAPER_PLACE.get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.1f, pitch, false);
    }

    public void addBeam(LaserBeam beam) {
        RandomSource random = Minecraft.getInstance().level.random;
        double x = beam.end.x;
        double y = beam.end.y;
        double z = beam.end.z;
        ClientLevel world = Minecraft.getInstance().level;
        Supplier<Double> randomSpeed = () -> (random.nextDouble() - .5d) * .2f;
        Supplier<Double> randomOffset = () -> (random.nextDouble() - .5d) * .2f;
        for (int i = 0; i < 10; i++) {
            world.addParticle(ParticleTypes.END_ROD, x, y, z, randomSpeed.get(), randomSpeed.get(), randomSpeed.get());
            world.addParticle(ParticleTypes.FIREWORK, x + randomOffset.get(), y + randomOffset.get(),
                z + randomOffset.get(), 0, 0, 0);
        }
        cachedBeams.add(beam);
    }

    public void dontAnimateItem(InteractionHand hand) {
        LocalPlayer player = Minecraft.getInstance().player;
        boolean rightHand = hand == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
        dontReequipRight |= rightHand;
        dontReequipLeft |= !rightHand;
    }

    // --- Brush outline rendering (replaces Outliner) ---

    private Collection<BlockPos> currentBrushPositions;

    protected void gatherSelectedBlocks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        LocalPlayer player = mc.player;
        ItemStack heldMain = player.getMainHandItem();
        ItemStack heldOff = player.getOffhandItem();
        boolean zapperInMain = WorldshaperItem.isWorldshaper(heldMain);
        boolean zapperInOff = WorldshaperItem.isWorldshaper(heldOff);

        if (zapperInMain) {
            if (!heldMain.has(WSDataComponents.SHAPER_SWAP.get()) || !zapperInOff) {
                createBrushOutline(player, heldMain);
                return;
            }
        }

        if (zapperInOff) {
            createBrushOutline(player, heldOff);
            return;
        }

        currentBrushPositions = null;
    }

    public void createBrushOutline(LocalPlayer player, ItemStack zapper) {
        if (!zapper.has(WSDataComponents.SHAPER_BRUSH_PARAMS.get())) {
            currentBrushPositions = null;
            return;
        }

        Brush brush = zapper.getOrDefault(WSDataComponents.SHAPER_BRUSH.get(), TerrainBrushes.Cuboid).get();
        PlacementOptions placement = zapper.getOrDefault(WSDataComponents.SHAPER_PLACEMENT_OPTIONS.get(), PlacementOptions.Merged);
        TerrainTools tool = zapper.getOrDefault(WSDataComponents.SHAPER_TOOL.get(), TerrainTools.Fill);
        BlockPos params = zapper.get(WSDataComponents.SHAPER_BRUSH_PARAMS.get());
        brush.set(params.getX(), params.getY(), params.getZ());

        Vec3 start = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 range = player.getLookAngle().scale(128);
        BlockHitResult raytrace = player.level().clip(new ClipContext(start, start.add(range), Block.OUTLINE, Fluid.NONE, player));
        if (raytrace == null || raytrace.getType() == Type.MISS) {
            currentBrushPositions = null;
            return;
        }

        BlockPos pos = raytrace.getBlockPos().offset(brush.getOffset(player.getLookAngle(), raytrace.getDirection(), placement));
        currentBrushPositions = brush.addToGlobalPositions(player.level(), pos, raytrace.getDirection(), new ArrayList<>(), tool);
    }

    /**
     * Renders the brush outline and beams. Called from RenderLevelStageEvent.
     */
    public void renderLevel(PoseStack poseStack, float partialTick) {
        // Render beams
        renderBeams(poseStack, partialTick);
    }

    private void renderBeams(PoseStack poseStack, float partialTick) {
        if (cachedBeams.isEmpty())
            return;

        for (LaserBeam beam : cachedBeams) {
            // Simple line rendering using debug rendering would go here
            // For simplicity, beams decay is handled in tick()
            beam.intensity *= .6f;
        }
    }

    public static void updateTickCounter(int tick, float partial) {
        tickCounter = tick;
        partialTicks = partial;
    }

    public static int getTickCounter() { return tickCounter; }
    public static float getPartialTicks() { return partialTicks; }

    public static class LaserBeam {
        public float intensity;
        public final Vec3 start;
        public final Vec3 end;

        public LaserBeam(Vec3 start, Vec3 end) {
            this.start = start;
            this.end = end;
            intensity = 1;
        }
    }
}
