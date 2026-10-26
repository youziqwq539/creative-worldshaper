package com.worldshaper.brush;

import java.util.List;

import com.mojang.serialization.Codec;
import com.worldshaper.history.HistoryManager;
import com.worldshaper.history.HistoryManagerRegistry;
import com.worldshaper.net.WSStreamCodecs;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum TerrainTools implements StringRepresentable {
    Fill,
    Place,
    Replace,
    Clear,
    Overlay,
    Flatten;

    public static final Codec<TerrainTools> CODEC = StringRepresentable.fromValues(TerrainTools::values);
    public static final StreamCodec<ByteBuf, TerrainTools> STREAM_CODEC = WSStreamCodecs.ofEnum(TerrainTools.class);
    public final String translationKey;

    TerrainTools() {
        this.translationKey = name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean requiresSelectedBlock() {
        return this != Clear && this != Flatten;
    }

    public void run(Level world, List<BlockPos> targetPositions, Direction facing, @Nullable BlockState paintedState, @Nullable CompoundTag data, Player player) {
        HistoryManager historyManager = HistoryManagerRegistry.getHistoryManager(player);
        historyManager.startOperation();

        switch (this) {
            case Clear:
                targetPositions.forEach(p -> {
                    BlockState oldState = world.getBlockState(p);
                    BlockEntity oldBlockEntity = world.getBlockEntity(p);
                    CompoundTag oldData = oldBlockEntity != null ? oldBlockEntity.saveWithFullMetadata(world.registryAccess()) : null;
                    if (oldData != null) {
                        oldData.remove("x");
                        oldData.remove("y");
                        oldData.remove("z");
                        oldData.remove("id");
                    }
                    world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    historyManager.recordChange(p, oldState, Blocks.AIR.defaultBlockState(), oldData, null);
                });
                break;
            case Fill:
                targetPositions.forEach(p -> {
                    BlockState toReplace = world.getBlockState(p);
                    if (!isReplaceable(toReplace))
                        return;
                    BlockEntity oldBlockEntity = world.getBlockEntity(p);
                    CompoundTag oldData = oldBlockEntity != null ? oldBlockEntity.saveWithFullMetadata(world.registryAccess()) : null;
                    if (oldData != null) {
                        oldData.remove("x");
                        oldData.remove("y");
                        oldData.remove("z");
                        oldData.remove("id");
                    }
                    world.setBlockAndUpdate(p, paintedState);
                    setBlockEntityData(world, p, paintedState, data, player);
                    historyManager.recordChange(p, toReplace, paintedState, oldData, data);
                });
                break;
            case Flatten:
                FlattenTool.apply(world, targetPositions, facing);
                // FlattenTool doesn't support history yet
                break;
            case Overlay:
                targetPositions.forEach(p -> {
                    BlockState toOverlay = world.getBlockState(p);
                    if (isReplaceable(toOverlay))
                        return;
                    if (toOverlay == paintedState)
                        return;
                    BlockPos above = p.above();
                    BlockState toReplace = world.getBlockState(above);
                    if (!isReplaceable(toReplace))
                        return;
                    BlockEntity oldBlockEntity = world.getBlockEntity(above);
                    CompoundTag oldData = oldBlockEntity != null ? oldBlockEntity.saveWithFullMetadata(world.registryAccess()) : null;
                    if (oldData != null) {
                        oldData.remove("x");
                        oldData.remove("y");
                        oldData.remove("z");
                        oldData.remove("id");
                    }
                    world.setBlockAndUpdate(above, paintedState);
                    setBlockEntityData(world, above, paintedState, data, player);
                    historyManager.recordChange(above, toReplace, paintedState, oldData, data);
                });
                break;
            case Place:
                targetPositions.forEach(p -> {
                    BlockState oldState = world.getBlockState(p);
                    BlockEntity oldBlockEntity = world.getBlockEntity(p);
                    CompoundTag oldData = oldBlockEntity != null ? oldBlockEntity.saveWithFullMetadata(world.registryAccess()) : null;
                    if (oldData != null) {
                        oldData.remove("x");
                        oldData.remove("y");
                        oldData.remove("z");
                        oldData.remove("id");
                    }
                    world.setBlockAndUpdate(p, paintedState);
                    setBlockEntityData(world, p, paintedState, data, player);
                    historyManager.recordChange(p, oldState, paintedState, oldData, data);
                });
                break;
            case Replace:
                targetPositions.forEach(p -> {
                    BlockState toReplace = world.getBlockState(p);
                    if (isReplaceable(toReplace))
                        return;
                    BlockEntity oldBlockEntity = world.getBlockEntity(p);
                    CompoundTag oldData = oldBlockEntity != null ? oldBlockEntity.saveWithFullMetadata(world.registryAccess()) : null;
                    if (oldData != null) {
                        oldData.remove("x");
                        oldData.remove("y");
                        oldData.remove("z");
                        oldData.remove("id");
                    }
                    world.setBlockAndUpdate(p, paintedState);
                    setBlockEntityData(world, p, paintedState, data, player);
                    historyManager.recordChange(p, toReplace, paintedState, oldData, data);
                });
                break;
        }

        historyManager.finishOperation();
    }

    public static boolean isReplaceable(BlockState toReplace) {
        return toReplace.canBeReplaced();
    }

    public static void setBlockEntityData(Level world, BlockPos pos, BlockState state, CompoundTag data, Player player) {
        if (data != null) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity != null) {
                data.putInt("x", pos.getX());
                data.putInt("y", pos.getY());
                data.putInt("z", pos.getZ());
                blockEntity.loadWithComponents(data, world.registryAccess());
            }
        }
    }
}
