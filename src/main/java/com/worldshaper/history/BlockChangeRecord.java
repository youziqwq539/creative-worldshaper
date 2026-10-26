package com.worldshaper.history;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Records a single block change for undo/redo functionality.
 */
public class BlockChangeRecord {
    public final BlockPos pos;
    public final BlockState oldState;
    public final BlockState newState;
    public final CompoundTag oldData;
    public final CompoundTag newData;

    public BlockChangeRecord(BlockPos pos, BlockState oldState, BlockState newState, CompoundTag oldData, CompoundTag newData) {
        this.pos = pos;
        this.oldState = oldState;
        this.newState = newState;
        this.oldData = oldData;
        this.newData = newData;
    }
}