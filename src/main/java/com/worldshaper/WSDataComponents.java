package com.worldshaper;

import com.worldshaper.brush.PlacementOptions;
import com.worldshaper.brush.PlacementPatterns;
import com.worldshaper.brush.TerrainBrushes;
import com.worldshaper.brush.TerrainTools;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class WSDataComponents {

    public static final Supplier<DataComponentType<PlacementPatterns>> PLACEMENT_PATTERN =
        WorldshaperMod.DATA_COMPONENTS.register("placement_pattern",
            () -> DataComponentType.<PlacementPatterns>builder()
                .persistent(PlacementPatterns.CODEC)
                .networkSynchronized(PlacementPatterns.STREAM_CODEC)
                .build());

    public static final Supplier<DataComponentType<TerrainBrushes>> SHAPER_BRUSH =
        WorldshaperMod.DATA_COMPONENTS.register("shaper_brush",
            () -> DataComponentType.<TerrainBrushes>builder()
                .persistent(TerrainBrushes.CODEC)
                .networkSynchronized(TerrainBrushes.STREAM_CODEC)
                .build());

    public static final Supplier<DataComponentType<BlockPos>> SHAPER_BRUSH_PARAMS =
        WorldshaperMod.DATA_COMPONENTS.register("shaper_brush_params",
            () -> DataComponentType.<BlockPos>builder()
                .persistent(BlockPos.CODEC)
                .networkSynchronized(BlockPos.STREAM_CODEC)
                .build());

    public static final Supplier<DataComponentType<PlacementOptions>> SHAPER_PLACEMENT_OPTIONS =
        WorldshaperMod.DATA_COMPONENTS.register("shaper_placement_options",
            () -> DataComponentType.<PlacementOptions>builder()
                .persistent(PlacementOptions.CODEC)
                .networkSynchronized(PlacementOptions.STREAM_CODEC)
                .build());

    public static final Supplier<DataComponentType<TerrainTools>> SHAPER_TOOL =
        WorldshaperMod.DATA_COMPONENTS.register("shaper_tool",
            () -> DataComponentType.<TerrainTools>builder()
                .persistent(TerrainTools.CODEC)
                .networkSynchronized(TerrainTools.STREAM_CODEC)
                .build());

    public static final Supplier<DataComponentType<BlockState>> SHAPER_BLOCK_USED =
        WorldshaperMod.DATA_COMPONENTS.register("shaper_block_used",
            () -> DataComponentType.<BlockState>builder()
                .persistent(BlockState.CODEC)
                .networkSynchronized(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY))
                .build());

    public static final Supplier<DataComponentType<Boolean>> SHAPER_SWAP =
        WorldshaperMod.DATA_COMPONENTS.register("shaper_swap",
            () -> DataComponentType.<Boolean>builder()
                .persistent(Codec.BOOL)
                .networkSynchronized(ByteBufCodecs.BOOL)
                .build());

    public static final Supplier<DataComponentType<CompoundTag>> SHAPER_BLOCK_DATA =
        WorldshaperMod.DATA_COMPONENTS.register("shaper_block_data",
            () -> DataComponentType.<CompoundTag>builder()
                .persistent(CompoundTag.CODEC)
                .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                .build());

    public static void register() {
        // Triggers static initialization
    }
}
