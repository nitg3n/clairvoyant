package com.nitg3n.clairvoyant.commands

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.nitg3n.clairvoyant.Clairvoyant
import com.nitg3n.clairvoyant.models.MinedBlockAction
import com.nitg3n.clairvoyant.services.DatabaseManager
import com.nitg3n.clairvoyant.storage.PlayerActions
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SortOrder

object CommandManager {

    // 플러그인 전용 코루틴 스코프 생성
    private val scope = CoroutineScope(Dispatchers.Default)

    fun registerCommands(plugin: Clairvoyant, commands: Commands) {
        // Brigadier 명령어 빌더를 생성합니다.
        val commandBuilder: LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("clairvoyant")
            .requires { it.sender is Player && it.sender.hasPermission("clairvoyant.admin") }
            .then(
                Commands.literal("trace")
                    .requires { it.sender.hasPermission("clairvoyant.trace") }
                    .then(
                        Commands.argument("player", ArgumentTypes.player())
                            .executes { ctx ->
                                val admin = ctx.source.sender as Player
                                val target = ctx.getArgument("player", Player::class.java)
                                tracePlayer(plugin, admin, target)
                                1
                            }
                    )
            )
            .then(
                Commands.literal("stats")
                    .requires { it.sender.hasPermission("clairvoyant.stats") }
                    .then(
                        Commands.argument("player", ArgumentTypes.player())
                            .executes { ctx ->
                                ctx.source.sender.sendMessage(Component.text("Stats feature is not yet implemented.", NamedTextColor.YELLOW))
                                1
                            }
                    )
            )
            .then(
                Commands.literal("check")
                    .requires { it.sender.hasPermission("clairvoyant.check") }
                    .then(
                        Commands.argument("player", ArgumentTypes.player())
                            .executes { ctx ->
                                ctx.source.sender.sendMessage(Component.text("Check feature is not yet implemented.", NamedTextColor.YELLOW))
                                1
                            }
                    )
            )
            .executes { ctx ->
                ctx.source.sender.sendMessage(Component.text("--- Clairvoyant Help ---", NamedTextColor.GOLD))
                ctx.source.sender.sendMessage(Component.text("/cv trace <player>", NamedTextColor.AQUA))
                ctx.source.sender.sendMessage(Component.text("/cv stats <player>", NamedTextColor.AQUA))
                ctx.source.sender.sendMessage(Component.text("/cv check <player>", NamedTextColor.AQUA))
                1
            }

        // The command is registered using the registrar from the lifecycle event
        commands.register(commandBuilder.build(), "Clairvoyant main command", listOf("cv", "xray"))
    }

    private fun tracePlayer(plugin: Clairvoyant, admin: Player, target: Player) {
        admin.sendMessage(Component.text("Tracking recent 100 mining activities for ${target.name}...", NamedTextColor.GRAY))

        // 코루틴을 사용하여 비동기 작업 수행
        scope.launch {
            try {
                // 데이터베이스 조회 (IO 스레드에서 실행)
                val actions = DatabaseManager.dbQuery {
                    // 'select' 경고 해결을 위해 'selectAll().where' 구문 사용
                    PlayerActions.selectAll().where { PlayerActions.playerUuid eq target.uniqueId.toString() }
                        .orderBy(PlayerActions.timestamp, SortOrder.DESC)
                        .limit(100)
                        .mapNotNull {
                            if (it[PlayerActions.actionType] == "MINE") {
                                MinedBlockAction(
                                    playerUuid = target.uniqueId,
                                    timestamp = it[PlayerActions.timestamp],
                                    world = it[PlayerActions.world],
                                    x = it[PlayerActions.x],
                                    y = it[PlayerActions.y],
                                    z = it[PlayerActions.z],
                                    blockMaterial = it[PlayerActions.blockMaterial] ?: "STONE"
                                )
                            } else null
                        }
                }

                // Bukkit API 호출을 위해 메인 스레드로 전환
                withContext(Dispatchers.Main) {
                    if (actions.isEmpty()) {
                        admin.sendMessage(Component.text("${target.name} has no recent mining activity.", NamedTextColor.RED))
                        return@withContext
                    }
                    plugin.visualizationManager.showTrace(admin, actions)
                    admin.sendMessage(Component.text("Visualized ${actions.size} activities. They will disappear in 5 minutes.", NamedTextColor.GREEN))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    admin.sendMessage(Component.text("An error occurred while tracing: ${e.message}", NamedTextColor.RED))
                }
                e.printStackTrace()
            }
        }
    }
}