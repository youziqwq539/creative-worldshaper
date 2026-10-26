package com.worldshaper.history;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Player;

/**
 * Registry for managing HistoryManager instances per player.
 */
public class HistoryManagerRegistry {
    private static final Map<UUID, HistoryManager> historyManagers = new ConcurrentHashMap<>();

    /**
     * Gets the HistoryManager for a player.
     */
    public static HistoryManager getHistoryManager(Player player) {
        return historyManagers.computeIfAbsent(player.getUUID(), uuid -> new HistoryManager());
    }

    /**
     * Removes the HistoryManager for a player (when they disconnect).
     */
    public static void removeHistoryManager(Player player) {
        historyManagers.remove(player.getUUID());
    }

    /**
     * Clears all history managers.
     */
    public static void clear() {
        historyManagers.clear();
    }
}