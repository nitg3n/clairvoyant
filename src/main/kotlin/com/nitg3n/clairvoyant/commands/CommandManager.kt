package com.nitg3n.clairvoyant.commands

import com.nitg3n.clairvoyant.Clairvoyant
import com.nitg3n.clairvoyant.services.* // services.* 로 변경하여 ConfigManager 포함
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Manages and executes all plugin commands.
 */
class CommandManager(
    private val plugin: Clairvoyant,
    private val visualizationManager: VisualizationManager,
    private val databaseManager: DatabaseManager,
    private val heuristicsEngine: HeuristicsEngine,
    private val configManager: ConfigManager // 생성자에 configManager 추가
) : CommandExecutor, TabCompleter {

    private val commandScope = CoroutineScope(Dispatchers.Default)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("clairvoyant.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "trace" -> handleTraceCommand(sender, args)
            "stats" -> handleStatsCommand(sender, args)
            "check" -> handleCheckCommand(sender, args)
            "help" -> sendHelp(sender)
            else -> sender.sendMessage(Component.text("Unknown subcommand. Use /cv help for a list of commands.", NamedTextColor.RED))
        }

        return true
    }

    private fun handleTraceCommand(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(Component.text("This command can only be run by a player.", NamedTextColor.RED))
            return
        }
        if (args.size < 2) {
            sender.sendMessage(Component.text("Usage: /cv trace <player>", NamedTextColor.RED))
            return
        }
        val targetPlayerName = args[1]
        sender.sendMessage(Component.text("Starting trace for player $targetPlayerName...", NamedTextColor.AQUA))

        commandScope.launch {
            try {
                visualizationManager.visualizePlayerActions(sender, targetPlayerName)
            } catch (e: Exception) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage(Component.text("An error occurred while generating the trace.", NamedTextColor.RED))
                    plugin.logger.severe("Error during trace command: ${e.message}")
                    e.printStackTrace()
                })
            }
        }
    }

    private fun handleStatsCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Usage: /cv stats <player>", NamedTextColor.RED))
            return
        }
        val targetPlayerName = args[1]

        commandScope.launch {
            try {
                val targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName)
                if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
                    plugin.server.scheduler.runTask(plugin, Runnable { sender.sendMessage(Component.text("Player '$targetPlayerName' not found.", NamedTextColor.RED)) })
                    return@launch
                }

                val stats = databaseManager.getPlayerMiningStats(targetPlayer.uniqueId)

                plugin.server.scheduler.runTask(plugin, Runnable {
                    if (stats.isEmpty()) {
                        sender.sendMessage(Component.text("No mining data found for ${targetPlayer.name}.", NamedTextColor.YELLOW))
                        return@Runnable
                    }

                    val builder = Component.text()
                    builder.append(Component.text("--- Mining Stats for ${targetPlayer.name} ---\n", NamedTextColor.GOLD))
                    stats.entries.sortedByDescending { it.value }.forEach { (material, count) ->
                        builder.append(Component.text("$material: ", NamedTextColor.GRAY))
                        builder.append(Component.text("$count\n", NamedTextColor.WHITE))
                    }
                    sender.sendMessage(builder.build())
                })
            } catch (e: Exception) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage(Component.text("An error occurred while fetching stats.", NamedTextColor.RED))
                    plugin.logger.severe("Error during stats command: ${e.message}")
                    e.printStackTrace()
                })
            }
        }
    }

    private fun handleCheckCommand(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(Component.text("Usage: /cv check <player>", NamedTextColor.RED))
            return
        }
        val targetPlayerName = args[1]

        commandScope.launch {
            try {
                val targetPlayer = Bukkit.getOfflinePlayer(targetPlayerName)
                if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline) {
                    plugin.server.scheduler.runTask(plugin, Runnable { sender.sendMessage(Component.text("Player '$targetPlayerName' not found.", NamedTextColor.RED)) })
                    return@launch
                }

                plugin.server.scheduler.runTask(plugin, Runnable { sender.sendMessage(Component.text("Running heuristics analysis for ${targetPlayer.name}...", NamedTextColor.AQUA)) })

                val report = heuristicsEngine.analyzePlayer(targetPlayer.uniqueId)

                plugin.server.scheduler.runTask(plugin, Runnable {
                    sendReport(sender, report)
                })
            } catch (e: Exception) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    sender.sendMessage(Component.text("An error occurred during the analysis.", NamedTextColor.RED))
                    plugin.logger.severe("Error during check command: ${e.message}")
                    e.printStackTrace()
                })
            }
        }
    }

    /**
     * Formats and sends the suspicion report to the command sender.
     */
    private fun sendReport(sender: CommandSender, report: SuspicionReport) {
        val suspiciousThreshold = configManager.getSuspiciousThreshold()
        val dangerousThreshold = configManager.getDangerousThreshold()

        val (statusText, statusColor) = when {
            report.overallScore >= dangerousThreshold -> "[DANGEROUS]" to NamedTextColor.DARK_RED
            report.overallScore >= suspiciousThreshold -> "[SUSPICIOUS]" to NamedTextColor.YELLOW
            else -> "[NORMAL]" to NamedTextColor.GREEN
        }

        sender.sendMessage(
            Component.text()
                .append(Component.text("--- Heuristics Analysis Report for ${report.playerName} ---\n", NamedTextColor.GOLD))
                .append(Component.text("Status: ", Style.style(NamedTextColor.WHITE)))
                .append(Component.text(statusText, statusColor, TextDecoration.BOLD))
                .append(Component.text("\n", Style.style(NamedTextColor.WHITE)))
                .append(Component.text("Overall Suspicion Score: ", Style.style(NamedTextColor.WHITE)))
                .append(Component.text("%.2f".format(report.overallScore), statusColor, TextDecoration.BOLD))
                .append(Component.text(" / 100.0\n", Style.style(NamedTextColor.WHITE)))
                .build()
        )
        sender.sendMessage(Component.empty())

        report.reportDetails.forEach { detail ->
            val scoreMatch = """Score: (\d+\.\d)""".toRegex().find(detail)
            val score = scoreMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val scoreColor = when {
                score > 75 -> NamedTextColor.RED
                score > 50 -> NamedTextColor.YELLOW
                else -> NamedTextColor.GREEN
            }

            val scoreString = "Score: %.1f".format(score)
            val parts = detail.split(scoreString)

            val builder = Component.text()
            if (parts.isNotEmpty()) {
                builder.append(Component.text(parts[0], NamedTextColor.GRAY))
            }
            builder.append(Component.text("Score: ", NamedTextColor.GRAY))
            builder.append(Component.text("%.1f".format(score), scoreColor))
            if (parts.size > 1) {
                builder.append(Component.text(parts[1], NamedTextColor.GRAY))
            }

            sender.sendMessage(builder.build())
        }
    }

    /**
     * Sends the help message to the command sender.
     */
    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage(
            Component.text()
                .append(Component.text("--- Clairvoyant Help ---\n", NamedTextColor.GOLD))
                .append(Component.text("/cv trace <player>", NamedTextColor.AQUA))
                .append(Component.text(" - Visualize a player's recent actions.\n", NamedTextColor.WHITE))
                .append(Component.text("/cv stats <player>", NamedTextColor.AQUA))
                .append(Component.text(" - Show mining statistics for a player.\n", NamedTextColor.WHITE))
                .append(Component.text("/cv check <player>", NamedTextColor.AQUA))
                .append(Component.text(" - Analyze a player for suspicious activity.\n", NamedTextColor.WHITE))
                .append(Component.text("/cv help", NamedTextColor.AQUA))
                .append(Component.text(" - Shows this help message.", NamedTextColor.WHITE))
                .build()
        )
    }

    /**
     * Handles tab completion for commands.
     */
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if (args.size == 1) {
            return mutableListOf("trace", "stats", "check", "help").filter { it.startsWith(args[0], ignoreCase = true) }.toMutableList()
        }
        if (args.size == 2 && listOf("trace", "stats", "check").contains(args[0].lowercase())) {
            return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }.toMutableList()
        }
        return mutableListOf()
    }

    /**
     * Cancels all running coroutines in this scope.
     * Called when the plugin is disabled.
     */
    fun cancelJobs() {
        commandScope.cancel()
    }
}