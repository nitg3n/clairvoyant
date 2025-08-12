package com.nitg3n.clairvoyant.listeners

import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import com.nitg3n.clairvoyant.services.DatabaseManager
import com.nitg3n.clairvoyant.services.VisualizationManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Interaction
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.text.SimpleDateFormat
import java.util.*

/**
 * Listens for various player interaction events.
 */
class InteractionListener(
    private val databaseManager: DatabaseManager,
    private val visualizationManager: VisualizationManager
) : Listener {

    private val trackedPlaceItems = setOf(Material.TORCH, Material.SOUL_TORCH)
    private val trackedInteractBlocks = setOf(Material.CHEST, Material.TRAPPED_CHEST, Material.SPAWNER, Material.BOOKSHELF)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    /**
     * Logs when a player places a tracked item (e.g., a torch).
     */
    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.block.type in trackedPlaceItems) {
            log(event.player, event.block.location, ActionType.BLOCK_PLACE, event.block.type.name)
        }
    }

    /**
     * Logs when a player right-clicks a tracked block (e.g., a chest).
     */
    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            val clickedBlock = event.clickedBlock
            if (clickedBlock != null && clickedBlock.type in trackedInteractBlocks) {
                log(event.player, clickedBlock.location, ActionType.INTERACT, clickedBlock.type.name)
            }
        }
    }

    /**
     * Handles interaction with the visualization entities created by the /cv trace command.
     * When a player right-clicks a trace marker, it displays detailed information about that action.
     */
    @EventHandler
    fun onTraceEntityInteract(event: PlayerInteractEntityEvent) {
        val player = event.player
        val clickedEntity = event.rightClicked

        // Check if the right-clicked entity is a tracked Interaction entity
        if (clickedEntity is Interaction) {
            val actionData = visualizationManager.interactionDataMap[clickedEntity.uniqueId]
            if (actionData != null) {
                event.isCancelled = true // Prevent any default interaction
                val timestamp = dateFormat.format(Date(actionData.timestamp))

                player.sendMessage(
                    Component.text()
                        .append(Component.text("--- Action Details ---\n", NamedTextColor.GOLD))
                        .append(Component.text("Player: ", NamedTextColor.AQUA)).append(Component.text("${actionData.playerName}\n", NamedTextColor.WHITE))
                        .append(Component.text("Action: ", NamedTextColor.AQUA)).append(Component.text("${actionData.actionType}\n", NamedTextColor.WHITE))
                        .append(Component.text("Block: ", NamedTextColor.AQUA)).append(Component.text("${actionData.material}\n", NamedTextColor.WHITE))
                        .append(Component.text("Location: ", NamedTextColor.AQUA)).append(Component.text("X:${actionData.x} Y:${actionData.y} Z:${actionData.z}\n", NamedTextColor.WHITE))
                        .append(Component.text("Time: ", NamedTextColor.AQUA)).append(Component.text(timestamp, NamedTextColor.WHITE))
                        .build()
                )
            }
        }
    }

    /**
     * Helper function to create and log an ActionData object.
     */
    private fun log(player: org.bukkit.entity.Player, location: org.bukkit.Location, actionType: ActionType, material: String) {
        val actionData = ActionData(
            playerUUID = player.uniqueId,
            playerName = player.name,
            actionType = actionType,
            material = material,
            world = location.world.name,
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
        databaseManager.logAction(actionData)
    }
}