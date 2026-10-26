package com.worldshaper.net;

import com.worldshaper.WorldshaperMod;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class WSPackets {

    public static final CustomPacketPayload.Type<ConfigureWorldshaperPacket> CONFIGURE_TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WorldshaperMod.MOD_ID, "configure_worldshaper"));

    public static final CustomPacketPayload.Type<WorldshaperBeamPacket> BEAM_TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WorldshaperMod.MOD_ID, "beam_effect"));

    public static final CustomPacketPayload.Type<UndoRedoPacket> UNDO_REDO_TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(WorldshaperMod.MOD_ID, "undo_redo"));

    public static void register(IEventBus modBus) {
        modBus.addListener(WSPackets::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(WorldshaperMod.MOD_ID).versioned("1.0.0");

        registrar.playToServer(CONFIGURE_TYPE, ConfigureWorldshaperPacket.STREAM_CODEC,
            (payload, context) -> payload.handle(context.player()));

        registrar.playToServer(UNDO_REDO_TYPE, UndoRedoPacket.STREAM_CODEC,
            (payload, context) -> payload.handle(context.player()));

        registrar.playToClient(BEAM_TYPE, WorldshaperBeamPacket.STREAM_CODEC,
            (payload, context) -> payload.handleClient());
    }
}
