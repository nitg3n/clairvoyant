package com.nitg3n.clairvoyant.listeners

import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import com.nitg3n.clairvoyant.services.DatabaseManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

/**
 * Listens for block break events and logs them to the database.
 */
class BlockBreakListener(private val databaseManager: DatabaseManager) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val location = block.location

        val actionData = ActionData(
            playerUUID = player.uniqueId,
            playerName = player.name,
            actionType = ActionType.BLOCK_BREAK,
            material = block.type.name,
            world = location.world.name,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
        databaseManager.logAction(actionData)
    }
}