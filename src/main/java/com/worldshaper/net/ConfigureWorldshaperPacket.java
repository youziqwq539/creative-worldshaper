package com.worldshaper.net;

import com.worldshaper.WSDataComponents;
import com.worldshaper.brush.PlacementOptions;
import com.worldshaper.brush.PlacementPatterns;
import com.worldshaper.brush.TerrainBrushes;
import com.worldshaper.brush.TerrainTools;
import com.worldshaper.item.WorldshaperItem;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record ConfigureWorldshaperPacket(
    InteractionHand hand,
    PlacementPatterns pattern,
    TerrainBrushes brush,
    int brushParamX,
    int brushParamY,
    int brushParamZ,
    TerrainTools tool,
    PlacementOptions placement
) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, ConfigureWorldshaperPacket> STREAM_CODEC = StreamCodec.ofMember(
        (packet, buf) -> {
            WSStreamCodecs.HAND.encode(buf, packet.hand);
            PlacementPatterns.STREAM_CODEC.encode(buf, packet.pattern);
            TerrainBrushes.STREAM_CODEC.encode(buf, packet.brush);
            ByteBufCodecs.VAR_INT.encode(buf, packet.brushParamX);
            ByteBufCodecs.VAR_INT.encode(buf, packet.brushParamY);
            ByteBufCodecs.VAR_INT.encode(buf, packet.brushParamZ);
            TerrainTools.STREAM_CODEC.encode(buf, packet.tool);
            PlacementOptions.STREAM_CODEC.encode(buf, packet.placement);
        },
        buf -> new ConfigureWorldshaperPacket(
            WSStreamCodecs.HAND.decode(buf),
            PlacementPatterns.STREAM_CODEC.decode(buf),
            TerrainBrushes.STREAM_CODEC.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            ByteBufCodecs.VAR_INT.decode(buf),
            TerrainTools.STREAM_CODEC.decode(buf),
            PlacementOptions.STREAM_CODEC.decode(buf)
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return WSPackets.CONFIGURE_TYPE;
    }

    public void handle(Player player) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof WorldshaperItem) {
            WorldshaperItem.configureSettings(stack, pattern, brush, brushParamX, brushParamY, brushParamZ, tool, placement);
        }
    }
}
