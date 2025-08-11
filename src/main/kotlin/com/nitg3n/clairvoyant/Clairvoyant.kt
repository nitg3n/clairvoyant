package com.nitg3n.clairvoyant

import com.nitg3n.clairvoyant.commands.CommandManager
import com.nitg3n.clairvoyant.listeners.BlockBreakListener
import com.nitg3n.clairvoyant.listeners.InteractionListener
import com.nitg3n.clairvoyant.listeners.PlayerMoveListener
import com.nitg3n.clairvoyant.services.ConfigManager
import com.nitg3n.clairvoyant.services.DatabaseManager
import com.nitg3n.clairvoyant.services.HeuristicsEngine
import com.nitg3n.clairvoyant.services.VisualizationManager
import org.bukkit.plugin.java.JavaPlugin

/**
 * 플러그인의 메인 클래스.
 * 모든 서비스와 리스너, 커맨드를 초기화하고 관리합니다.
 * (오류 수정: 생성자 인수 오류 해결)
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

    override fun onEnable() {
        instance = this
        logger.info("Clairvoyant is enabling...")

        // 서비스 초기화 (ConfigManager를 가장 먼저 초기화)
        configManager = ConfigManager(this)
        databaseManager = DatabaseManager(this)
        heuristicsEngine = HeuristicsEngine(databaseManager, configManager)
        // 오류 수정: 생성자에 맞는 올바른 인수를 전달합니다.
        visualizationManager = VisualizationManager(databaseManager, configManager)

        // 리스너 등록 (DatabaseManager를 직접 전달)
        server.pluginManager.registerEvents(BlockBreakListener(databaseManager), this)
        server.pluginManager.registerEvents(InteractionListener(databaseManager), this)
        server.pluginManager.registerEvents(PlayerMoveListener(databaseManager, configManager), this)

        // 커맨드 등록
        commandManager = CommandManager(this, visualizationManager, databaseManager, heuristicsEngine)
        getCommand("clairvoyant")?.setExecutor(commandManager)
        getCommand("clairvoyant")?.tabCompleter = commandManager

        logger.info("Clairvoyant has been enabled.")
    }

    override fun onDisable() {
        logger.info("Clairvoyant has been disabled.")
    }
}
