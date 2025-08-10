package com.nitg3n.clairvoyant.listeners

import com.nitg3n.clairvoyant.models.PlayerMoveAction
import com.nitg3n.clairvoyant.services.LogProcessor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent

object PlayerMoveListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (event.hasChangedBlock()) {
            val to = event.to
            LogProcessor.enqueue(PlayerMoveAction(
                playerUuid = event.player.uniqueId,
                timestamp = System.currentTimeMillis(),
                world = to.world.name,
                x = to.blockX,
                y = to.blockY,
                z = to.blockZ
            ))
        }
    }
}