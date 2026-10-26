package com.worldshaper.net;

import com.worldshaper.WSDataComponents;
import com.worldshaper.history.HistoryManager;
import com.worldshaper.item.WorldshaperItem;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Packet for undo/redo operations.
 */
public record UndoRedoPacket(
    InteractionHand hand,
    boolean isUndo
) implements CustomPacketPayload {

    public static final StreamCodec<ByteBuf, UndoRedoPacket> STREAM_CODEC = StreamCodec.ofMember(
        (packet, buf) -> {
            WSStreamCodecs.HAND.encode(buf, packet.hand);
            ByteBufCodecs.BOOL.encode(buf, packet.isUndo);
        },
        buf -> new UndoRedoPacket(
            WSStreamCodecs.HAND.decode(buf),
            ByteBufCodecs.BOOL.decode(buf)
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return WSPackets.UNDO_REDO_TYPE;
    }

    public void handle(Player player) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof WorldshaperItem) {
            WorldshaperItem.performUndoRedo(stack, player, isUndo);
        }
    }
}