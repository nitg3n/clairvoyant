package com.nitg3n.clairvoyant.services

import com.nitg3n.clairvoyant.Clairvoyant
import com.nitg3n.clairvoyant.models.ActionData
import com.nitg3n.clairvoyant.models.ActionType
import org.bukkit.Bukkit
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Data class to hold the results of heuristic analysis.
 */
data class SuspicionReport(
    val playerName: String,
    val playerUUID: UUID,
    val overallScore: Double,
    val reportDetails: List<String>
)

/**
 * HeuristicsEngine analyzes player behavior data to assess the likelihood of X-ray usage.
 */
class HeuristicsEngine(
    private val plugin: Clairvoyant,
    private val databaseManager: DatabaseManager,
    private val config: ConfigManager
) {

    /**
     * Helper class that holds the context for a single analysis run to avoid redundant data lookups.
     */
    private data class AnalysisContext(
        val allActions: List<ActionData>,
        val breakActions: List<ActionData>,
        val valuableFinds: List<ActionData>,
        val highValueOres: Set<String>
    )

    /**
     * Analyzes a player's data and generates a suspicion report.
     * If the score exceeds the threshold, it automatically punishes the player.
     * @param playerUUID The UUID of the player to analyze.
     */
    fun analyzeAndAct(playerUUID: UUID) {
        val report = analyzePlayer(playerUUID)

        // Check auto-punish conditions
        if (config.autoPunishEnabled && report.overallScore >= config.autoPunishThresholdScore) {
            punishPlayer(playerUUID, report.overallScore)
        }
    }

    /**
     * Punishes a player using the configured command.
     * @param playerUUID The UUID of the player to punish.
     * @param score The player's score at the time of punishment.
     */
    private fun punishPlayer(playerUUID: UUID, score: Double) {
        val player = Bukkit.getOfflinePlayer(playerUUID)
        // Replace the %player% placeholder with the actual player name
        val command = config.autoPunishCommand.replace("%player%", player.name ?: "Unknown", ignoreCase = true)
        plugin.logger.info("Auto-punishing player ${player.name} (score: $score). Executing command: '$command'")

        // Execute the command on the server's main thread
        Bukkit.getScheduler().runTask(plugin, Runnable {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
        })

        // Reset score and data to prevent repeated punishment
        // Note: To fully implement this, a method like clearPlayerActions in DatabaseManager would be needed.
        // databaseManager.clearPlayerActions(playerUUID)
        plugin.logger.info("Action data for player ${player.name} should be cleared to prevent re-punishment.")
    }

    /**
     * Analyzes a player's data and generates a suspicion report. (Internal logic)
     * @param playerUUID The UUID of the player to analyze.
     * @return A SuspicionReport object.
     */
    fun analyzePlayer(playerUUID: UUID): SuspicionReport {
        val allActions = databaseManager.getPlayerActions(playerUUID)
        val playerName = Bukkit.getOfflinePlayer(playerUUID).name ?: "Unknown"

        if (allActions.size < config.getMinTotalForAnalysis()) {
            return SuspicionReport(playerName, playerUUID, 0.0, listOf("Not enough data available for this player (<${config.getMinTotalForAnalysis()} actions)."))
        }

        val context = AnalysisContext(
            allActions = allActions,
            breakActions = allActions.filter { it.actionType == ActionType.BLOCK_BREAK },
            highValueOres = config.getHighValueOres(),
            valuableFinds = allActions.filter { it.actionType == ActionType.BLOCK_BREAK && it.material in config.getHighValueOres() }
        )

        val heuristics: List<(AnalysisContext) -> Triple<String, Double, String>> = listOf(
            ::calculateOreToStoneRatio,
            ::analyzeAnomalousMining,
            ::analyzeYLevelDistribution,
            ::analyzeTunnelingPatterns,
            ::analyzeMiningPurity,
            ::analyzePathEfficiency,
            ::analyzeTorchUsage,
            ::analyzeTimeAndDistance,
            ::analyzeInitialDiscoveryTime
        )

        val reports = heuristics.map { it(context) }

        val weightedScores = mutableMapOf<String, Double>()
        reports.forEach { (key, score, _) ->
            weightedScores[key] = score * config.getWeight(key)
        }

        val overallScore = weightedScores.values.sum()
        val reportDetails = reports.map { it.third }

        return SuspicionReport(playerName, playerUUID, overallScore.coerceIn(0.0, 100.0), reportDetails)
    }

    // --- Heuristic Functions ---

    private fun calculateOreToStoneRatio(ctx: AnalysisContext): Triple<String, Double, String> {
        val key = "high-value-ore-ratio"
        val commonStones = config.getStoneTypes()
        val oresMined = ctx.valuableFinds.size.toDouble()
        val stonesMined = ctx.breakActions.count { it.material in commonStones }.toDouble()

        if (stonesMined < config.getMinStoneForAnalysis()) {
            return Triple(key, 0.0, "[High-Value Ore Ratio] Not enough stone blocks mined.")
        }

        val ratio = if (stonesMined > 0) oresMined / stonesMined else 0.0
        val threshold = config.getHighValueRatioThreshold()
        val score = if (ratio > threshold) ((ratio - threshold) * 5000).coerceIn(0.0, 100.0) else 0.0

        val report = "[High-Value Ore Ratio | Score: %.1f] Mined %d high-value ores vs %d common blocks. (Ratio: %.4f)".format(score, oresMined.toInt(), stonesMined.toInt(), ratio)
        return Triple(key, score, report)
    }

    private fun analyzeAnomalousMining(ctx: AnalysisContext): Triple<String, Double, String> {
        val key = "anomalous-mining"
        val commonStonesMined = ctx.breakActions.count { it.material in config.getStoneTypes() }.toDouble()
        if (commonStonesMined < config.getMinStoneForAnalysis()) {
            return Triple(key, 0.0, "[Anomalous Mining] Not enough stone blocks mined.")
        }

        var maxScore = 0.0
        var reportDetails = ""

        for ((oreName, oreMaterials) in config.getCommonOres()) {
            val oreMined = ctx.breakActions.count { it.material in oreMaterials }.toDouble()
            val threshold = config.getCommonOreRatioThreshold(oreName)
            val ratio = if (commonStonesMined > 0) oreMined / commonStonesMined else 0.0

            val score = if (ratio > threshold) ((ratio - threshold) * 1000).coerceIn(0.0, 100.0) else 0.0

            if (score > maxScore) {
                maxScore = score
                reportDetails = "(Target: $oreName, Ratio: %.4f)".format(ratio)
            }
        }

        val report = "[Anomalous Mining | Score: %.1f] Checked common ore ratios. %s".format(maxScore, reportDetails)
        return Triple(key, maxScore, report)
    }

    private fun analyzeYLevelDistribution(ctx: AnalysisContext): Triple<String, Double, String> {
        val key = "y-level-analysis"
        val suspiciousRanges = config.getSuspiciousYRanges()
        val breaksInSuspiciousZones = ctx.breakActions.filter { action -> suspiciousRanges.any { range -> action.y in range } }

        val totalBreaks = ctx.breakActions.size.toDouble()
        if (totalBreaks < 1) return Triple(key, 0.0, "[Y-Level Analysis] No break actions found.")

        val concentrationRatio = breaksInSuspiciousZones.size / totalBreaks
        val concentrationThreshold = config.getYLevelConcentrationRatio()
        val concentrationScore = if (concentrationRatio > concentrationThreshold) ((concentrationRatio - concentrationThreshold) * 200).coerceIn(0.0, 100.0) else 0.0

        val subContext = ctx.copy(breakActions = breaksInSuspiciousZones, valuableFinds = breaksInSuspiciousZones.filter { it.material in ctx.highValueOres })
        val (_, zoneOreScore, _) = calculateOreToStoneRatio(subContext)

        val finalScore = (concentrationScore * 0.4) + (zoneOreScore * 0.6)
        val report = "[Y-Level Analysis | Score: %.1f] %.2f%% of mining at suspicious Y-levels (sub-score: %.1f).".format(finalScore, concentrationRatio * 100, zoneOreScore)
        return Triple(key, finalScore, report)
    }

    private fun analyzeTunnelingPatterns(ctx: AnalysisContext): Triple<String, Double, String> {
        val key = "tunneling-pattern"
        if (ctx.valuableFinds.size < 2) {
            return Triple(key, 0.0, "[Tunneling | Score: 0.0] Not enough valuable ore finds.")
        }

        var suspiciousTunnels = 0
        val varianceThreshold = config.getTunnelVarianceThreshold()

        for (find in ctx.valuableFinds) {
            val precedingPath = getPrecedingPath(find, ctx.breakActions, 50)
            if (precedingPath.size < 10) continue

            val xVar = calculateVariance(precedingPath.map { it.x.toDouble() })
            val yVar = calculateVariance(precedingPath.map { it.y.toDouble() })
            val zVar = calculateVariance(precedingPath.map { it.z.toDouble() })

            // Detect straight line or vertical/horizontal tunnel patterns
            if ((xVar < varianceThreshold && zVar < varianceThreshold) || // Vertical
                (xVar < varianceThreshold && yVar < varianceThreshold) || // Z-axis straight line
                (yVar < varianceThreshold && zVar < varianceThreshold)) { // X-axis straight line
                suspiciousTunnels++
            }
        }

        val score = (suspiciousTunnels.toDouble() / ctx.valuableFinds.size) * 150
        val report = "[Tunneling | Score: %.1f] Detected %d straight/vertical tunnel(s).".format(score.coerceIn(0.0, 100.0), suspiciousTunnels)
        return Triple(key, score.coerceIn(0.0, 100.0), report)
    }

    private fun analyzeMiningPurity(ctx: AnalysisContext): Triple<String, Double, String> {
        val key = "mining-purity"
        if (ctx.valuableFinds.isEmpty()) {
            return Triple(key, 0.0, "[Mining Purity | Score: 0.0] No high-value ores found.")
        }

        var suspiciousPaths = 0
        val commonOres = config.getCommonOres().values.flatten().toSet()
        val ignorableInteractions = config.getIgnorableInteractions()

        for (find in ctx.valuableFinds) {
            val precedingPath = getPrecedingPath(find, ctx.breakActions, config.getMiningPurityWindow())
            if (precedingPath.isEmpty()) continue

            val nonOreBlocks = precedingPath.count { it.material !in commonOres && it.material !in ignorableInteractions }
            val purityRatio = nonOreBlocks.toDouble() / precedingPath.size

            if (purityRatio >= config.getMiningPuritySuspiciousRatio()) {
                val interactions = ctx.allActions.any { it.actionType == ActionType.INTERACT && it.timestamp >= precedingPath.first().timestamp && it.timestamp < find.timestamp }
                if (!interactions) {
                    suspiciousPaths++
                }
            }
        }

        val score = (suspiciousPaths.toDouble() / ctx.valuableFinds.size) * 100
        val report = "[Mining Purity | Score: %.1f] Detected %d case(s) of ignoring interactions.".format(score, suspiciousPaths)
        return Triple(key, score.coerceIn(0.0, 100.0), report)
    }

    private fun analyzePathEfficiency(ctx: AnalysisContext): Triple<String, Double, String> {
        val key = "path-efficiency"
        if (ctx.valuableFinds.size < 3) {
            return Triple(key, 0.0, "[Path Efficiency | Score: 0.0] Not enough finds for analysis.")
        }

        var totalPathDistance = 0.0
        var totalStraightLineDistance = 0.0

        val valuableFindsSorted = ctx.valuableFinds.sortedBy { it.timestamp }

        for (i in 0 until valuableFindsSorted.size - 1) {
            val find1 = valuableFindsSorted[i]
            val find2 = valuableFindsSorted[i+1]

            totalStraightLineDistance += getDistance(find1, find2)

            val pathSegment = ctx.breakActions.filter { it.timestamp > find1.timestamp && it.timestamp <= find2.timestamp }
            if (pathSegment.size < 2) continue
            for (j in 0 until pathSegment.size - 1) {
                totalPathDistance += getDistance(pathSegment[j], pathSegment[j+1])
            }
        }

        if (totalStraightLineDistance < 1) return Triple(key, 0.0, "[Path Efficiency | Score: 0.0] Path distance is too short.")

        val efficiencyRatio = if (totalStraightLineDistance > 0) totalPathDistance / totalStraightLineDistance else 1.0
        val suspiciousRatio = config.getPathEfficiencySuspiciousRatio()

        val score = if (efficiencyRatio < suspiciousRatio) ((1.0 - (efficiencyRatio - 1.0) / (suspiciousRatio - 1.0)) * 100) else 0.0
        val report = "[Path Efficiency | Score: %.1f] Path efficiency ratio: %.2f".format(score.coerceIn(0.0, 100.0), efficiencyRatio)
        return Triple(key, score.coerceIn(0.0, 100.0), report)
    }

    private fun analyzeTorchUsage(ctx: AnalysisContext): Triple<String, Double, String> {
        val key = "torch-usage"
        val yLimit = config.getTorchCheckYLevel()
        val deepActions = ctx.allActions.filter { it.y < yLimit }

        val breakCount = deepActions.count { it.actionType == ActionType.BLOCK_BREAK }.toDouble()
        val torchCount = deepActions.count { it.actionType == ActionType.BLOCK_PLACE && (it.material == "TORCH" || it.material == "SOUL_TORCH") }.toDouble()

        if (breakCount < config.getTorchMinBlocks()) {
            return Triple(key, 0.0, "[Torch Usage | Score: 0.0] Not enough blocks broken below Y=$yLimit.")
        }

        val ratio = breakCount / (torchCount + 1)
        val suspiciousRatio = config.getTorchSuspiciousRatio()

        val score = if (ratio > suspiciousRatio) (((ratio / suspiciousRatio) - 1.0) * 100).coerceIn(0.0, 100.0) else 0.0
        val report = "[Torch Usage | Score: %.1f] Broke %.0f blocks per torch placed below Y=%d.".format(score, ratio, yLimit)
        return Triple(key, score, report)
    }

    private fun analyzeTimeAndDistance(ctx: AnalysisContext): Triple<String, Double, String> {
        val key = "time-distance"
        if (ctx.valuableFinds.size < 2) {
            return Triple(key, 0.0, "[Time/Distance | Score: 0.0] Not enough valuable finds.")
        }

        var suspiciousFinds = 0
        val speedThreshold = config.getSuspiciousSpeedThreshold()
        val valuableFindsSorted = ctx.valuableFinds.sortedBy { it.timestamp }

        for (i in 0 until valuableFindsSorted.size - 1) {
            val find1 = valuableFindsSorted[i]
            val find2 = valuableFindsSorted[i + 1]
            val distance = getDistance(find1, find2)
            if (distance < 10) continue
            val timeDiffSeconds = (find2.timestamp - find1.timestamp) / 1000.0
            if (timeDiffSeconds < 1) continue
            if ((distance / timeDiffSeconds) > speedThreshold) suspiciousFinds++
        }

        val score = (suspiciousFinds.toDouble() / ctx.valuableFinds.size) * 100
        val report = "[Time/Distance | Score: %.1f] Detected %d suspiciously fast travel(s).".format(score.coerceIn(0.0, 100.0), suspiciousFinds)
        return Triple(key, score.coerceIn(0.0, 100.0), report)
    }

    private fun analyzeInitialDiscoveryTime(ctx: AnalysisContext): Triple<String, Double, String> {
        val key = "initial-discovery"
        val firstZoneEntry = ctx.allActions.find { it.actionType == ActionType.ZONE_ENTRY } ?: return Triple(key, 0.0, "[Initial Discovery | Score: 0.0] Player has not entered a suspicious zone.")

        val firstHighValueFind = ctx.valuableFinds.find { it.timestamp > firstZoneEntry.timestamp } ?: return Triple(key, 0.0, "[Initial Discovery | Score: 0.0] No high-value ores found after entering zone.")

        val timeDiffSeconds = (firstHighValueFind.timestamp - firstZoneEntry.timestamp) / 1000.0
        val suspiciousTime = config.getInitialDiscoverySuspiciousTime().toDouble()

        val score = if (timeDiffSeconds < suspiciousTime) ((1.0 - (timeDiffSeconds / suspiciousTime)) * 100).coerceIn(0.0, 100.0) else 0.0
        val report = "[Initial Discovery | Score: %.1f] First high-value ore found in %.1f seconds.".format(score, timeDiffSeconds)
        return Triple(key, score, report)
    }

    // --- Helper Functions ---

    private fun getPrecedingPath(find: ActionData, breakActions: List<ActionData>, window: Int): List<ActionData> {
        val findIndex = breakActions.indexOfFirst { it.timestamp == find.timestamp && it.x == find.x && it.y == find.y && it.z == find.z }
        if (findIndex == -1 || findIndex < window) return emptyList()
        return breakActions.subList(findIndex - window, findIndex)
    }

    private fun getDistance(a: ActionData, b: ActionData): Double {
        return sqrt((a.x - b.x).toDouble().pow(2) + (a.y - b.y).toDouble().pow(2) + (a.z - b.z).toDouble().pow(2))
    }

    private fun calculateVariance(data: List<Double>): Double {
        if (data.size < 2) return 0.0
        val mean = data.average()
        return data.sumOf { (it - mean).pow(2) } / data.size
    }
}