package com.worldshaper.item;

import java.util.Objects;

import com.worldshaper.WSDataComponents;
import com.worldshaper.WSSoundEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber
public class WorldshaperInteractionHandler {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide)
            return;
        ItemStack heldItem = event.getEntity().getMainHandItem();
        if (heldItem.getItem() instanceof WorldshaperItem && trySelect(heldItem, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public static boolean trySelect(ItemStack stack, Player player) {
        if (player.isShiftKeyDown())
            return false;

        Vec3 start = player.position().add(0, player.getEyeHeight(), 0);
        int range = stack.getItem() instanceof WorldshaperItem ? ((WorldshaperItem) stack.getItem()).getZappingRange(stack) : 0;
        Vec3 rangeVec = player.getLookAngle().scale(range);
        BlockHitResult raytrace = player.level().clip(new ClipContext(start, start.add(rangeVec), Block.OUTLINE, Fluid.NONE, player));
        BlockPos pos = raytrace.getBlockPos();
        if (pos == null)
            return false;

        player.level().destroyBlockProgress(player.getId(), pos, -1);
        BlockState newState = player.level().getBlockState(pos);

        // Align with Create: check if block can be obtained as an item (rejects air, barrier, bedrock, etc.)
        if (newState.getBlock().asItem() == net.minecraft.world.item.Items.AIR)
            return false;
        if (newState.hasBlockEntity())
            return false;
        if (newState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))
            return false;
        if (newState.hasProperty(BlockStateProperties.ATTACHED))
            return false;
        if (newState.hasProperty(BlockStateProperties.HANGING))
            return false;
        if (newState.hasProperty(BlockStateProperties.BED_PART))
            return false;
        if (newState.hasProperty(BlockStateProperties.STAIRS_SHAPE))
            newState = newState.setValue(BlockStateProperties.STAIRS_SHAPE, StairsShape.STRAIGHT);
        if (newState.hasProperty(BlockStateProperties.PERSISTENT))
            newState = newState.setValue(BlockStateProperties.PERSISTENT, true);
        if (newState.hasProperty(BlockStateProperties.WATERLOGGED))
            newState = newState.setValue(BlockStateProperties.WATERLOGGED, false);

        CompoundTag data = null;
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity != null) {
            data = blockEntity.saveWithFullMetadata(player.registryAccess());
            data.remove("x");
            data.remove("y");
            data.remove("z");
            data.remove("id");
        }

        if (stack.has(WSDataComponents.SHAPER_BLOCK_USED.get())
            && stack.get(WSDataComponents.SHAPER_BLOCK_USED.get()) == newState
            && Objects.equals(data, stack.get(WSDataComponents.SHAPER_BLOCK_DATA.get()))) {
            return false;
        }

        stack.set(WSDataComponents.SHAPER_BLOCK_USED.get(), newState);
        if (data == null)
            stack.remove(WSDataComponents.SHAPER_BLOCK_DATA.get());
        else
            stack.set(WSDataComponents.SHAPER_BLOCK_DATA.get(), data);

        player.playSound(WSSoundEvents.CONFIRM.get(), 0.5f, 1.0f);
        return true;
    }
}
