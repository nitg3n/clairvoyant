package com.nitg3n.clairvoyant.listeners

import com.nitg3n.clairvoyant.models.MinedBlockAction
import com.nitg3n.clairvoyant.services.LogProcessor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

object BlockBreakListener : Listener {
    private val valuableOres = setOf(
        Material.COAL_ORE, Material.COPPER_ORE, Material.IRON_ORE, Material.GOLD_ORE,
        Material.REDSTONE_ORE, Material.LAPIS_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE,
        Material.DEEPSLATE_COAL_ORE, Material.DEEPSLATE_COPPER_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.DEEPSLATE_GOLD_ORE, Material.DEEPSLATE_REDSTONE_ORE, Material.DEEPSLATE_LAPIS_ORE,
        Material.DEEPSLATE_DIAMOND_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS
    )

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        if (player.gameMode == GameMode.SURVIVAL && block.type in valuableOres) {
            LogProcessor.enqueue(MinedBlockAction(
                playerUuid = player.uniqueId,
                timestamp = System.currentTimeMillis(),
                world = block.world.name,
                x = block.x,
                y = block.y,
                z = block.z,
                blockMaterial = block.type.name
            ))
        }
    }
}