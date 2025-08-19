package com.nitg3n.clairvoyant.listeners

import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import com.nitg3n.clairvoyant.services.ConfigManager
import com.nitg3n.clairvoyant.services.DatabaseManager
import com.nitg3n.clairvoyant.services.HeuristicsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

/**
 * Listens for block break events, logs them, and triggers heuristic analysis periodically.
 */
class BlockBreakListener(
    private val databaseManager: DatabaseManager,
    private val heuristicsEngine: HeuristicsEngine,
    private val configManager: ConfigManager
) : Listener {

    private val listenerScope = CoroutineScope(Dispatchers.Default)
    // This value can be made configurable in config.yml if needed.
    private val analysisCheckInterval = 100

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
        // This is now an async call to prevent blocking the main server thread.
        listenerScope.launch {
            databaseManager.logAction(actionData)
            triggerAnalysisIfNeeded(player)
        }
    }

    /**
     * Checks if an analysis should be run for the player and triggers it if conditions are met.
     */
    private fun triggerAnalysisIfNeeded(player: Player) {
        val actionCount = databaseManager.getActionCount(player.uniqueId)
        val minThreshold = configManager.getMinTotalForAnalysis()

        // Run analysis only when the total action count exceeds the minimum threshold
        // and at a specific interval to avoid performance issues.
        if (actionCount > minThreshold && actionCount % analysisCheckInterval == 0L) {
            heuristicsEngine.analyzeAndAct(player.uniqueId)
        }
    }
}