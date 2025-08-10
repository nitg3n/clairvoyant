package com.nitg3n.clairvoyant.services

import com.nitg3n.clairvoyant.Clairvoyant
import com.nitg3n.clairvoyant.models.MinedBlockAction
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.entity.Interaction
import org.bukkit.entity.Player
import org.bukkit.metadata.FixedMetadataValue
import java.util.UUID

class VisualizationManager(private val plugin: Clairvoyant) {
    private val activeVisualizations = mutableMapOf<UUID, MutableSet<Entity>>()
    val TRACE_METADATA_KEY = "ClairvoyantTraceEntity"

    fun showTrace(admin: Player, actions: List<MinedBlockAction>) {
        clearVisualization(admin)
        val world = admin.world
        val entities = mutableSetOf<Entity>()

        actions.forEach { action ->
            val location = Location(world, action.x + 0.5, action.y + 0.5, action.z + 0.5)
            val material = Material.getMaterial(action.blockMaterial) ?: Material.STONE
            val blockDisplay = world.spawn(location, BlockDisplay::class.java) {
                it.block = material.createBlockData()
                it.isVisibleByDefault = false
            }
            val interaction = world.spawn(location, Interaction::class.java) {
                it.interactionWidth = 1.0f
                it.interactionHeight = 1.0f
                it.isVisibleByDefault = false
            }
            val metadataValue = FixedMetadataValue(plugin, action.timestamp)
            listOf(blockDisplay, interaction).forEach {
                it.setMetadata(TRACE_METADATA_KEY, metadataValue)
                admin.showEntity(plugin, it)
                entities.add(it)
            }
        }
        activeVisualizations[admin.uniqueId] = entities
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            if (activeVisualizations.containsKey(admin.uniqueId)) {
                clearVisualization(admin)
                admin.sendMessage(Component.text("Trace visualization has been cleared.", NamedTextColor.GRAY))
            }
        }, 20 * 60 * 5)
    }

    fun clearVisualization(admin: Player) {
        activeVisualizations.remove(admin.uniqueId)?.forEach { if (it.isValid) it.remove() }
    }

    fun clearAllVisualizations() {
        activeVisualizations.values.forEach { set -> set.forEach { if (it.isValid) it.remove() } }
        activeVisualizations.clear()
    }
}