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
 * 플레이어의 이동 이벤트를 감지합니다.
 * LogProcessor 대신 DatabaseManager를 직접 사용하도록 수정되었습니다.
 */
class PlayerMoveListener(
    private val databaseManager: DatabaseManager,
    private val config: ConfigManager
) : Listener {

    private val playersInSuspiciousZone = mutableSetOf<UUID>()

    @EventHandler(ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
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
        else if (!isInZone && wasInZone) {
            playersInSuspiciousZone.remove(player.uniqueId)
        }
    }
}
