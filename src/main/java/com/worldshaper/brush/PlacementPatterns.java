package com.worldshaper.brush;

import java.util.List;
import java.util.function.Predicate;

import com.google.common.base.Predicates;
import com.mojang.serialization.Codec;
import com.worldshaper.WSDataComponents;
import com.worldshaper.net.WSStreamCodecs;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

public enum PlacementPatterns implements StringRepresentable {
    Solid,
    Checkered,
    InverseCheckered,
    Chance25,
    Chance50,
    Chance75;

    public static final Codec<PlacementPatterns> CODEC = StringRepresentable.fromValues(PlacementPatterns::values);
    public static final StreamCodec<ByteBuf, PlacementPatterns> STREAM_CODEC = WSStreamCodecs.ofEnum(PlacementPatterns.class);

    public final String translationKey;

    private PlacementPatterns() {
        this.translationKey = name().toLowerCase(java.util.Locale.ROOT);
    }

    public static void applyPattern(List<BlockPos> blocksIn, ItemStack stack, RandomSource random) {
        PlacementPatterns pattern = stack.getOrDefault(WSDataComponents.PLACEMENT_PATTERN.get(), Solid);
        Predicate<BlockPos> filter = Predicates.alwaysFalse();

        switch (pattern) {
            case Chance25:
                filter = pos -> random.nextBoolean() || random.nextBoolean();
                break;
            case Chance50:
                filter = pos -> random.nextBoolean();
                break;
            case Chance75:
                filter = pos -> random.nextBoolean() && random.nextBoolean();
                break;
            case Checkered:
                filter = pos -> (pos.getX() + pos.getY() + pos.getZ()) % 2 == 0;
                break;
            case InverseCheckered:
                filter = pos -> (pos.getX() + pos.getY() + pos.getZ()) % 2 != 0;
                break;
            case Solid:
            default:
                break;
        }

        blocksIn.removeIf(filter);
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
