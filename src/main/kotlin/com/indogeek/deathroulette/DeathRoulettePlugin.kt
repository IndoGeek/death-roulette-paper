package com.indogeek.deathroulette

import com.indogeek.deathroulette.command.DeathRouletteCommand
import com.indogeek.deathroulette.config.DeathRouletteConfig
import com.indogeek.deathroulette.game.DeathRouletteGame
import org.bukkit.plugin.java.JavaPlugin

class DeathRoulettePlugin : JavaPlugin() {

    override fun onEnable() {
        instance = this

        // Load configuration first.
        CONFIG = DeathRouletteConfig(this)
        CONFIG.load()

        // Register the /roulette command.
        DeathRouletteCommand(this)

        val game = DeathRouletteGame.getInstance(this)

        // Restore any previously running roulette.
        game.loadState()

        // Auto-start if the configuration wants it.
        if (CONFIG.isEnabled() && !game.isRunning()) {
            game.start()
            logger.info("Death Roulette has been enabled")
        }

        logger.info("[Death Roulette] Plugin enabled!")
    }

    override fun onDisable() {
        DeathRouletteGame.getInstance(this).saveState()
        logger.info("[Death Roulette] Plugin disabled!")
    }

    companion object {
        const val MOD_ID = "deathroulette"
        lateinit var instance: DeathRoulettePlugin
        lateinit var CONFIG: DeathRouletteConfig
    }
}