package com.worldshaper.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.worldshaper.WSDataComponents;
import com.worldshaper.WSSoundEvents;
import com.worldshaper.WorldshaperMod;
import com.worldshaper.brush.Brush;
import com.worldshaper.brush.PlacementOptions;
import com.worldshaper.brush.PlacementPatterns;
import com.worldshaper.brush.TerrainBrushes;
import com.worldshaper.brush.TerrainTools;
import com.worldshaper.client.WorldshaperRenderHandler;
import com.worldshaper.history.HistoryManager;
import com.worldshaper.history.HistoryManagerRegistry;
import com.worldshaper.net.WorldshaperBeamPacket;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredItem;

public class WorldshaperItem extends Item {

    public static final DeferredItem<WorldshaperItem> WORLDSHAPER =
        WorldshaperMod.ITEMS.registerItem("handheld_worldshaper",
            props -> new WorldshaperItem(props.stacksTo(1)));

    public WorldshaperItem(Properties properties) {
        super(properties);
    }

    public static void register() {
        // Triggers static initialization
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        if (stack.has(WSDataComponents.SHAPER_BLOCK_USED.get())) {
            MutableComponent usedBlock = stack.get(WSDataComponents.SHAPER_BLOCK_USED.get()).getBlock().getName();
            tooltip.add(Component.translatable("worldshaper.usingBlock", usedBlock.withStyle(ChatFormatting.GRAY))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        boolean differentBlock = false;
        if (oldStack.has(WSDataComponents.SHAPER_BLOCK_USED.get()) && newStack.has(WSDataComponents.SHAPER_BLOCK_USED.get()))
            differentBlock = oldStack.get(WSDataComponents.SHAPER_BLOCK_USED.get()) != newStack.get(WSDataComponents.SHAPER_BLOCK_USED.get());
        return slotChanged || !isWorldshaper(newStack) || differentBlock;
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            if (context.getLevel().isClientSide) {
                openGui(context.getItemInHand(), context.getHand());
                context.getPlayer().getCooldowns().addCooldown(context.getItemInHand().getItem(), 10);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);
        boolean mainHand = hand == InteractionHand.MAIN_HAND;

        // Shift -> Open GUI
        if (player.isShiftKeyDown()) {
            if (world.isClientSide) {
                openGui(item, hand);
                player.getCooldowns().addCooldown(item.getItem(), 10);
            }
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, item);
        }

        // Check swap
        if (shouldSwap(player, item, hand))
            return new InteractionResultHolder<>(InteractionResult.FAIL, item);

        // Check if can be used
        Component msg = validateUsage(item);
        if (msg != null) {
            world.playSound(null, player.blockPosition(), WSSoundEvents.DENY.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
            player.displayClientMessage(msg.plainCopy().withStyle(ChatFormatting.RED), true);
            return new InteractionResultHolder<>(InteractionResult.FAIL, item);
        }

        BlockState stateToUse = Blocks.AIR.defaultBlockState();
        if (item.has(WSDataComponents.SHAPER_BLOCK_USED.get()))
            stateToUse = item.get(WSDataComponents.SHAPER_BLOCK_USED.get());
        stateToUse = setZeroAge(stateToUse);

        CompoundTag data = null;
        if (item.has(WSDataComponents.SHAPER_BLOCK_DATA.get()) && stateToUse.hasBlockEntity()) {
            data = item.get(WSDataComponents.SHAPER_BLOCK_DATA.get());
        }

        // Raytrace
        Vec3 start = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 range = player.getLookAngle().scale(getZappingRange(item));
        BlockHitResult raytrace = world.clip(new ClipContext(start, start.add(range), Block.OUTLINE, Fluid.NONE, player));
        BlockPos pos = raytrace.getBlockPos();
        BlockState stateReplaced = world.getBlockState(pos);

        // No target
        if (stateReplaced.getBlock() == Blocks.AIR) {
            applyCooldown(player, item, hand, getCooldownDelay(item));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, item);
        }

        // Find barrel pos for VFX
        boolean rightHand = mainHand == (player.getMainArm() == HumanoidArm.RIGHT);
        Vec3 barrelPos = getGunBarrelVec(player, mainHand, new Vec3(.35f, -0.1f, 1));

        // Client side
        if (world.isClientSide) {
            WorldshaperRenderHandler.INSTANCE.dontAnimateItem(hand);
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, item);
        }

        // Server side
        if (activate(world, player, item, stateToUse, raytrace, data)) {
            applyCooldown(player, item, hand, getCooldownDelay(item));
            // Send beam packet to all tracking players
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer,
                    new WorldshaperBeamPacket(barrelPos, hand, false, raytrace.getLocation()));
                PacketDistributor.sendToPlayer(serverPlayer,
                    new WorldshaperBeamPacket(barrelPos, hand, true, raytrace.getLocation()));
            }
        }

        return new InteractionResultHolder<>(InteractionResult.SUCCESS, item);
    }

    public Component validateUsage(ItemStack item) {
        if (!item.has(WSDataComponents.SHAPER_BRUSH_PARAMS.get()))
            return Component.translatable("worldshaper.shiftRightClickToSet");
        if (!canActivateWithoutSelectedBlock(item) && !item.has(WSDataComponents.SHAPER_BLOCK_USED.get()))
            return Component.translatable("worldshaper.leftClickToSet");
        return null;
    }

    protected int getZappingRange(ItemStack stack) {
        return 128;
    }

    protected int getCooldownDelay(ItemStack item) {
        return 2;
    }

    protected boolean canActivateWithoutSelectedBlock(ItemStack stack) {
        TerrainTools tool = stack.getOrDefault(WSDataComponents.SHAPER_TOOL.get(), TerrainTools.Fill);
        return !tool.requiresSelectedBlock();
    }

    protected boolean activate(Level level, Player player, ItemStack stack, BlockState stateToUse,
        BlockHitResult raytrace, CompoundTag data) {

        BlockPos targetPos = raytrace.getBlockPos();
        List<BlockPos> affectedPositions = new ArrayList<>();

        Brush brush = stack.getOrDefault(WSDataComponents.SHAPER_BRUSH.get(), TerrainBrushes.Cuboid).get();
        BlockPos params = stack.get(WSDataComponents.SHAPER_BRUSH_PARAMS.get());
        PlacementOptions option = stack.getOrDefault(WSDataComponents.SHAPER_PLACEMENT_OPTIONS.get(), PlacementOptions.Merged);
        TerrainTools tool = stack.getOrDefault(WSDataComponents.SHAPER_TOOL.get(), TerrainTools.Fill);

        brush.set(params.getX(), params.getY(), params.getZ());
        targetPos = targetPos.offset(brush.getOffset(player.getLookAngle(), raytrace.getDirection(), option));
        brush.addToGlobalPositions(level, targetPos, raytrace.getDirection(), affectedPositions, tool);
        PlacementPatterns.applyPattern(affectedPositions, stack, level.random);
        brush.redirectTool(tool)
            .run(level, affectedPositions, raytrace.getDirection(), stateToUse, data, player);

        return true;
    }

    @OnlyIn(Dist.CLIENT)
    protected void openGui(ItemStack item, InteractionHand hand) {
        net.minecraft.client.Minecraft.getInstance().setScreen(
            new com.worldshaper.gui.WorldshaperScreen(item, hand));
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level worldIn, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Nullable
    public ArmPose getArmPose(ItemStack stack, AbstractClientPlayer player, InteractionHand hand) {
        if (!player.swinging) {
            return ArmPose.CROSSBOW_HOLD;
        }
        return null;
    }

    public static void configureSettings(ItemStack stack, PlacementPatterns pattern, TerrainBrushes brush,
        int brushParamX, int brushParamY, int brushParamZ, TerrainTools tool, PlacementOptions placement) {
        stack.set(WSDataComponents.PLACEMENT_PATTERN.get(), pattern);
        stack.set(WSDataComponents.SHAPER_BRUSH.get(), brush);
        stack.set(WSDataComponents.SHAPER_BRUSH_PARAMS.get(), new BlockPos(brushParamX, brushParamY, brushParamZ));
        stack.set(WSDataComponents.SHAPER_TOOL.get(), tool);
        stack.set(WSDataComponents.SHAPER_PLACEMENT_OPTIONS.get(), placement);
    }

    /**
     * Performs undo or redo operation.
     */
    public static void performUndoRedo(ItemStack stack, Player player, boolean isUndo) {
        if (player.level().isClientSide) {
            return;
        }

        HistoryManager historyManager = HistoryManagerRegistry.getHistoryManager(player);
        boolean success = isUndo ? historyManager.undo(player.level(), player) : historyManager.redo(player.level(), player);

        if (success) {
            player.playSound(WSSoundEvents.CONFIRM.get(), 0.5f, 1.0f);
        } else {
            player.playSound(WSSoundEvents.DENY.get(), 0.5f, 1.0f);
        }
    }

    // --- Helper methods (inlined from Create's utilities) ---

    public static boolean isWorldshaper(ItemStack stack) {
        return stack.getItem() instanceof WorldshaperItem;
    }

    private boolean shouldSwap(Player player, ItemStack item, InteractionHand hand) {
        boolean isSwap = item.has(WSDataComponents.SHAPER_SWAP.get());
        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        InteractionHand otherHand = mainHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean shaperInOtherHand = isWorldshaper(player.getItemInHand(otherHand));

        if (mainHand && isSwap && shaperInOtherHand)
            return true;
        if (mainHand && !isSwap && shaperInOtherHand)
            item.set(WSDataComponents.SHAPER_SWAP.get(), true);
        if (!mainHand && isSwap)
            item.remove(WSDataComponents.SHAPER_SWAP.get());
        if (!mainHand && shaperInOtherHand)
            player.getItemInHand(InteractionHand.MAIN_HAND).remove(WSDataComponents.SHAPER_SWAP.get());
        player.startUsingItem(hand);
        return false;
    }

    private void applyCooldown(Player player, ItemStack item, InteractionHand hand, int cooldown) {
        if (cooldown <= 0)
            return;
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        boolean shaperInOtherHand = isWorldshaper(player.getItemInHand(otherHand));
        player.getCooldowns().addCooldown(item.getItem(), shaperInOtherHand ? cooldown * 2 / 3 : cooldown);
    }

    public static Vec3 getGunBarrelVec(Player player, boolean mainHand, Vec3 rightHandForward) {
        Vec3 start = player.position().add(0, player.getEyeHeight(), 0);
        float yaw = (float) ((player.getYRot()) / -180 * Math.PI);
        float pitch = (float) ((player.getXRot()) / -180 * Math.PI);
        int flip = mainHand == (player.getMainArm() == HumanoidArm.RIGHT) ? -1 : 1;
        Vec3 barrelPosNoTransform = new Vec3(flip * rightHandForward.x, rightHandForward.y, rightHandForward.z);
        return start.add(barrelPosNoTransform.xRot(pitch).yRot(yaw));
    }

    public static BlockState setZeroAge(BlockState blockState) {
        if (blockState.hasProperty(BlockStateProperties.AGE_1))
            return blockState.setValue(BlockStateProperties.AGE_1, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_2))
            return blockState.setValue(BlockStateProperties.AGE_2, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_3))
            return blockState.setValue(BlockStateProperties.AGE_3, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_5))
            return blockState.setValue(BlockStateProperties.AGE_5, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_7))
            return blockState.setValue(BlockStateProperties.AGE_7, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_15))
            return blockState.setValue(BlockStateProperties.AGE_15, 0);
        if (blockState.hasProperty(BlockStateProperties.AGE_25))
            return blockState.setValue(BlockStateProperties.AGE_25, 0);
        if (blockState.hasProperty(BlockStateProperties.LEVEL_HONEY))
            return blockState.setValue(BlockStateProperties.LEVEL_HONEY, 0);
        if (blockState.hasProperty(BlockStateProperties.HATCH))
            return blockState.setValue(BlockStateProperties.HATCH, 0);
        if (blockState.hasProperty(BlockStateProperties.STAGE))
            return blockState.setValue(BlockStateProperties.STAGE, 0);
        if (blockState.hasProperty(BlockStateProperties.LEVEL_COMPOSTER))
            return blockState.setValue(BlockStateProperties.LEVEL_COMPOSTER, 0);
        if (blockState.hasProperty(BlockStateProperties.EXTENDED))
            return blockState.setValue(BlockStateProperties.EXTENDED, false);
        return blockState;
    }
}
