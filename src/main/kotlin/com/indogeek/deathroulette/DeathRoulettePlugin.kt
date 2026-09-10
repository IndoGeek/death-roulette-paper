package com.indogeek.deathroulette

import com.indogeek.deathroulette.command.DeathRouletteCommand
import com.indogeek.deathroulette.config.DeathRouletteConfig
import com.indogeek.deathroulette.game.DeathRouletteGame
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

/**
 * Entry point for the Death Roulette plugin.
 *
 * The plugin owns exactly one [DeathRouletteConfig] and one [DeathRouletteGame]; both are created
 * in [onEnable] and torn down in [onDisable] so nothing survives a plugin reload.
 */
class DeathRoulettePlugin : JavaPlugin() {

    private lateinit var configuration: DeathRouletteConfig
    private lateinit var game: DeathRouletteGame

    override fun onEnable() {
        configuration = DeathRouletteConfig(this).apply { load() }
        game = DeathRouletteGame(this, configuration)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            DeathRouletteCommand(configuration, game).register(event.registrar())
        }

        game.enable()
        componentLogger.info("Enabled ({}).", game.summary)
    }

    override fun onDisable() {
        if (this::game.isInitialized) {
            game.disable()
        }
        componentLogger.info("Disabled.")
    }
}
