package com.indogeek.deathroulette.game

import com.indogeek.deathroulette.config.DeathRouletteConfig
import com.indogeek.deathroulette.message.Messages
import com.indogeek.deathroulette.state.DeathRouletteState
import kotlin.random.Random
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.sound.Sound as AdventureSound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.GameMode
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Enemy
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * Drives the roulette: a single repeating task advances the announcement, the countdown and the day
 * based scheduling, then picks and kills a player or a nearby mob.
 */
class DeathRouletteGame(private val plugin: Plugin, private val config: DeathRouletteConfig) {

    private val persistence = DeathRouletteState(plugin)

    /**
     * Titles, action bars and broadcasts go to players only. The console gets a single log line per
     * event instead, so nothing shows up twice in the server log.
     */
    private val audience: Audience
        get() = Audience.audience(plugin.server.onlinePlayers)

    private var task: BukkitTask? = null
    private var phase = Phase.IDLE
    private var phaseTicks = 0
    private var startWorldTime = 0L
    private var lastProcessedDay = -1L

    var isRunning: Boolean = false
        private set

    /** Days of world time that have passed since the current roulette began. */
    val elapsedDays: Long
        get() = (worldTime - startWorldTime) / TICKS_PER_DAY

    /** Ticks that have passed inside the current roulette day. */
    val ticksIntoCurrentDay: Long
        get() = (worldTime - startWorldTime) % TICKS_PER_DAY

    /** Every player currently connected. */
    val onlinePlayers: List<Player>
        get() = plugin.server.onlinePlayers.toList()

    /** One line summary of the current state, used for the single enable log line. */
    val summary: String
        get() = if (isRunning) "roulette running, day $elapsedDays" else "roulette stopped"

    private val worldTime: Long
        get() = plugin.server.worlds.firstOrNull()?.fullTime ?: 0L

    // ----------------------------------------------------------------- lifecycle

    /** Restores the previous snapshot, auto starts when configured, and schedules the ticker. */
    fun enable() {
        val snapshot = persistence.read()
        isRunning = snapshot.running
        startWorldTime = snapshot.startWorldTime
        lastProcessedDay = snapshot.lastProcessedDay
        resetPhase()
        reanchorIfRewound()

        if (config.enabled && !isRunning) {
            start()
        }

        task = plugin.server.scheduler.runTaskTimer(plugin, ::tick, 0L, 1L)
    }

    /** Cancels the ticker and persists the current snapshot. */
    fun disable() {
        task?.cancel()
        task = null
        save()
    }

    /** Begins a new roulette, discarding any progress from a previous one. */
    fun start() {
        startWorldTime = worldTime
        lastProcessedDay = 0L
        isRunning = true
        resetPhase()
        save()
    }

    /** Stops the roulette and clears any announcement or countdown in progress. */
    fun stop() {
        isRunning = false
        resetPhase()
        save()
    }

    /** Rewinds the clock by [days] world days so the next round triggers immediately. */
    fun fastForward(days: Int) {
        require(days >= 1) { "days must be at least 1" }
        startWorldTime -= days * TICKS_PER_DAY
        lastProcessedDay = elapsedDays
        beginRound(elapsedDays)
        save()
    }

    /** Sends [message] to every online player. */
    private fun broadcast(message: Component) {
        audience.sendMessage(Messages.prefixed(message))
    }

    // ---------------------------------------------------------------------- tick

    private fun tick() {
        if (!isRunning) {
            return
        }
        when (phase) {
            Phase.ANNOUNCING -> tickAnnouncement()
            Phase.COUNTING_DOWN -> tickCountdown()
            Phase.IDLE -> tickIdle()
        }
    }

    private fun tickIdle() {
        if (!config.enabled) {
            return
        }
        reanchorIfRewound()
        val currentDay = elapsedDays
        if (currentDay < lastProcessedDay + config.intervalDays) {
            return
        }
        lastProcessedDay = currentDay
        beginRound(currentDay)
        save()
    }

    /**
     * Restarts the schedule when the world clock is behind the recorded anchor.
     *
     * That happens after `/time set`, a world swap, or a restored snapshot that belongs to a world
     * which no longer exists. Without this the elapsed day count would go negative and rounds would
     * stop firing.
     */
    private fun reanchorIfRewound() {
        if (worldTime >= startWorldTime) {
            return
        }
        startWorldTime = worldTime
        lastProcessedDay = 0L
        save()
        plugin.componentLogger.warn("World time moved backwards; roulette schedule re-anchored.")
    }

    private fun tickAnnouncement() {
        if (--phaseTicks > 0) {
            return
        }
        phase = Phase.COUNTING_DOWN
        phaseTicks = COUNTDOWN_TICKS
        showCountdown(COUNTDOWN_TICKS / TICKS_PER_SECOND)
    }

    private fun tickCountdown() {
        if (--phaseTicks % TICKS_PER_SECOND != 0) {
            return
        }
        val secondsLeft = phaseTicks / TICKS_PER_SECOND
        if (secondsLeft > 0) {
            showCountdown(secondsLeft)
            return
        }
        resetPhase()
        audience.showTitle(
            Messages.title(
                Component.text("ROLL!", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                Component.text("Death Roulette has chosen...", NamedTextColor.RED),
            )
        )
        announce(roll())
    }

    private fun beginRound(day: Long) {
        if (phase != Phase.IDLE) {
            return
        }
        broadcast(
            Component.text("Day ", NamedTextColor.GRAY)
                .append(Component.text(day, NamedTextColor.YELLOW))
                .append(Component.text(" has begun.", NamedTextColor.GRAY))
        )
        phase = Phase.ANNOUNCING
        phaseTicks = ANNOUNCEMENT_TICKS

        if (config.showStartTitle) {
            audience.showTitle(
                Messages.title(
                    Component.text("DEATH ROULETTE", NamedTextColor.DARK_RED, TextDecoration.BOLD),
                    Component.text("The wheel is turning...", NamedTextColor.RED),
                )
            )
        }
        if (config.showStartParticles) {
            spawnStartParticles()
        }
        if (config.playStartSound) {
            playSound(Sound.ITEM_TOTEM_USE, pitch = 0.6f)
        }
    }

    private fun resetPhase() {
        phase = Phase.IDLE
        phaseTicks = 0
    }

    private fun save() {
        persistence.write(DeathRouletteState.Snapshot(isRunning, startWorldTime, lastProcessedDay))
    }

    // -------------------------------------------------------------------- roulette

    private fun roll(): RouletteResult {
        if (Random.nextDouble(MAX_CHANCE) < config.playerChance) {
            val player = randomPlayer() ?: return RouletteResult.NoPlayer
            player.health = 0.0
            return RouletteResult.PlayerKilled(player.name)
        }
        val mob = randomNearbyMob() ?: return RouletteResult.NoMob
        val type = mob.type
        mob.health = 0.0
        return RouletteResult.MobKilled(type)
    }

    private fun randomPlayer(): Player? =
        onlinePlayers.filter { it.gameMode == GameMode.SURVIVAL && !it.isDead }.randomOrNull()

    private fun randomNearbyMob(): Mob? {
        val anchor = randomPlayer() ?: return null
        val radius = config.mobSearchRadius
        return anchor.world
            .getNearbyEntitiesByType(Mob::class.java, anchor.location, radius, radius, radius) {
                it.isValid && !it.isDead && it.isSelectable()
            }
            .randomOrNull()
    }

    private fun Mob.isSelectable(): Boolean =
        if (this is Enemy) config.allowHostileMobs else config.allowPassiveMobs

    private fun announce(result: RouletteResult) {
        when (result) {
            is RouletteResult.PlayerKilled -> {
                showCompletion(config.playPlayerDeathSound, Sound.ENTITY_ENDER_DRAGON_GROWL)
                showResult(
                    Component.text(result.name, NamedTextColor.YELLOW)
                        .append(Component.text(" was chosen by the wheel.", NamedTextColor.GRAY))
                )
                plugin.componentLogger.info("Day {}: killed player {}.", elapsedDays, result.name)
            }

            is RouletteResult.MobKilled -> {
                showCompletion(config.playMobDeathSound, Sound.ENTITY_GENERIC_DEATH)
                showResult(
                    Component.translatable(result.type)
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text(" was chosen by the wheel.", NamedTextColor.GRAY))
                )
                plugin.componentLogger.info("Day {}: killed {}.", elapsedDays, result.type.key)
            }

            RouletteResult.NoPlayer -> {
                showFailure("No eligible players were found.")
                plugin.componentLogger.info("Day {}: skipped, no eligible players.", elapsedDays)
            }

            RouletteResult.NoMob -> {
                showFailure("No eligible mobs were found nearby.")
                plugin.componentLogger.info(
                    "Day {}: skipped, no eligible mobs nearby.",
                    elapsedDays,
                )
            }
        }
    }

    // ---------------------------------------------------------------- presentation

    private fun showCountdown(seconds: Int) {
        audience.showTitle(
            Messages.title(
                Component.text(seconds, NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Prepare yourself...", NamedTextColor.GRAY),
            )
        )
        if (config.playCountdownSound) {
            playSound(
                Sound.BLOCK_NOTE_BLOCK_PLING,
                pitch = if (seconds <= URGENT_SECONDS) 0.7f else 1.0f,
            )
        }
    }

    private fun showCompletion(soundEnabled: Boolean, sound: Sound) {
        if (config.showCompletionTitle) {
            audience.showTitle(
                Messages.title(
                    Component.text("ROULETTE COMPLETE", NamedTextColor.GOLD, TextDecoration.BOLD)
                )
            )
        }
        if (soundEnabled) {
            playSound(sound)
        }
    }

    private fun showResult(message: Component) {
        if (config.showResultActionBar) {
            audience.sendActionBar(Messages.prefixed(message))
        }
    }

    private fun showFailure(reason: String) {
        audience.sendActionBar(
            Messages.prefixed(
                Component.text("Round skipped: ", NamedTextColor.RED)
                    .append(Component.text(reason, NamedTextColor.YELLOW))
            )
        )
    }

    private fun spawnStartParticles() {
        for (player in onlinePlayers) {
            player.world.spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                player.location.clone().add(0.0, 1.0, 0.0),
                PARTICLE_COUNT,
                PARTICLE_SPREAD,
                PARTICLE_HEIGHT,
                PARTICLE_SPREAD,
                PARTICLE_SPEED,
            )
        }
    }

    private fun playSound(sound: Sound, volume: Float = 1.0f, pitch: Float = 1.0f) {
        audience.playSound(AdventureSound.sound(sound, AdventureSound.Source.MASTER, volume, pitch))
    }

    private enum class Phase {
        IDLE,
        ANNOUNCING,
        COUNTING_DOWN,
    }

    companion object {
        private const val TICKS_PER_SECOND = 20
        private const val TICKS_PER_DAY = 24_000L
        private const val ANNOUNCEMENT_TICKS = 80
        private const val COUNTDOWN_TICKS = 200
        private const val URGENT_SECONDS = 3
        private const val MAX_CHANCE = 100.0
        private const val PARTICLE_COUNT = 40
        private const val PARTICLE_SPREAD = 0.8
        private const val PARTICLE_HEIGHT = 1.0
        private const val PARTICLE_SPEED = 0.2
    }
}
