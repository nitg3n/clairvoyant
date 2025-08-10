package com.nitg3n.clairvoyant.listeners

import com.nitg3n.clairvoyant.Clairvoyant
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import java.text.SimpleDateFormat
import java.util.Date

class InteractionListener(private val plugin: Clairvoyant) : Listener {
    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        if (entity.hasMetadata(plugin.visualizationManager.TRACE_METADATA_KEY)) {
            event.isCancelled = true
            val timestamp = entity.getMetadata(plugin.visualizationManager.TRACE_METADATA_KEY).firstOrNull()?.asLong() ?: return
            val date = Date(timestamp)
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            event.player.sendMessage(
                Component.text("Activity Time: ", NamedTextColor.AQUA)
                    .append(Component.text(format.format(date), NamedTextColor.WHITE))
            )
        }
    }
}