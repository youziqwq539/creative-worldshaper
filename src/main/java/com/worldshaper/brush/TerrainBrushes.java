package com.worldshaper.brush;

import com.mojang.serialization.Codec;
import com.worldshaper.net.WSStreamCodecs;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import org.jetbrains.annotations.NotNull;

public enum TerrainBrushes implements StringRepresentable {
    Cuboid(new CuboidBrush()),
    Sphere(new SphereBrush()),
    Cylinder(new CylinderBrush()),
    Surface(new DynamicBrush(true)),
    Cluster(new DynamicBrush(false));

    public static final Codec<TerrainBrushes> CODEC = StringRepresentable.fromValues(TerrainBrushes::values);
    public static final StreamCodec<ByteBuf, TerrainBrushes> STREAM_CODEC = WSStreamCodecs.ofEnum(TerrainBrushes.class);

    private final Brush brush;

    TerrainBrushes(Brush brush) {
        this.brush = brush;
    }

    public Brush get() {
        return brush;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
