package com.nitg3n.clairvoyant.commands

import com.nitg3n.clairvoyant.Clairvoyant
import com.nitg3n.clairvoyant.services.DatabaseManager
import com.nitg3n.clairvoyant.services.HeuristicsEngine
import com.nitg3n.clairvoyant.services.SuspicionReport
import com.nitg3n.clairvoyant.services.VisualizationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * 플러그인의 모든 명령어를 관리하고 실행합니다.
 * (리팩토링: ChatColor -> Adventure API로 전환)
 */
class CommandManager(
    private val plugin: Clairvoyant,
    private val visualizationManager: VisualizationManager,
    private val databaseManager: DatabaseManager,
    private val heuristicsEngine: HeuristicsEngine
) : CommandExecutor, TabCompleter {

    private val commandScope = CoroutineScope(Dispatchers.Default)

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("clairvoyant.use")) {
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
                withContext(Dispatchers.Main) {
                    sender.sendMessage(Component.text("An error occurred while generating the trace.", NamedTextColor.RED))
                    plugin.logger.severe("Error during trace command: ${e.message}")
                    e.printStackTrace()
                }
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
                    withContext(Dispatchers.Main) { sender.sendMessage(Component.text("Player '$targetPlayerName' not found.", NamedTextColor.RED)) }
                    return@launch
                }

                val stats = databaseManager.getPlayerMiningStats(targetPlayer.uniqueId)
                withContext(Dispatchers.Main) {
                    if (stats.isEmpty()) {
                        sender.sendMessage(Component.text("No mining data found for ${targetPlayer.name}.", NamedTextColor.YELLOW))
                        return@withContext
                    }

                    val builder = Component.text()
                    builder.append(Component.text("--- Mining Stats for ${targetPlayer.name} ---\n", NamedTextColor.GOLD))
                    stats.entries.sortedByDescending { it.value }.forEach { (material, count) ->
                        builder.append(Component.text("$material: ", NamedTextColor.GRAY))
                        builder.append(Component.text("$count\n", NamedTextColor.WHITE))
                    }
                    sender.sendMessage(builder.build())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sender.sendMessage(Component.text("An error occurred while fetching stats.", NamedTextColor.RED))
                    plugin.logger.severe("Error during stats command: ${e.message}")
                    e.printStackTrace()
                }
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
                    withContext(Dispatchers.Main) { sender.sendMessage(Component.text("Player '$targetPlayerName' not found.", NamedTextColor.RED)) }
                    return@launch
                }

                withContext(Dispatchers.Main) { sender.sendMessage(Component.text("Running heuristics analysis for ${targetPlayer.name}...", NamedTextColor.AQUA)) }

                val report = heuristicsEngine.analyzePlayer(targetPlayer.uniqueId)
                withContext(Dispatchers.Main) {
                    sendReport(sender, report)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    sender.sendMessage(Component.text("An error occurred during the analysis.", NamedTextColor.RED))
                    plugin.logger.severe("Error during check command: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun sendReport(sender: CommandSender, report: SuspicionReport) {
        val overallScoreColor = when {
            report.overallScore > 70 -> NamedTextColor.DARK_RED
            report.overallScore > 40 -> NamedTextColor.RED
            else -> NamedTextColor.YELLOW
        }

        sender.sendMessage(
            Component.text()
                .append(Component.text("--- Heuristics Analysis Report for ${report.playerName} ---\n", NamedTextColor.GOLD))
                .append(Component.text("Overall Suspicion Score: ", Style.style(NamedTextColor.WHITE)))
                .append(Component.text("%.2f".format(report.overallScore), overallScoreColor, TextDecoration.BOLD))
                .append(Component.text(" / 100.0\n", Style.style(NamedTextColor.WHITE)))
                .build()
        )
        sender.sendMessage(Component.empty()) // 줄바꿈

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

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): MutableList<String> {
        if (args.size == 1) {
            return mutableListOf("trace", "stats", "check", "help").filter { it.startsWith(args[0], ignoreCase = true) }.toMutableList()
        }
        if (args.size == 2 && listOf("trace", "stats", "check").contains(args[0].lowercase())) {
            return Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }.toMutableList()
        }
        return mutableListOf()
    }
}
