package com.worldshaper;

import java.util.function.Supplier;

import com.worldshaper.item.WorldshaperItem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class WSCreativeTabs {

    public static final Supplier<CreativeModeTab> WORLDSHAPER =
        WorldshaperMod.CREATIVE_TABS.register("worldshaper", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.worldshaper"))
            .icon(() -> new ItemStack(WorldshaperItem.WORLDSHAPER.get()))
            .displayItems((parameters, output) -> output.accept(WorldshaperItem.WORLDSHAPER.get()))
            .build());

    public static void register() {
        // Triggers static initialization
    }
}
