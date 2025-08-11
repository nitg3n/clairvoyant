package com.nitg3n.clairvoyant.services

import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player

/**
 * ActionData 객체를 Bukkit의 Location 객체로 변환하는 확장 함수.
 */
fun ActionData.toLocation(): Location = Location(Bukkit.getWorld(this.world), this.x.toDouble(), this.y.toDouble(), this.z.toDouble())

/**
 * 플레이어의 행동을 시각화하는 기능을 관리합니다.
 * (리팩토링: ChatColor -> Adventure API로 전환)
 */
class VisualizationManager(
    private val databaseManager: DatabaseManager,
    private val configManager: ConfigManager
) {

    /**
     * 특정 플레이어의 채굴 활동을 관리자에게 가상 블록으로 보여줍니다.
     * @param admin 명령어를 실행한 관리자
     * @param targetPlayerName 추적할 플레이어의 이름
     */
    suspend fun visualizePlayerActions(admin: Player, targetPlayerName: String) {
        val targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName)
        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
            withContext(Dispatchers.Main) {
                admin.sendMessage(Component.text("Player '$targetPlayerName' not found.", NamedTextColor.RED))
            }
            return
        }

        val actions = databaseManager.getPlayerActions(targetPlayer.uniqueId)
            .filter { it.actionType == ActionType.BLOCK_BREAK }

        if (actions.isEmpty()) {
            withContext(Dispatchers.Main) {
                admin.sendMessage(Component.text("No mining data found for ${targetPlayer.name}.", NamedTextColor.YELLOW))
            }
            return
        }

        val visualMap = configManager.getVisualizationMapping()
        val defaultMarker = configManager.getDefaultVisualMarker()

        withContext(Dispatchers.Main) {
            actions.forEach { action ->
                try {
                    val originalMaterial = Material.valueOf(action.material.uppercase())
                    val displayMaterial = visualMap[originalMaterial] ?: defaultMarker
                    admin.sendBlockChange(action.toLocation(), displayMaterial.createBlockData())
                } catch (_: IllegalArgumentException) {
                    // 무시: 잘못된 블록 이름이 데이터에 있는 경우
                }
            }
            admin.sendMessage(Component.text("Trace for ${targetPlayer.name} has been visualized. It will disappear when you relog or move far away.", NamedTextColor.GREEN))
        }
    }
}
