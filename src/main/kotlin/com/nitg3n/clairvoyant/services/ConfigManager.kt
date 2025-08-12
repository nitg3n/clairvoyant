package com.nitg3n.clairvoyant.services

import com.nitg3n.clairvoyant.Clairvoyant
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration

/**
 * Manages loading and accessing settings from the config.yml file.
 */
class ConfigManager(private val plugin: Clairvoyant) {

    private val config: FileConfiguration

    init {
        // Save the default config if it doesn't exist, then load it.
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        config = plugin.config
    }

    // --- Getters for various configuration values ---

    fun getWeight(key: String): Double = config.getDouble("weights.$key", 0.0)

    fun getHighValueOres(): Set<String> = config.getStringList("ore-lists.high-value").toSet()
    fun getCommonOres(): Map<String, Set<String>> {
        val commonOresSection = config.getConfigurationSection("ore-lists.common") ?: return emptyMap()
        return commonOresSection.getKeys(false).associateWith { oreName ->
            commonOresSection.getStringList(oreName).toSet()
        }
    }
    fun getStoneTypes(): Set<String> = config.getStringList("ore-lists.stones").toSet()
    fun getIgnorableInteractions(): Set<String> = config.getStringList("ore-lists.ignorable-interactions").toSet()

    fun getVisualizationMapping(): Map<Material, Material> {
        val mappingSection = config.getConfigurationSection("visualization-mapping.ores") ?: return emptyMap()
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
            Material.valueOf(config.getString("visualization-mapping.default-marker", "OAK_FENCE")!!.uppercase())
        } catch (_: IllegalArgumentException) {
            Material.OAK_FENCE
        }
    }

    fun getMinStoneForAnalysis(): Int = config.getInt("thresholds.min-blocks-for-analysis.stone", 100)
    fun getMinTotalForAnalysis(): Int = config.getInt("thresholds.min-blocks-for-analysis.total", 500)
    fun getHighValueRatioThreshold(): Double = config.getDouble("thresholds.high-value-ratio", 0.02)
    fun getCommonOreRatioThreshold(oreName: String): Double = config.getDouble("thresholds.common-ore-ratios.${oreName.uppercase()}", 0.1)
    fun getTunnelVarianceThreshold(): Double = config.getDouble("thresholds.tunnel-variance", 2.0)
    fun getSuspiciousSpeedThreshold(): Double = config.getDouble("thresholds.suspicious-speed", 5.0)
    fun getYLevelConcentrationRatio(): Double = config.getDouble("thresholds.y-level-concentration-ratio", 0.4)
    fun getTorchCheckYLevel(): Int = config.getInt("thresholds.torch-usage.check-below-y", 40)
    fun getTorchMinBlocks(): Int = config.getInt("thresholds.torch-usage.min-blocks-for-check", 500)
    fun getTorchSuspiciousRatio(): Double = config.getDouble("thresholds.torch-usage.suspicious-ratio", 200.0)
    fun getMiningPurityWindow(): Int = config.getInt("thresholds.mining-purity.check-window-before-ore", 50)
    fun getMiningPuritySuspiciousRatio(): Double = config.getDouble("thresholds.mining-purity.suspicious-purity-ratio", 0.95)
    fun getInitialDiscoverySuspiciousTime(): Long = config.getLong("thresholds.initial-discovery.suspicious-time-seconds", 300)
    fun getPathEfficiencySuspiciousRatio(): Double = config.getDouble("thresholds.path-efficiency.suspicious-efficiency-ratio", 1.5)

    fun getSuspiciousYRanges(): List<IntRange> {
        return config.getStringList("thresholds.suspicious-y-levels").mapNotNull {
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