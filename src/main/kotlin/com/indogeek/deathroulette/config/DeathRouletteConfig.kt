package com.indogeek.deathroulette.config

import com.indogeek.deathroulette.DeathRoulettePlugin
import java.util.Properties
import org.bukkit.plugin.java.JavaPlugin

/**
 * Configuration for Death Roulette.
 *
 * Saved to config/deathroulette.properties inside the plugin's data folder.
 * Values are read once on load; use /roulette reload to re-read the file.
 */
class DeathRouletteConfig(private val plugin: JavaPlugin) {

    private val defaultEnabled = true
    private val defaultPlayerChance = 30.0
    private val defaultMobSearchRadius = 32.0
    private val defaultRouletteIntervalDays = 10L

    private val defaultAllowNonOperators = false
    private val defaultAllowPassiveMobs = true
    private val defaultAllowHostileMobs = true

    private val defaultShowStartTitle = true
    private val defaultShowCompletionTitle = true
    private val defaultShowResultActionbar = true
    private val defaultShowStartParticles = true

    private val defaultPlayStartSound = true
    private val defaultPlayCountdownSound = true
    private val defaultPlayPlayerDeathSound = true
    private val defaultPlayMobDeathSound = true

    private var enabled: Boolean = defaultEnabled
    private var playerChance: Double = defaultPlayerChance
    private var mobSearchRadius: Double = defaultMobSearchRadius
    private var rouletteIntervalDays: Long = defaultRouletteIntervalDays

    private var allowNonOperators: Boolean = defaultAllowNonOperators
    private var allowPassiveMobs: Boolean = defaultAllowPassiveMobs
    private var allowHostileMobs: Boolean = defaultAllowHostileMobs

    private var showStartTitle: Boolean = defaultShowStartTitle
    private var showCompletionTitle: Boolean = defaultShowCompletionTitle
    private var showResultActionbar: Boolean = defaultShowResultActionbar
    private var showStartParticles: Boolean = defaultShowStartParticles

    private var playStartSound: Boolean = defaultPlayStartSound
    private var playCountdownSound: Boolean = defaultPlayCountdownSound
    private var playPlayerDeathSound: Boolean = defaultPlayPlayerDeathSound
    private var playMobDeathSound: Boolean = defaultPlayMobDeathSound

    fun canUseCommands(op: Boolean): Boolean = op || allowNonOperators

    fun load() {
        val properties = Properties()
        val path = plugin.dataFolder.toPath().resolve("deathroulette.properties")

        if (!java.nio.file.Files.exists(path)) {
            setDefaults()
            save()
            return
        }

        try {
            java.nio.file.Files.newInputStream(path).use { input ->
                properties.load(input)
            }

            enabled = parseBoolean(properties, "enabled", defaultEnabled)

            playerChance = parseDouble(
                properties, "player_chance", defaultPlayerChance, 0.0, 100.0
            )

            mobSearchRadius = parseDouble(
                properties, "mob_search_radius", defaultMobSearchRadius, 1.0, 128.0
            )

            rouletteIntervalDays = parseLong(
                properties, "roulette_interval_days", defaultRouletteIntervalDays, 1L, 1000000L
            )

            allowNonOperators = parseBoolean(properties, "allow_non_operators", defaultAllowNonOperators)

            allowPassiveMobs = parseBoolean(properties, "allow_passive_mobs", defaultAllowPassiveMobs)

            allowHostileMobs = parseBoolean(properties, "allow_hostile_mobs", defaultAllowHostileMobs)

            showStartTitle = parseBoolean(properties, "show_start_title", defaultShowStartTitle)

            showCompletionTitle = parseBoolean(properties, "show_completion_title", defaultShowCompletionTitle)

            showResultActionbar = parseBoolean(properties, "show_result_actionbar", defaultShowResultActionbar)

            showStartParticles = parseBoolean(properties, "show_start_particles", defaultShowStartParticles)

            playStartSound = parseBoolean(properties, "play_start_sound", defaultPlayStartSound)

            playCountdownSound = parseBoolean(properties, "play_countdown_sound", defaultPlayCountdownSound)

            playPlayerDeathSound = parseBoolean(properties, "play_player_death_sound", defaultPlayPlayerDeathSound)

            playMobDeathSound = parseBoolean(properties, "play_mob_death_sound", defaultPlayMobDeathSound)

            save()
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load configuration: ${e.message}")
            setDefaults()
        }
    }

    fun save() {
        try {
            plugin.dataFolder.mkdirs()
            val path = plugin.dataFolder.toPath().resolve("deathroulette.properties")
            java.nio.file.Files.newBufferedWriter(path).use { writer ->
                writer.write("# ==========================================")
                writer.newLine()
                writer.write("# Death Roulette Configuration")
                writer.newLine()
                writer.write("# ==========================================")
                writer.newLine()
                writer.newLine()

                writer.write("# Master switch for automatic Death Roulette.")
                writer.newLine()
                writer.write("# true  = Death Roulette runs automatically.")
                writer.newLine()
                writer.write("# false = Death Roulette is disabled.")
                writer.newLine()
                writer.write("enabled=" + enabled)
                writer.newLine()
                writer.newLine()

                writer.write("# Number of Minecraft days between roulette events.")
                writer.newLine()
                writer.write("roulette_interval_days=" + rouletteIntervalDays)
                writer.newLine()
                writer.newLine()

                writer.write("# Chance of selecting a player instead of a mob.")
                writer.newLine()
                writer.write("# 0.0   = always mob")
                writer.newLine()
                writer.write("# 50.0  = 50% player / 50% mob")
                writer.newLine()
                writer.write("# 100.0 = always player")
                writer.newLine()
                writer.write("player_chance=" + playerChance)
                writer.newLine()
                writer.newLine()

                writer.write("# Radius used when searching for nearby mobs.")
                writer.newLine()
                writer.write("mob_search_radius=" + mobSearchRadius)
                writer.newLine()
                writer.newLine()

                writer.write("# ==========================================")
                writer.newLine()
                writer.write("# Command Permissions")
                writer.newLine()
                writer.write("# ==========================================")
                writer.newLine()
                writer.newLine()

                writer.write("# Allow non-operators to use /roulette commands.")
                writer.newLine()
                writer.write("# false = operators only")
                writer.newLine()
                writer.write("# true  = all players can use commands")
                writer.newLine()
                writer.write("allow_non_operators=" + allowNonOperators)
                writer.newLine()
                writer.newLine()

                writer.write("# ==========================================")
                writer.newLine()
                writer.write("# Mob Selection")
                writer.newLine()
                writer.write("# ==========================================")
                writer.newLine()
                writer.newLine()

                writer.write("# Allow passive/non-hostile mobs to be selected.")
                writer.newLine()
                writer.write("allow_passive_mobs=" + allowPassiveMobs)
                writer.newLine()
                writer.newLine()

                writer.write("# Allow hostile mobs to be selected.")
                writer.newLine()
                writer.write("allow_hostile_mobs=" + allowHostileMobs)
                writer.newLine()
                writer.newLine()

                writer.write("# ==========================================")
                writer.newLine()
                writer.write("# Visual Settings")
                writer.newLine()
                writer.write("# ==========================================")
                writer.newLine()
                writer.newLine()

                writer.write("# Show the \"DEATH ROULETTE STARTED\" title.")
                writer.newLine()
                writer.write("show_start_title=" + showStartTitle)
                writer.newLine()
                writer.newLine()

                writer.write("# Show the \"DEATH ROULETTE COMPLETE\" title.")
                writer.newLine()
                writer.write("show_completion_title=" + showCompletionTitle)
                writer.newLine()
                writer.newLine()

                writer.write("# Show the selected player/mob result in the action bar.")
                writer.newLine()
                writer.write("show_result_actionbar=" + showResultActionbar)
                writer.newLine()
                writer.newLine()

                writer.write("# Show Totem of Undying particles when roulette starts.")
                writer.newLine()
                writer.write("show_start_particles=" + showStartParticles)
                writer.newLine()
                writer.newLine()

                writer.write("# ==========================================")
                writer.newLine()
                writer.write("# Sound Settings")
                writer.newLine()
                writer.write("# ==========================================")
                writer.newLine()
                writer.newLine()

                writer.write("# Play the roulette-start sound.")
                writer.newLine()
                writer.write("play_start_sound=" + playStartSound)
                writer.newLine()
                writer.newLine()

                writer.write("# Play the note-block countdown sounds.")
                writer.newLine()
                writer.write("play_countdown_sound=" + playCountdownSound)
                writer.newLine()
                writer.newLine()

                writer.write("# Play the player death sound.")
                writer.newLine()
                writer.write("play_player_death_sound=" + playPlayerDeathSound)
                writer.newLine()
                writer.newLine()

                writer.write("# Play the mob death sound.")
                writer.newLine()
                writer.write("play_mob_death_sound=" + playMobDeathSound)
                writer.newLine()
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save configuration: ${e.message}")
        }
    }

    fun isEnabled(): Boolean = enabled
    fun getPlayerChance(): Double = playerChance
    fun getMobSearchRadius(): Double = mobSearchRadius
    fun getRouletteIntervalDays(): Long = rouletteIntervalDays
    fun isAllowNonOperators(): Boolean = allowNonOperators
    fun isShowStartTitle(): Boolean = showStartTitle
    fun isShowCompletionTitle(): Boolean = showCompletionTitle
    fun isShowResultActionbar(): Boolean = showResultActionbar
    fun isShowStartParticles(): Boolean = showStartParticles
    fun isPlayStartSound(): Boolean = playStartSound
    fun isPlayCountdownSound(): Boolean = playCountdownSound
    fun isPlayPlayerDeathSound(): Boolean = playPlayerDeathSound
    fun isPlayMobDeathSound(): Boolean = playMobDeathSound
    fun isAllowPassiveMobs(): Boolean = allowPassiveMobs
    fun isAllowHostileMobs(): Boolean = allowHostileMobs

    private fun setDefaults() {
        enabled = defaultEnabled
        playerChance = defaultPlayerChance
        mobSearchRadius = defaultMobSearchRadius
        rouletteIntervalDays = defaultRouletteIntervalDays

        allowNonOperators = defaultAllowNonOperators
        allowPassiveMobs = defaultAllowPassiveMobs
        allowHostileMobs = defaultAllowHostileMobs

        showStartTitle = defaultShowStartTitle
        showCompletionTitle = defaultShowCompletionTitle
        showResultActionbar = defaultShowResultActionbar
        showStartParticles = defaultShowStartParticles

        playStartSound = defaultPlayStartSound
        playCountdownSound = defaultPlayCountdownSound
        playPlayerDeathSound = defaultPlayPlayerDeathSound
        playMobDeathSound = defaultPlayMobDeathSound
    }

    private fun parseBoolean(properties: Properties, key: String, defaultValue: Boolean): Boolean {
        val value = properties.getProperty(key) ?: return defaultValue
        if (value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)) {
            return value.toBoolean()
        }
        plugin.logger.warning("Invalid $key: $value. Using default: $defaultValue")
        return defaultValue
    }

    private fun parseDouble(
        properties: Properties,
        key: String,
        defaultValue: Double,
        min: Double,
        max: Double
    ): Double {
        val value = properties.getProperty(key) ?: return defaultValue
        return try {
            val parsed = value.toDouble()
            if (parsed < min || parsed > max) {
                plugin.logger.warning("Invalid $key: $value. Using default: $defaultValue")
                defaultValue
            } else {
                parsed
            }
        } catch (e: NumberFormatException) {
            plugin.logger.warning("Invalid $key: $value. Using default: $defaultValue")
            defaultValue
        }
    }

    private fun parseLong(
        properties: Properties,
        key: String,
        defaultValue: Long,
        min: Long,
        max: Long
    ): Long {
        val value = properties.getProperty(key) ?: return defaultValue
        return try {
            val parsed = value.toLong()
            if (parsed < min || parsed > max) {
                plugin.logger.warning("Invalid $key: $value. Using default: $defaultValue")
                defaultValue
            } else {
                parsed
            }
        } catch (e: NumberFormatException) {
            plugin.logger.warning("Invalid $key: $value. Using default: $defaultValue")
            defaultValue
        }
    }
}