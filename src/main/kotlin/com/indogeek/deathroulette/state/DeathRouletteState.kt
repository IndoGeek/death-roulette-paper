package com.indogeek.deathroulette.state

import java.io.File
import org.bukkit.plugin.Plugin

/**
 * Persistent-state container for Death Roulette.
 *
 * Lightweight engagement data (the running flag, the world time the roulette
 * started at, and the last processed day) is kept in a simple properties file
 * under the plugin's data folder so a running roulette survives a server
 * restart.
 */
class DeathRouletteState private constructor(private val plugin: Plugin) {

    private fun stateFile(): File {
        plugin.dataFolder.mkdirs()
        return File(plugin.dataFolder, "state.properties")
    }

    /** Reads [running], [startWorldTime], [lastProcessedDay] from disk. */
    fun read(): PersistedState {
        val file = stateFile()
        if (!file.exists()) {
            return PersistedState(running = false, startWorldTime = 0L, lastProcessedDay = -1L)
        }
        return try {
            val props = java.util.Properties()
            file.inputStream().use { props.load(it) }
            PersistedState(
                running = props.getProperty("running", "false").toBoolean(),
                startWorldTime = props.getProperty("startWorldTime", "0").toLong(),
                lastProcessedDay = props.getProperty("lastProcessedDay", "-1").toLong())
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load Death Roulette state: ${e.message}")
            PersistedState(running = false, startWorldTime = 0L, lastProcessedDay = -1L)
        }
    }

    /** Writes [running], [startWorldTime], [lastProcessedDay] to disk. */
    fun write(running: Boolean, startWorldTime: Long, lastProcessedDay: Long) {
        try {
            val file = stateFile()
            val props = java.util.Properties()
            props.setProperty("running", running.toString())
            props.setProperty("startWorldTime", startWorldTime.toString())
            props.setProperty("lastProcessedDay", lastProcessedDay.toString())
            file.outputStream().use { props.store(it, "Death Roulette persistent state") }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save Death Roulette state: ${e.message}")
        }
    }

    companion object {
        fun get(plugin: Plugin): DeathRouletteState = DeathRouletteState(plugin)
    }
}

/** Immutable snapshot of the persisted Death Roulette state. */
data class PersistedState(
    val running: Boolean,
    val startWorldTime: Long,
    val lastProcessedDay: Long
)