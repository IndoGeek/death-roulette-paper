package com.indogeek.deathroulette.command

import com.indogeek.deathroulette.DeathRoulettePlugin
import com.indogeek.deathroulette.game.DeathRouletteGame
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin

/**
 * Registers and handles the /roulette command tree.
 *
 * Subcommands mirror the original Fabric mod:
 *  /roulette start, stop, status, reload, test <days>
 */
class DeathRouletteCommand(plugin: JavaPlugin) : CommandExecutor {

    private val game: DeathRouletteGame = DeathRouletteGame.getInstance(plugin)

    init {
        val command = plugin.getCommand("roulette")
        if (command != null) {
            command.setExecutor(this)
        } else {
            plugin.logger.warning("Failed to register /roulette command.")
        }
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!DeathRoulettePlugin.CONFIG.canUseCommands(sender.isOp())) {
            sender.sendMessage(
                Component.text("You don't have permission to use this command.")
                    .color(NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(usageMessage())
            return true
        }

        return when (args[0].lowercase()) {
            "start" -> handleStart(sender)
            "stop" -> handleStop(sender)
            "status" -> handleStatus(sender)
            "reload" -> handleReload()
            "test" -> {
                val days = args.getOrNull(1)?.toIntOrNull()
                if (days == null || days < 1) {
                    sender.sendMessage(
                        Component.text("Usage: /roulette test <days> (days must be at least 1)").color(NamedTextColor.RED))
                    true
                } else {
                    handleTest(sender, days)
                }
            }
            else -> {
                sender.sendMessage(usageMessage())
                true
            }
        }
    }

    private fun handleStart(sender: CommandSender): Boolean {
        if (game.isRunning()) {
            sender.sendMessage(
                Component.text("Death Roulette is already running.").color(NamedTextColor.RED))
            return false
        }
        game.start()
        sender.sendMessage(
            Component.text("Death Roulette started!").color(NamedTextColor.GREEN))
        return true
    }

    private fun handleStop(sender: CommandSender): Boolean {
        if (!game.isRunning()) {
            sender.sendMessage(
                Component.text("Death Roulette is not running.").color(NamedTextColor.RED))
            return false
        }
        game.stop()
        sender.sendMessage(
            Component.text("Death Roulette: ").color(NamedTextColor.GOLD)
                .append(Component.text("STOPPED").color(NamedTextColor.RED)))
        return true
    }

    private fun handleStatus(sender: CommandSender): Boolean {
        val status = when {
            game.isRunning() -> {
                val playerNames = game.getOnlinePlayers().joinToString(", ") { it.name }
                Component.text("Death Roulette: ").color(NamedTextColor.GOLD)
                    .append(Component.text("RUNNING").color(NamedTextColor.GREEN))
                    .append(Component.text(" | Day: ").color(NamedTextColor.GRAY))
                    .append(Component.text(game.getElapsedDays().toString()).color(NamedTextColor.YELLOW))
                    .append(Component.text(" | Ticks: ").color(NamedTextColor.GRAY))
                    .append(Component.text(game.getTicksIntoCurrentDay().toString()).color(NamedTextColor.YELLOW))
                    .append(Component.text(" | Players: ").color(NamedTextColor.GRAY))
                    .append(Component.text(game.getOnlinePlayerCount().toString()).color(NamedTextColor.YELLOW))
                    .append(Component.text(" | Online: ").color(NamedTextColor.GRAY))
                    .append(Component.text(playerNames).color(NamedTextColor.YELLOW))
            }
            else -> {
                Component.text("Death Roulette: ").color(NamedTextColor.GOLD)
                    .append(Component.text("STOPPED").color(NamedTextColor.RED))
            }
        }
        sender.sendMessage(status)
        return true
    }

    private fun handleReload(): Boolean {
        DeathRoulettePlugin.CONFIG.load()
        game.showActionBarAll(
            Component.text("Death Roulette configuration reloaded!")
                .color(NamedTextColor.AQUA))
        DeathRoulettePlugin.instance.logger.info("Configuration reloaded.")
        return true
    }

    private fun handleTest(sender: CommandSender, days: Int): Boolean {
        if (!game.isRunning()) {
            sender.sendMessage(
                Component.text("Death Roulette is not running.").color(NamedTextColor.RED))
            return true
        }
        game.fastForwardTest(days)
        sender.sendMessage(
            Component.text("Roulette test: ").color(NamedTextColor.AQUA)
                .append(
                    Component.text("fast-forwarded $days day(s).").color(NamedTextColor.YELLOW)))
        return true
    }

    private fun usageMessage(): Component {
        return Component.text("Death Roulette commands:").color(NamedTextColor.GOLD)
            .append(Component.newline())
            .append(Component.text("/roulette start").color(NamedTextColor.GRAY)
                .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Start the roulette").color(NamedTextColor.WHITE)))
            .append(Component.newline())
            .append(Component.text("/roulette stop").color(NamedTextColor.GRAY)
                .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Stop the roulette").color(NamedTextColor.WHITE)))
            .append(Component.newline())
            .append(Component.text("/roulette status").color(NamedTextColor.GRAY)
                .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Show current status").color(NamedTextColor.WHITE)))
            .append(Component.newline())
            .append(Component.text("/roulette reload").color(NamedTextColor.GRAY)
                .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Reload configuration").color(NamedTextColor.WHITE)))
            .append(Component.newline())
            .append(Component.text("/roulette test <days>").color(NamedTextColor.GRAY)
                .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("Simulate days").color(NamedTextColor.WHITE)))
    }
}