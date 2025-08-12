package com.nitg3n.clairvoyant.listeners

import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import com.nitg3n.clairvoyant.services.ConfigManager
import com.nitg3n.clairvoyant.services.DatabaseManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID

/**
 * Listens for player movement events.
 */
class PlayerMoveListener(
    private val databaseManager: DatabaseManager,
    private val config: ConfigManager
) : Listener {

    // Tracks players who are currently inside a suspicious Y-level zone.
    private val playersInSuspiciousZone = mutableSetOf<UUID>()

    @EventHandler(ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        // Ignore minor movements within the same block
        if (event.from.blockX == event.to.blockX &&
            event.from.blockY == event.to.blockY &&
            event.from.blockZ == event.to.blockZ
        ) {
            return
        }

        val player = event.player
        val y = event.to.y.toInt()
        val isInZone = config.getSuspiciousYRanges().any { y in it }
        val wasInZone = playersInSuspiciousZone.contains(player.uniqueId)

        // Log an event when a player enters a suspicious Y-level zone
        if (isInZone && !wasInZone) {
            playersInSuspiciousZone.add(player.uniqueId)
            val location = player.location
            val actionData = ActionData(
                playerUUID = player.uniqueId,
                playerName = player.name,
                actionType = ActionType.ZONE_ENTRY,
                material = "Y=$y",
                world = location.world.name,
                x = location.blockX,
                y = location.blockY,
                z = location.blockZ
            )
            databaseManager.logAction(actionData)
        }
        // Update state when the player leaves the zone
        else if (!isInZone && wasInZone) {
            playersInSuspiciousZone.remove(player.uniqueId)
        }
    }
}