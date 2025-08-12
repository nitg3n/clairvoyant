package com.nitg3n.clairvoyant.listeners

import com.nitg3n.clairvoyant.services.VisualizationManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/**
 * Listens for when a player quits the server.
 */
class PlayerQuitListener(private val visualizationManager: VisualizationManager) : Listener {

    /**
     * Cleans up any active visualizations for a player when they log out.
     * This prevents "ghost" entities from being left behind if an admin with an
     * active trace disconnects.
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        visualizationManager.clearVisualization(event.player)
    }
}