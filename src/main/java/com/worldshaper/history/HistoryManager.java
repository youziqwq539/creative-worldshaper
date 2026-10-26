package com.worldshaper.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Manages undo/redo history for worldshaper operations.
 */
public class HistoryManager {
    private static final int MAX_HISTORY_SIZE = 50;

    private final Stack<List<BlockChangeRecord>> undoStack = new Stack<>();
    private final Stack<List<BlockChangeRecord>> redoStack = new Stack<>();
    private List<BlockChangeRecord> currentOperation = new ArrayList<>();

    /**
     * Starts recording a new operation.
     */
    public void startOperation() {
        currentOperation = new ArrayList<>();
    }

    /**
     * Records a block change in the current operation.
     */
    public void recordChange(BlockPos pos, BlockState oldState, BlockState newState, CompoundTag oldData, CompoundTag newData) {
        currentOperation.add(new BlockChangeRecord(pos, oldState, newState, oldData, newData));
    }

    /**
     * Finishes the current operation and adds it to the undo stack.
     */
    public void finishOperation() {
        if (!currentOperation.isEmpty()) {
            undoStack.push(currentOperation);
            redoStack.clear(); // Clear redo stack when new operation is recorded

            // Limit history size
            if (undoStack.size() > MAX_HISTORY_SIZE) {
                undoStack.remove(0);
            }

            currentOperation = new ArrayList<>();
        }
    }

    /**
     * Undoes the last operation.
     */
    public boolean undo(Level world, Player player) {
        if (undoStack.isEmpty()) {
            return false;
        }

        List<BlockChangeRecord> operation = undoStack.pop();
        List<BlockChangeRecord> redoOperation = new ArrayList<>();

        for (BlockChangeRecord record : operation) {
            // Get current state before undo (for redo)
            BlockState currentState = world.getBlockState(record.pos);
            BlockEntity currentBlockEntity = world.getBlockEntity(record.pos);
            CompoundTag currentData = currentBlockEntity != null ? currentBlockEntity.saveWithFullMetadata(world.registryAccess()) : null;
            if (currentData != null) {
                currentData.remove("x");
                currentData.remove("y");
                currentData.remove("z");
                currentData.remove("id");
            }

            // Restore old state
            world.setBlockAndUpdate(record.pos, record.oldState);

            // Restore block entity data if present
            if (record.oldData != null) {
                BlockEntity blockEntity = world.getBlockEntity(record.pos);
                if (blockEntity != null) {
                    record.oldData.putInt("x", record.pos.getX());
                    record.oldData.putInt("y", record.pos.getY());
                    record.oldData.putInt("z", record.pos.getZ());
                    blockEntity.loadWithComponents(record.oldData, world.registryAccess());
                }
            }

            // Record for redo (old state -> current state, so redo can restore B)
            redoOperation.add(new BlockChangeRecord(record.pos, record.oldState, currentState, record.oldData, currentData));
        }

        redoStack.push(redoOperation);
        return true;
    }

    /**
     * Redoes the last undone operation.
     */
    public boolean redo(Level world, Player player) {
        if (redoStack.isEmpty()) {
            return false;
        }

        List<BlockChangeRecord> operation = redoStack.pop();
        List<BlockChangeRecord> undoOperation = new ArrayList<>();

        for (BlockChangeRecord record : operation) {
            // Get current state before redo (for undo)
            BlockState currentState = world.getBlockState(record.pos);
            BlockEntity currentBlockEntity = world.getBlockEntity(record.pos);
            CompoundTag currentData = currentBlockEntity != null ? currentBlockEntity.saveWithFullMetadata(world.registryAccess()) : null;
            if (currentData != null) {
                currentData.remove("x");
                currentData.remove("y");
                currentData.remove("z");
                currentData.remove("id");
            }

            // Restore new state
            world.setBlockAndUpdate(record.pos, record.newState);

            // Restore block entity data if present
            if (record.newData != null) {
                BlockEntity blockEntity = world.getBlockEntity(record.pos);
                if (blockEntity != null) {
                    record.newData.putInt("x", record.pos.getX());
                    record.newData.putInt("y", record.pos.getY());
                    record.newData.putInt("z", record.pos.getZ());
                    blockEntity.loadWithComponents(record.newData, world.registryAccess());
                }
            }

            // Record for undo (new state -> current state, so undo can restore A)
            undoOperation.add(new BlockChangeRecord(record.pos, record.newState, currentState, record.newData, currentData));
        }

        undoStack.push(undoOperation);
        return true;
    }

    /**
     * Checks if undo is available.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Checks if redo is available.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Clears all history.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        currentOperation.clear();
    }
}