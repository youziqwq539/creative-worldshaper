package com.worldshaper;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

public class WSSoundEvents {

    public static final Supplier<SoundEvent> WORLDSHAPER_PLACE =
        WorldshaperMod.SOUND_EVENTS.register("worldshaper_place",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WorldshaperMod.MOD_ID, "worldshaper_place")));

    public static final Supplier<SoundEvent> DENY =
        WorldshaperMod.SOUND_EVENTS.register("deny",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WorldshaperMod.MOD_ID, "deny")));

    public static final Supplier<SoundEvent> CONFIRM =
        WorldshaperMod.SOUND_EVENTS.register("confirm",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(WorldshaperMod.MOD_ID, "confirm")));

    public static void register() {
        // Triggers static initialization
    }
}
