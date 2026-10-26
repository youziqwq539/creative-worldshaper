package com.worldshaper.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

public class WSStreamCodecs {

    public static final StreamCodec<ByteBuf, InteractionHand> HAND = StreamCodec.ofMember(
        (hand, buf) -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND),
        buf -> buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND
    );

    public static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.ofMember(
        (vec, buf) -> {
            buf.writeDouble(vec.x);
            buf.writeDouble(vec.y);
            buf.writeDouble(vec.z);
        },
        buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
    );

    public static <E extends Enum<E> & StringRepresentable> StreamCodec<ByteBuf, E> ofEnum(Class<E> clazz) {
        E[] values = clazz.getEnumConstants();
        return StreamCodec.ofMember(
            (val, buf) -> ByteBufCodecs.VAR_INT.encode(buf, val.ordinal()),
            buf -> values[ByteBufCodecs.VAR_INT.decode(buf)]
        );
    }
}
