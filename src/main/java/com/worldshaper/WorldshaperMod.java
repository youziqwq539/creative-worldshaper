package com.worldshaper;

import com.worldshaper.item.WorldshaperItem;
import com.worldshaper.net.WSPackets;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(WorldshaperMod.MOD_ID)
public class WorldshaperMod {
    public static final String MOD_ID = "worldshaper";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.createDataComponents(MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public WorldshaperMod(IEventBus modBus) {
        WSDataComponents.register();
        WSSoundEvents.register();
        WorldshaperItem.register();
        WSCreativeTabs.register();

        ITEMS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        SOUND_EVENTS.register(modBus);
        CREATIVE_TABS.register(modBus);

        WSPackets.register(modBus);
    }
}
