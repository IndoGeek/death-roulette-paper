package com.indogeek.deathroulette.config

import java.io.IOException
import java.nio.file.Files
import java.util.Properties
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin

/**
 * User facing settings, stored as `plugins/DeathRoulette/config.yml`.
 *
 * The file is created from the bundled template on first start. Missing keys fall back to that
 * template, so a plugin update never leaves an incomplete configuration behind. Call [load] again,
 * through `/roulette reload`, to pick up manual edits.
 */
class DeathRouletteConfig(private val plugin: JavaPlugin) {

    var enabled: Boolean = true
        private set

    var intervalDays: Long = 10L
        private set

    var playerChance: Double = 30.0
        private set

    var mobSearchRadius: Double = 32.0
        private set

    var allowNonOperators: Boolean = false
        private set

    var allowPassiveMobs: Boolean = true
        private set

    var allowHostileMobs: Boolean = true
        private set

    var showStartTitle: Boolean = true
        private set

    var showCompletionTitle: Boolean = true
        private set

    var showResultActionBar: Boolean = true
        private set

    var showStartParticles: Boolean = true
        private set

    var playStartSound: Boolean = true
        private set

    var playCountdownSound: Boolean = true
        private set

    var playPlayerDeathSound: Boolean = true
        private set

    var playMobDeathSound: Boolean = true
        private set

    /** Reads the configuration from disk, falling back to the bundled defaults where needed. */
    fun load() {
        migrateLegacyFile()
        plugin.saveDefaultConfig()
        plugin.reloadConfig()

        val config = plugin.config
        bundledDefaults()?.let(config::setDefaults)

        enabled = config.getBoolean(ENABLED, enabled)
        intervalDays =
            config.long(INTERVAL_DAYS, intervalDays, MIN_INTERVAL_DAYS, MAX_INTERVAL_DAYS)
        playerChance = config.double(PLAYER_CHANCE, playerChance, MIN_CHANCE, MAX_CHANCE)
        mobSearchRadius = config.double(MOB_SEARCH_RADIUS, mobSearchRadius, MIN_RADIUS, MAX_RADIUS)

        allowNonOperators = config.getBoolean(ALLOW_NON_OPERATORS, allowNonOperators)
        allowPassiveMobs = config.getBoolean(PASSIVE_MOBS, allowPassiveMobs)
        allowHostileMobs = config.getBoolean(HOSTILE_MOBS, allowHostileMobs)

        showStartTitle = config.getBoolean(START_TITLE, showStartTitle)
        showCompletionTitle = config.getBoolean(COMPLETION_TITLE, showCompletionTitle)
        showResultActionBar = config.getBoolean(RESULT_ACTION_BAR, showResultActionBar)
        showStartParticles = config.getBoolean(START_PARTICLES, showStartParticles)

        playStartSound = config.getBoolean(START_SOUND, playStartSound)
        playCountdownSound = config.getBoolean(COUNTDOWN_SOUND, playCountdownSound)
        playPlayerDeathSound = config.getBoolean(PLAYER_DEATH_SOUND, playPlayerDeathSound)
        playMobDeathSound = config.getBoolean(MOB_DEATH_SOUND, playMobDeathSound)
    }

    /** True when [op] or the configuration lets every player run the commands. */
    fun canBypassPermissions(op: Boolean): Boolean = op || allowNonOperators

    /** Loads the template shipped inside the jar so missing keys still resolve. */
    private fun bundledDefaults(): YamlConfiguration? =
        plugin.getResource(FILE_NAME)?.bufferedReader()?.use(YamlConfiguration::loadConfiguration)

    /**
     * Converts a pre-YAML `deathroulette.properties` into `config.yml` exactly once, then removes
     * the old file so the migration cannot run twice.
     */
    private fun migrateLegacyFile() {
        val legacy = plugin.dataPath.resolve(LEGACY_FILE_NAME)
        if (Files.notExists(legacy)) {
            return
        }
        val target = plugin.dataPath.resolve(FILE_NAME)
        try {
            if (Files.notExists(target)) {
                val properties = Properties()
                Files.newInputStream(legacy).use(properties::load)

                val migrated = bundledDefaults() ?: YamlConfiguration()
                var moved = 0
                for ((legacyKey, path) in LEGACY_KEYS) {
                    val raw = properties.getProperty(legacyKey)?.trim() ?: continue
                    migrated.set(
                        path,
                        raw.toBooleanStrictOrNull()
                            ?: raw.toLongOrNull()
                            ?: raw.toDoubleOrNull()
                            ?: continue,
                    )
                    moved++
                }
                migrated.save(target.toFile())
                plugin.componentLogger.info(
                    "Migrated {} setting(s) from {} to {}.",
                    moved,
                    LEGACY_FILE_NAME,
                    FILE_NAME,
                )
            }
            Files.deleteIfExists(legacy)
        } catch (exception: IOException) {
            plugin.componentLogger.warn(
                "Could not migrate {}; it was left in place.",
                LEGACY_FILE_NAME,
                exception,
            )
        }
    }

    private fun org.bukkit.configuration.file.FileConfiguration.long(
        path: String,
        fallback: Long,
        min: Long,
        max: Long,
    ): Long {
        val value = getLong(path, fallback)
        if (value in min..max) {
            return value
        }
        warnOutOfRange(path, value, fallback)
        return fallback
    }

    private fun org.bukkit.configuration.file.FileConfiguration.double(
        path: String,
        fallback: Double,
        min: Double,
        max: Double,
    ): Double {
        val value = getDouble(path, fallback)
        if (value.isFinite() && value in min..max) {
            return value
        }
        warnOutOfRange(path, value, fallback)
        return fallback
    }

    private fun warnOutOfRange(path: String, value: Any, fallback: Any) {
        plugin.componentLogger.warn("'{}' is out of range ({}); using {}.", path, value, fallback)
    }

    private companion object {
        const val FILE_NAME = "config.yml"
        const val LEGACY_FILE_NAME = "deathroulette.properties"

        const val ENABLED = "enabled"
        const val INTERVAL_DAYS = "interval-days"
        const val PLAYER_CHANCE = "player-chance"
        const val MOB_SEARCH_RADIUS = "mob-search-radius"
        const val PASSIVE_MOBS = "mobs.passive"
        const val HOSTILE_MOBS = "mobs.hostile"
        const val ALLOW_NON_OPERATORS = "commands.allow-non-operators"
        const val START_TITLE = "titles.start"
        const val COMPLETION_TITLE = "titles.completion"
        const val RESULT_ACTION_BAR = "effects.result-action-bar"
        const val START_PARTICLES = "effects.start-particles"
        const val START_SOUND = "sounds.start"
        const val COUNTDOWN_SOUND = "sounds.countdown"
        const val PLAYER_DEATH_SOUND = "sounds.player-death"
        const val MOB_DEATH_SOUND = "sounds.mob-death"

        const val MIN_INTERVAL_DAYS = 1L
        const val MAX_INTERVAL_DAYS = 1_000_000L
        const val MIN_CHANCE = 0.0
        const val MAX_CHANCE = 100.0
        const val MIN_RADIUS = 1.0
        const val MAX_RADIUS = 128.0

        /** Maps every key of the retired properties file onto its `config.yml` path. */
        val LEGACY_KEYS =
            listOf(
                "enabled" to ENABLED,
                "roulette_interval_days" to INTERVAL_DAYS,
                "player_chance" to PLAYER_CHANCE,
                "mob_search_radius" to MOB_SEARCH_RADIUS,
                "allow_passive_mobs" to PASSIVE_MOBS,
                "allow_hostile_mobs" to HOSTILE_MOBS,
                "allow_non_operators" to ALLOW_NON_OPERATORS,
                "show_start_title" to START_TITLE,
                "show_completion_title" to COMPLETION_TITLE,
                "show_result_actionbar" to RESULT_ACTION_BAR,
                "show_start_particles" to START_PARTICLES,
                "play_start_sound" to START_SOUND,
                "play_countdown_sound" to COUNTDOWN_SOUND,
                "play_player_death_sound" to PLAYER_DEATH_SOUND,
                "play_mob_death_sound" to MOB_DEATH_SOUND,
            )
    }
}
