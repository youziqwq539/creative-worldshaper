package com.worldshaper.brush;

import com.mojang.serialization.Codec;
import com.worldshaper.net.WSStreamCodecs;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.NotNull;

public enum PlacementOptions implements StringRepresentable {
    Merged,
    Attached,
    Inserted;

    public static final Codec<PlacementOptions> CODEC = StringRepresentable.fromValues(PlacementOptions::values);
    public static final StreamCodec<ByteBuf, PlacementOptions> STREAM_CODEC = WSStreamCodecs.ofEnum(PlacementOptions.class);

    public final String translationKey;

    PlacementOptions() {
        this.translationKey = name().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
