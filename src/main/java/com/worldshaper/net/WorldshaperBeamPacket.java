package com.worldshaper.net;

import com.worldshaper.client.WorldshaperRenderHandler;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record WorldshaperBeamPacket(
    Vec3 location,
    InteractionHand hand,
    boolean self,
    Vec3 target
) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, WorldshaperBeamPacket> STREAM_CODEC = StreamCodec.composite(
        WSStreamCodecs.VEC3, WorldshaperBeamPacket::location,
        WSStreamCodecs.HAND, WorldshaperBeamPacket::hand,
        ByteBufCodecs.BOOL, WorldshaperBeamPacket::self,
        WSStreamCodecs.VEC3, WorldshaperBeamPacket::target,
        WorldshaperBeamPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return WSPackets.BEAM_TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    public void handleClient() {
        Entity renderViewEntity = Minecraft.getInstance().getCameraEntity();
        if (renderViewEntity == null)
            return;
        if (renderViewEntity.position().distanceTo(location) > 100)
            return;

        WorldshaperRenderHandler handler = WorldshaperRenderHandler.INSTANCE;
        handler.addBeam(new WorldshaperRenderHandler.LaserBeam(location, target));
        if (self)
            handler.shoot(hand, location);
        else
            handler.playSound(hand, location);
    }
}
