package com.nitg3n.clairvoyant

import com.nitg3n.clairvoyant.commands.CommandManager
import com.nitg3n.clairvoyant.listeners.BlockBreakListener
import com.nitg3n.clairvoyant.listeners.InteractionListener
import com.nitg3n.clairvoyant.listeners.PlayerMoveListener
import com.nitg3n.clairvoyant.listeners.PlayerQuitListener
import com.nitg3n.clairvoyant.services.ConfigManager
import com.nitg3n.clairvoyant.services.DatabaseManager
import com.nitg3n.clairvoyant.services.HeuristicsEngine
import com.nitg3n.clairvoyant.services.VisualizationManager
import org.bukkit.plugin.java.JavaPlugin

/**
 * Main class of the Clairvoyant plugin.
 * Initializes and manages all services, listeners, and commands.
 */
class Clairvoyant : JavaPlugin() {

    companion object {
        lateinit var instance: Clairvoyant
            private set
    }

    private lateinit var configManager: ConfigManager
    private lateinit var databaseManager: DatabaseManager
    private lateinit var visualizationManager: VisualizationManager
    private lateinit var heuristicsEngine: HeuristicsEngine
    private lateinit var commandManager: CommandManager

    /**
     * Called when the plugin is enabled.
     */
    override fun onEnable() {
        instance = this
        logger.info("Clairvoyant is enabling...")

        // Initialize services
        configManager = ConfigManager(this)
        databaseManager = DatabaseManager(this)
        heuristicsEngine = HeuristicsEngine(databaseManager, configManager)
        visualizationManager = VisualizationManager(this, databaseManager, configManager)

        // Register event listeners
        server.pluginManager.registerEvents(BlockBreakListener(databaseManager), this)
        server.pluginManager.registerEvents(InteractionListener(databaseManager, visualizationManager), this)
        server.pluginManager.registerEvents(PlayerMoveListener(databaseManager, configManager), this)
        server.pluginManager.registerEvents(PlayerQuitListener(visualizationManager), this)

        // Register commands
        commandManager = CommandManager(this, visualizationManager, databaseManager, heuristicsEngine)
        getCommand("clairvoyant")?.setExecutor(commandManager)
        getCommand("clairvoyant")?.tabCompleter = commandManager

        logger.info("Clairvoyant has been enabled.")
    }

    /**
     * Called when the plugin is disabled.
     */
    override fun onDisable() {
        // Cancel any running coroutine jobs to prevent memory leaks
        commandManager.cancelJobs()
        logger.info("Clairvoyant has been disabled.")
    }
}