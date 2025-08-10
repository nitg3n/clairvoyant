package com.nitg3n.clairvoyant

import com.nitg3n.clairvoyant.commands.CommandManager
import com.nitg3n.clairvoyant.listeners.BlockBreakListener
import com.nitg3n.clairvoyant.listeners.InteractionListener
import com.nitg3n.clairvoyant.listeners.PlayerMoveListener
import com.nitg3n.clairvoyant.services.DatabaseManager
import com.nitg3n.clairvoyant.services.LogProcessor
import com.nitg3n.clairvoyant.services.VisualizationManager
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class Clairvoyant : JavaPlugin() {

    companion object {
        lateinit var instance: Clairvoyant
            private set
    }

    lateinit var visualizationManager: VisualizationManager
        private set

    override fun onEnable() {
        instance = this

        logger.info("Initializing database...")
        DatabaseManager.init(this)

        logger.info("Starting LogProcessor...")
        LogProcessor.start(this)

        visualizationManager = VisualizationManager(this)

        logger.info("Registering event listeners...")
        server.pluginManager.registerEvents(BlockBreakListener, this)
        server.pluginManager.registerEvents(PlayerMoveListener, this)
        server.pluginManager.registerEvents(InteractionListener(this), this)

        logger.info("Registering commands...")
        val lifecycleManager = this.lifecycleManager
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            CommandManager.registerCommands(this, event.registrar())
        }

        logger.info("Clairvoyant has been enabled successfully.")
    }

    override fun onDisable() {
        logger.info("Stopping LogProcessor...")
        LogProcessor.stop()

        logger.info("Clearing all visualizations...")
        visualizationManager.clearAllVisualizations()

        logger.info("Closing database connection pool...")
        DatabaseManager.close()

        logger.info("Clairvoyant has been disabled.")
    }
}