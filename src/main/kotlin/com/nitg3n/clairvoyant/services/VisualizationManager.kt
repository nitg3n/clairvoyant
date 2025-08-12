package com.nitg3n.clairvoyant.services

import com.nitg3n.clairvoyant.Clairvoyant
import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * An extension function to convert an ActionData object to a Bukkit Location.
 * Adds 0.5 to X and Z to center the location within the block.
 */
fun ActionData.toLocation(): Location = Location(Bukkit.getWorld(this.world), this.x.toDouble() + 0.5, this.y.toDouble(), this.z.toDouble() + 0.5)

/**
 * Manages the visualization of player actions using Display and Interaction entities.
 */
class VisualizationManager(
    private val plugin: Clairvoyant,
    private val databaseManager: DatabaseManager,
    private val configManager: ConfigManager
) {

    // Maps an Interaction entity's UUID to the corresponding ActionData.
    val interactionDataMap = ConcurrentHashMap<UUID, ActionData>()
    // Tracks the set of visualized entities for each admin player.
    private val activeVisualizations = ConcurrentHashMap<UUID, MutableSet<UUID>>()

    /**
     * Visualizes the recent actions of a target player for an admin.
     * @param admin The admin player executing the command.
     * @param targetPlayerName The name of the player to trace.
     */
    suspend fun visualizePlayerActions(admin: Player, targetPlayerName: String) {
        val targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName)
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                admin.sendMessage(Component.text("Player '$targetPlayerName' not found.", NamedTextColor.RED))
            })
            return
        }

        val actions = databaseManager.getPlayerActions(targetPlayer.uniqueId)
            .filter { it.actionType == ActionType.BLOCK_BREAK }
            .sortedBy { it.timestamp }
            .takeLast(200) // Limit to the last 200 actions for performance.

        plugin.logger.info("[DEBUG] Found ${actions.size} actions to visualize for ${targetPlayer.name}.")

        if (actions.isEmpty()) {
            plugin.server.scheduler.runTask(plugin, Runnable {
                admin.sendMessage(Component.text("No recent mining data found for ${targetPlayer.name}.", NamedTextColor.YELLOW))
            })
            return
        }

        // Switch to the main server thread to create entities.
        plugin.server.scheduler.runTask(plugin, Runnable {
            clearVisualization(admin) // Clear any previous visualization for this admin.

            val visualMap = configManager.getVisualizationMapping()
            val defaultMarker = configManager.getDefaultVisualMarker()
            val newEntities = mutableSetOf<UUID>()

            plugin.logger.info("[DEBUG] Now on main thread. Starting to create ${actions.size} visualization entities.")

            actions.forEach { action ->
                try {
                    val location = action.toLocation()
                    val originalMaterial = Material.valueOf(action.material.uppercase())
                    val displayMaterial = visualMap[originalMaterial] ?: defaultMarker

                    // Create the visible BlockDisplay entity.
                    val display = location.world.spawn(location, BlockDisplay::class.java) {
                        it.block = displayMaterial.createBlockData()
                        it.isVisibleByDefault = false // Only visible to specific players.
                        it.brightness = Display.Brightness(15, 15) // Set max brightness to be visible in the dark.
                        // Apply a transformation to center the model within the block space.
                        it.transformation = Transformation(
                            Vector3f(-0.5f, 0.0f, -0.5f), // Translate model to center it.
                            Quaternionf(), // No rotation.
                            Vector3f(1.0f, 1.0f, 1.0f),  // Default scale.
                            Quaternionf()  // No rotation.
                        )
                    }

                    // Create the invisible Interaction entity for click detection.
                    val interaction = location.world.spawn(location, Interaction::class.java) {
                        it.interactionWidth = 1.0f
                        it.interactionHeight = 1.0f
                        it.isVisibleByDefault = false
                    }

                    // Make the entities visible only to the admin.
                    admin.showEntity(plugin, display)
                    admin.showEntity(plugin, interaction)

                    // Store the data for later retrieval.
                    interactionDataMap[interaction.uniqueId] = action
                    newEntities.add(display.uniqueId)
                    newEntities.add(interaction.uniqueId)

                } catch (e: Exception) {
                    plugin.logger.warning("[DEBUG] Failed to spawn entity for action: $action. Reason: ${e.message}")
                }
            }
            activeVisualizations[admin.uniqueId] = newEntities
            plugin.logger.info("[DEBUG] Finished creating visualization. ${newEntities.size} entities were stored.")

            admin.sendMessage(Component.text("Trace for ${targetPlayer.name} has been visualized.", NamedTextColor.GREEN))
            admin.sendMessage(Component.text("Right-click a marker to see details. It will automatically disappear after 5 minutes.", NamedTextColor.GRAY))

            // Schedule a task to automatically remove the visualization after 5 minutes.
            object : BukkitRunnable() {
                override fun run() {
                    clearVisualization(admin)
                    plugin.logger.info("[DEBUG] Automatically cleared visualization for ${admin.name}.")
                }
            }.runTaskLater(plugin, 20 * 60 * 5L) // 20 ticks/sec * 60 sec/min * 5 min
        })
    }

    /**
     * Removes all visualization entities created for a specific admin.
     * @param admin The player whose visualization should be cleared.
     */
    fun clearVisualization(admin: Player) {
        val entityUUIDs = activeVisualizations.remove(admin.uniqueId) ?: return
        entityUUIDs.forEach { uuid ->
            Bukkit.getEntity(uuid)?.remove()
            interactionDataMap.remove(uuid)
        }
        plugin.logger.info("[DEBUG] Cleared ${entityUUIDs.size} visualization entities for ${admin.name}.")
    }
}