package com.nitg3n.clairvoyant

import org.bukkit.plugin.java.JavaPlugin

class Clairvoyant : JavaPlugin() {

    companion object {
        lateinit var instance: Clairvoyant
            private set
    }

    override fun onEnable() {
        instance = this

        logger.info("Clairvoyant has been enabled.")
    }

    override fun onDisable() {
        logger.info("Clairvoyant has been disabled.")
    }
}
