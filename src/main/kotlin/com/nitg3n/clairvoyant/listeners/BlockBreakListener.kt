package com.nitg3n.clairvoyant.listeners

import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import com.nitg3n.clairvoyant.services.DatabaseManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

/**
 * 플레이어의 블록 파괴 이벤트를 감지하여 데이터베이스에 기록합니다.
 * LogProcessor 대신 DatabaseManager를 직접 사용하도록 수정되었습니다.
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
            world = location.world?.name ?: "unknown",
            x = location.blockX,
            y = location.blockY,
            z = location.blockZ
        )
        // 직접 DatabaseManager를 통해 로그 기록
        databaseManager.logAction(actionData)
    }
}
