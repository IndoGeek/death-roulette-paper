package com.indogeek.deathroulette.state

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin

/**
 * Persists the small amount of data a running roulette needs to survive a restart.
 *
 * The snapshot lives in `plugins/DeathRoulette/state.yml`, separate from the user facing
 * configuration, because it is written by the plugin and is not meant to be edited by hand.
 */
class DeathRouletteState(private val plugin: Plugin) {

    private val file: Path
        get() = plugin.dataPath.resolve(FILE_NAME)

    /** Returns the persisted snapshot, or [Snapshot.EMPTY] when there is nothing usable on disk. */
    fun read(): Snapshot {
        val path = file
        if (Files.notExists(path)) {
            return readLegacyFile() ?: Snapshot.EMPTY
        }
        val yaml = YamlConfiguration.loadConfiguration(path.toFile())
        return Snapshot(
            running = yaml.getBoolean(KEY_RUNNING, Snapshot.EMPTY.running),
            startWorldTime = yaml.getLong(KEY_START_WORLD_TIME, Snapshot.EMPTY.startWorldTime),
            lastProcessedDay =
                yaml.getLong(KEY_LAST_PROCESSED_DAY, Snapshot.EMPTY.lastProcessedDay),
        )
    }

    /** Overwrites the persisted snapshot with [snapshot]. */
    fun write(snapshot: Snapshot) {
        val yaml =
            YamlConfiguration().apply {
                options().setHeader(HEADER)
                set(KEY_RUNNING, snapshot.running)
                set(KEY_START_WORLD_TIME, snapshot.startWorldTime)
                set(KEY_LAST_PROCESSED_DAY, snapshot.lastProcessedDay)
            }
        try {
            val path = file
            Files.createDirectories(path.parent)
            yaml.save(path.toFile())
        } catch (exception: IOException) {
            plugin.componentLogger.warn("Could not write {}.", FILE_NAME, exception)
        }
    }

    /** Reads the retired properties based snapshot once, then removes it. */
    private fun readLegacyFile(): Snapshot? {
        val legacy = plugin.dataPath.resolve(LEGACY_FILE_NAME)
        if (Files.notExists(legacy)) {
            return null
        }
        return try {
            val properties = Properties()
            Files.newInputStream(legacy).use(properties::load)
            val snapshot =
                Snapshot(
                    running = properties.getProperty("running").toBoolean(),
                    startWorldTime = properties.getProperty("startWorldTime")?.toLongOrNull() ?: 0L,
                    lastProcessedDay =
                        properties.getProperty("lastProcessedDay")?.toLongOrNull() ?: -1L,
                )
            write(snapshot)
            Files.deleteIfExists(legacy)
            snapshot
        } catch (exception: IOException) {
            plugin.componentLogger.warn(
                "Could not read {}; starting fresh.",
                LEGACY_FILE_NAME,
                exception,
            )
            null
        }
    }

    /** Immutable snapshot of a roulette in progress. */
    data class Snapshot(
        val running: Boolean,
        val startWorldTime: Long,
        val lastProcessedDay: Long,
    ) {
        companion object {
            val EMPTY = Snapshot(running = false, startWorldTime = 0L, lastProcessedDay = -1L)
        }
    }

    private companion object {
        const val FILE_NAME = "state.yml"
        const val LEGACY_FILE_NAME = "state.properties"
        const val KEY_RUNNING = "running"
        const val KEY_START_WORLD_TIME = "start-world-time"
        const val KEY_LAST_PROCESSED_DAY = "last-processed-day"

        val HEADER =
            listOf("Death Roulette runtime state.", "Managed by the plugin - do not edit by hand.")
    }
}
