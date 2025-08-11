package com.nitg3n.clairvoyant.listeners

import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import com.nitg3n.clairvoyant.services.DatabaseManager
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent

/**
 * 플레이어의 상호작용 관련 이벤트를 감지합니다.
 * LogProcessor 대신 DatabaseManager를 직접 사용하도록 수정되었습니다.
 */
class InteractionListener(private val databaseManager: DatabaseManager) : Listener {

    private val trackedPlaceItems = setOf(Material.TORCH, Material.SOUL_TORCH)
    private val trackedInteractBlocks = setOf(Material.CHEST, Material.TRAPPED_CHEST, Material.SPAWNER, Material.BOOKSHELF)

    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.block.type in trackedPlaceItems) {
            log(event.player, event.block.location, ActionType.BLOCK_PLACE, event.block.type.name)
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            val clickedBlock = event.clickedBlock
            if (clickedBlock != null && clickedBlock.type in trackedInteractBlocks) {
                log(event.player, clickedBlock.location, ActionType.INTERACT, clickedBlock.type.name)
            }
        }
    }

    private fun log(player: org.bukkit.entity.Player, location: org.bukkit.Location, actionType: ActionType, material: String) {
        val actionData = ActionData(
            playerUUID = player.uniqueId,
            playerName = player.name,
            actionType = actionType,
            material = material,
            world = location.world?.name ?: "unknown",
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
        databaseManager.logAction(actionData)
    }
}
