package com.nitg3n.clairvoyant.services

import com.nitg3n.clairvoyant.Clairvoyant
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration

/**
 * Manages loading and accessing settings from the config.yml file.
 */
class ConfigManager(private val plugin: Clairvoyant) {

    // Configuration variables related to auto-punishment
    var autoPunishEnabled: Boolean = true
        private set
    var autoPunishCommand: String = "kick %player% [Clairvoyant] Suspicious activity detected."
        private set
    var autoPunishThresholdScore: Double = 70.0
        private set

    init {
        // Creates and loads the default config.yml file if it doesn't exist.
        plugin.saveDefaultConfig()
        loadConfig()
    }

    /**
     * Reloads the configuration from the file and updates internal variables.
     */
    fun loadConfig() {
        plugin.reloadConfig()
        val config: FileConfiguration = plugin.config

        // Load auto-punish settings
        autoPunishEnabled = config.getBoolean("auto-punish.enabled", true)
        autoPunishCommand = config.getString("auto-punish.command", "kick %player% [Clairvoyant] Suspicious activity detected.")!!
        val thresholdLevel = config.getString("auto-punish.threshold-level", "dangerous")!!
        autoPunishThresholdScore = config.getDouble("thresholds.suspicion-levels.$thresholdLevel", 70.0)
    }

    /**
     * Sets the enabled state of the auto-punish feature and saves it to the file.
     * @param enabled Whether to enable the feature.
     */
    fun setAutoPunish(enabled: Boolean) {
        plugin.config.set("auto-punish.enabled", enabled)
        plugin.saveConfig()
        loadConfig() // Immediately reload the changed settings
    }


    // --- Getter functions for various configuration values ---

    fun getWeight(key: String): Double = plugin.config.getDouble("weights.$key", 0.0)

    fun getHighValueOres(): Set<String> = plugin.config.getStringList("ore-lists.high-value").toSet()
    fun getCommonOres(): Map<String, Set<String>> {
        val commonOresSection = plugin.config.getConfigurationSection("ore-lists.common") ?: return emptyMap()
        return commonOresSection.getKeys(false).associateWith { oreName ->
            commonOresSection.getStringList(oreName).toSet()
        }
    }
    fun getStoneTypes(): Set<String> = plugin.config.getStringList("ore-lists.stones").toSet()
    fun getIgnorableInteractions(): Set<String> = plugin.config.getStringList("ore-lists.ignorable-interactions").toSet()

    fun getVisualizationMapping(): Map<Material, Material> {
        val mappingSection = plugin.config.getConfigurationSection("visualization-mapping.ores") ?: return emptyMap()
        return mappingSection.getKeys(false).mapNotNull { original ->
            try {
                val originalMaterial = Material.valueOf(original.uppercase())
                val displayMaterial = Material.valueOf(mappingSection.getString(original, "OAK_FENCE")!!.uppercase())
                originalMaterial to displayMaterial
            } catch (_: IllegalArgumentException) {
                plugin.logger.warning("Invalid material name in visualization-mapping: $original")
                null
            }
        }.toMap()
    }

    fun getDefaultVisualMarker(): Material {
        return try {
            Material.valueOf(plugin.config.getString("visualization-mapping.default-marker", "OAK_FENCE")!!.uppercase())
        } catch (_: IllegalArgumentException) {
            Material.OAK_FENCE
        }
    }

    fun getMinStoneForAnalysis(): Int = plugin.config.getInt("thresholds.min-blocks-for-analysis.stone", 100)
    fun getMinTotalForAnalysis(): Int = plugin.config.getInt("thresholds.min-blocks-for-analysis.total", 500)
    fun getHighValueRatioThreshold(): Double = plugin.config.getDouble("thresholds.high-value-ratio", 0.02)
    fun getCommonOreRatioThreshold(oreName: String): Double = plugin.config.getDouble("thresholds.common-ore-ratios.${oreName.uppercase()}", 0.1)
    fun getTunnelVarianceThreshold(): Double = plugin.config.getDouble("thresholds.tunnel-variance", 2.0)
    fun getSuspiciousSpeedThreshold(): Double = plugin.config.getDouble("thresholds.suspicious-speed", 5.0)
    fun getYLevelConcentrationRatio(): Double = plugin.config.getDouble("thresholds.y-level-concentration-ratio", 0.4)
    fun getTorchCheckYLevel(): Int = plugin.config.getInt("thresholds.torch-usage.check-below-y", 40)
    fun getTorchMinBlocks(): Int = plugin.config.getInt("thresholds.torch-usage.min-blocks-for-check", 500)
    fun getTorchSuspiciousRatio(): Double = plugin.config.getDouble("thresholds.torch-usage.suspicious-ratio", 200.0)
    fun getMiningPurityWindow(): Int = plugin.config.getInt("thresholds.mining-purity.check-window-before-ore", 50)
    fun getMiningPuritySuspiciousRatio(): Double = plugin.config.getDouble("thresholds.mining-purity.suspicious-purity-ratio", 0.95)
    fun getInitialDiscoverySuspiciousTime(): Long = plugin.config.getLong("thresholds.initial-discovery.suspicious-time-seconds", 300)
    fun getPathEfficiencySuspiciousRatio(): Double = plugin.config.getDouble("thresholds.path-efficiency.suspicious-efficiency-ratio", 1.5)
    fun getSuspiciousThreshold(): Double = plugin.config.getDouble("thresholds.suspicion-levels.suspicious", 40.0)
    fun getDangerousThreshold(): Double = plugin.config.getDouble("thresholds.suspicion-levels.dangerous", 70.0)


    fun getSuspiciousYRanges(): List<IntRange> {
        return plugin.config.getStringList("thresholds.suspicious-y-levels").mapNotNull {
            try {
                val parts = it.split("&&").map { part -> part.trim() }
                val lowerBound = parts.find { p -> p.contains('>') }?.split('>')?.get(1)?.trim()?.toInt()
                val upperBound = parts.find { p -> p.contains('<') }?.split('<')?.get(1)?.trim()?.toInt()

                if (lowerBound != null && upperBound != null) {
                    (lowerBound + 1) until upperBound
                } else {
                    null
                }
            } catch (_: Exception) {
                plugin.logger.warning("Invalid Y-level range format in config.yml: $it")
                null
            }
        }
    }
}