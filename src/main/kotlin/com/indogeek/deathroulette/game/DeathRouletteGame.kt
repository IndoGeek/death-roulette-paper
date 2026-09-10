package com.indogeek.deathroulette.game

import com.indogeek.deathroulette.DeathRoulettePlugin
import com.indogeek.deathroulette.state.DeathRouletteState
import java.time.Duration
import java.util.ArrayList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.GameMode
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Mob
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * Core game logic for Death Roulette.
 *
 * While the game is running, a single repeating Bukkit task (one per tick)
 * drives the announcement, countdown and periodic day-based roulette rounds,
 * mirroring the original Fabric mod's server end-tick handler.
 */
class DeathRouletteGame private constructor(private val plugin: Plugin) {

    private var running = false
    private var startWorldTime: Long = 0L
    private var lastProcessedDay: Long = -1L
    private var countdownTicks = 0
    private var startAnnouncementTicks = 0

    private val config: com.indogeek.deathroulette.config.DeathRouletteConfig
        get() = DeathRoulettePlugin.CONFIG

    init {
        object : org.bukkit.scheduler.BukkitRunnable() {
            override fun run() {
                tick()
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }

    // ---------------------------------------------------------------- control

    fun isRunning(): Boolean = running

    fun loadState() {
        val persisted = DeathRouletteState.get(plugin).read()
        running = persisted.running
        startWorldTime = persisted.startWorldTime
        lastProcessedDay = persisted.lastProcessedDay
        countdownTicks = 0
        startAnnouncementTicks = 0
        plugin.logger.info(
            "State restored: running=$running, startWorldTime=$startWorldTime, lastProcessedDay=$lastProcessedDay")
    }

    fun saveState() {
        DeathRouletteState.get(plugin)
            .write(running = running, startWorldTime = startWorldTime, lastProcessedDay = lastProcessedDay)
    }

    fun start() {
        startWorldTime = worldTime
        lastProcessedDay = 0L
        countdownTicks = 0
        startAnnouncementTicks = 0
        running = true
        saveState()
        plugin.logger.info("Roulette started.")
    }

    fun stop() {
        running = false
        countdownTicks = 0
        startAnnouncementTicks = 0
        saveState()
        plugin.logger.info("Roulette stopped.")
    }

    fun fastForwardTest(days: Int) {
        if (days < 1) return
        startWorldTime -= days * 24000L
        val currentDay = getElapsedDays()
        lastProcessedDay = currentDay
        plugin.logger.info("Test roulette fast-forwarded by $days day(s). Current day: $currentDay")
        processNewDay(currentDay)
    }

    // --------------------------------------------------------------- ticker

    private fun tick() {
        if (!running) return

        if (startAnnouncementTicks > 0) {
            startAnnouncementTicks--
            if (startAnnouncementTicks == 0) {
                countdownTicks = 200
                showTitleAll(
                    Component.text("10").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
            }
            return
        }

        if (countdownTicks > 0) {
            countdownTicks--
            if (countdownTicks % 20 == 0) {
                val seconds = countdownTicks / 20
                if (seconds > 0) {
                    showTitleAll(
                        Component.text(seconds.toString()).color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                        Component.text("Prepare yourself...").color(NamedTextColor.GRAY))
                    if (config.isPlayCountdownSound()) {
                        playSoundToEveryone(
                            Sound.BLOCK_NOTE_BLOCK_PLING,
                            1.0f,
                            if (seconds <= 3) 0.7f else 1.0f)
                    }
                } else {
                    countdownTicks = 0
                    showTitleAll(
                        Component.text("ROLL!").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
                        Component.text("Death Roulette has chosen...").color(NamedTextColor.RED))
                    handleRouletteResult(executeRoulette())
                }
            }
        }

        // Periodic day-based roulette.
        if (config.isEnabled()) {
            val currentDay = getElapsedDays()
            val interval = config.getRouletteIntervalDays()
            if (currentDay >= lastProcessedDay + interval) {
                lastProcessedDay = currentDay
                processNewDay(currentDay)
                saveState()
            }
        }
    }

    private fun handleRouletteResult(result: String) {
        when {
            result == "NO_PLAYER" -> {
                showActionBarAll(
                    Component.text("Death Roulette failed: ").color(NamedTextColor.RED)
                        .append(
                            Component.text("No active players found..").color(NamedTextColor.YELLOW)))
                plugin.logger.warning("Roulette failed: no active players found.")
            }
            result.startsWith("PLAYER:") -> {
                val playerName = result.removePrefix("PLAYER:")
                if (config.isShowCompletionTitle()) {
                    showTitleAll(
                        Component.text("ROULETTE COMPLETE").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                }
                if (config.isPlayPlayerDeathSound()) {
                    playSoundToEveryone(Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f)
                }
                if (config.isShowResultActionbar()) {
                    showActionBarAll(
                        Component.text("Death Roulette: ").color(NamedTextColor.GOLD)
                            .append(
                                Component.text("Player $playerName was killed.").color(NamedTextColor.YELLOW)))
                }
                plugin.logger.info("Roulette result: PLAYER | Selected: $playerName | Status: KILLED")
            }
            result.startsWith("MOB:") -> {
                val mobName = result.removePrefix("MOB:")
                if (config.isShowCompletionTitle()) {
                    showTitleAll(
                        Component.text("ROULETTE COMPLETE").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                }
                if (config.isPlayMobDeathSound()) {
                    playSoundToEveryone(Sound.ENTITY_GENERIC_DEATH, 1.0f, 1.0f)
                }
                if (config.isShowResultActionbar()) {
                    showActionBarAll(
                        Component.text("Death Roulette: ").color(NamedTextColor.GOLD)
                            .append(
                                Component.text("$mobName was killed.").color(NamedTextColor.YELLOW)))
                }
                plugin.logger.info("Roulette result: MOB | Selected: $mobName | Status: KILLED")
            }
            result == "NO_MOB" -> {
                showActionBarAll(
                    Component.text("Death Roulette failed: ").color(NamedTextColor.RED)
                        .append(
                            Component.text("No nearby mobs found..").color(NamedTextColor.YELLOW)))
                plugin.logger.warning("Roulette failed: no nearby mobs found.")
            }
        }
    }

    // ----------------------------------------------------------- presentation

    fun announce(message: String) {
        for (player in plugin.server.onlinePlayers) {
            player.sendMessage(Component.text(message))
        }
    }

    fun showActionBar(player: Player, message: Component) {
        player.sendActionBar(message)
    }

    fun showActionBarAll(message: Component) {
        for (player in plugin.server.onlinePlayers) {
            showActionBar(player, message)
        }
    }

    private fun showTitleAll(title: Component, subtitle: Component = Component.empty()) {
        val times = Title.Times.times(
            Duration.ofMillis(100),
            Duration.ofMillis(600),
            Duration.ofMillis(200))
        val completeTitle = Title.title(title, subtitle, times)
        for (player in plugin.server.onlinePlayers) {
            player.showTitle(completeTitle)
        }
    }

    fun playSoundToEveryone(sound: Sound, volume: Float, pitch: Float) {
        for (player in plugin.server.onlinePlayers) {
            player.playSound(player.location, sound, org.bukkit.SoundCategory.MASTER, volume, pitch)
        }
    }

    fun playStartEffect() {
        for (player in plugin.server.onlinePlayers) {
            val world = player.world
            val location = player.location.clone().add(0.0, 1.0, 0.0)
            world.spawnParticle(Particle.TOTEM, location, 40, 0.8, 1.0, 0.8, 0.2)
        }
    }

    fun startCountdown(): Boolean {
        if (!running) return false
        if (countdownTicks > 0 || startAnnouncementTicks > 0) return false
        startAnnouncementTicks = 80
        if (config.isShowStartTitle()) {
            showTitleAll(
                Component.text("DEATH ROULETTE").color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD),
                Component.text("The wheel is turning...").color(NamedTextColor.RED))
        }
        if (config.isShowStartParticles()) playStartEffect()
        if (config.isPlayStartSound()) playSoundToEveryone(Sound.ITEM_TOTEM_USE, 1.0f, 0.6f)
        plugin.logger.info("Roulette announcement started.")
        return true
    }

    // ------------------------------------------------------------ player/mob

    fun getOnlinePlayerCount(): Int = plugin.server.onlinePlayers.size

    fun getOnlinePlayers(): List<Player> = plugin.server.onlinePlayers.toList()

    fun getRandomPlayer(): Player? {
        val survivalPlayers = ArrayList<Player>()
        for (player in getOnlinePlayers()) {
            if (player.gameMode == GameMode.SURVIVAL && !player.isDead) {
                survivalPlayers.add(player)
            }
        }
        if (survivalPlayers.isEmpty()) return null
        return survivalPlayers.random()
    }

    fun getNearbyMobs(): List<Mob> {
        val centerPlayer = getRandomPlayer() ?: return ArrayList()
        val world = centerPlayer.world
        val radius = config.getMobSearchRadius()
        val location = centerPlayer.location
        return world
            .getNearbyEntities(location, radius, radius, radius)
            .filterIsInstance<Mob>()
            .filter { it.isValid && !it.isDead && isAllowedMob(it) }
    }

    private fun isAllowedMob(mob: Mob): Boolean {
        return if (mob is Monster) config.isAllowHostileMobs() else config.isAllowPassiveMobs()
    }

    fun getRandomNearbyMob(): Mob? {
        val mobs = getNearbyMobs()
        if (mobs.isEmpty()) return null
        return mobs.random()
    }

    fun killRandomPlayer(): Boolean {
        val player = getRandomPlayer() ?: return false
        player.health = 0.0
        return true
    }

    fun executeRoulette(): String {
        if (isPlayerResult()) {
            val player = getRandomPlayer()
            if (player == null || player.isDead) return "NO_PLAYER"
            val playerName = player.name
            player.health = 0.0
            return "PLAYER:$playerName"
        }

        val mob = getRandomNearbyMob()
        if (mob == null || !mob.isValid || mob.isDead) return "NO_MOB"
        val mobName = mob.type.name.lowercase().replace('_', ' ')
        mob.health = 0.0
        return "MOB:$mobName"
    }

    fun isPlayerResult(): Boolean {
        return Math.random() < (config.getPlayerChance() / 100.0)
    }

    // --------------------------------------------------------------- timing

    private val worldTime: Long
        get() = plugin.server.worlds.firstOrNull()?.fullTime ?: 0L

    fun getElapsedDays(): Long {
        return (worldTime - startWorldTime) / 24000L
    }

    fun getTicksIntoCurrentDay(): Long {
        return (worldTime - startWorldTime) % 24000L
    }

    private fun processNewDay(day: Long) {
        announce("Death Roulette: Day $day has begun")
        startCountdown()
    }

    companion object {
        private var instance: DeathRouletteGame? = null

        @Synchronized
        fun getInstance(plugin: Plugin): DeathRouletteGame {
            if (instance == null) {
                instance = DeathRouletteGame(plugin)
            }
            return instance!!
        }
    }
}