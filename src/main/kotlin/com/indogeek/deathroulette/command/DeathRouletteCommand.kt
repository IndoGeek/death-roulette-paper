package com.indogeek.deathroulette.command

import com.indogeek.deathroulette.config.DeathRouletteConfig
import com.indogeek.deathroulette.game.DeathRouletteGame
import com.indogeek.deathroulette.message.Messages
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

/**
 * Registers `/roulette` and its subcommands on Paper's Brigadier dispatcher.
 *
 * Every subcommand is gated by its own `deathroulette.command.*` permission. Setting
 * `commands.allow-non-operators` in the configuration bypasses those checks entirely.
 *
 * Handlers only reply to the sender and never write to the log, so running a command from the
 * console produces a single line. Paper's own `log-admin-commands` setting covers the audit trail
 * for commands issued by players.
 */
class DeathRouletteCommand(
    private val config: DeathRouletteConfig,
    private val game: DeathRouletteGame,
) {

    /** Adds the command tree to [commands]. */
    fun register(commands: Commands) {
        val root =
            Commands.literal(ROOT)
                .requires { it.permitted(PERMISSION_ROOT) }
                .executes { context ->
                    context.reply(usage())
                    SUCCESS
                }
                .then(literal("start", PERMISSION_START, ::start))
                .then(literal("stop", PERMISSION_STOP, ::stop))
                .then(literal("status", PERMISSION_STATUS, ::status))
                .then(literal("reload", PERMISSION_RELOAD, ::reload))
                .then(
                    Commands.literal("test")
                        .requires { it.permitted(PERMISSION_TEST) }
                        .then(
                            Commands.argument(
                                    ARG_DAYS,
                                    IntegerArgumentType.integer(MIN_DAYS, MAX_DAYS),
                                )
                                .executes { context ->
                                    test(context, IntegerArgumentType.getInteger(context, ARG_DAYS))
                                }
                        )
                )
                .build()

        commands.register(root, DESCRIPTION, listOf(ALIAS))
    }

    private fun literal(
        name: String,
        permission: String,
        action: (CommandContext<CommandSourceStack>) -> Int,
    ): ArgumentBuilder<CommandSourceStack, *> =
        Commands.literal(name).requires { it.permitted(permission) }.executes(action)

    // ------------------------------------------------------------------ subcommands

    private fun start(context: CommandContext<CommandSourceStack>): Int {
        if (game.isRunning) {
            context.reply(Messages.error("Death Roulette is already running."))
            return FAILURE
        }
        game.start()
        context.reply(Messages.success("Death Roulette started."))
        return SUCCESS
    }

    private fun stop(context: CommandContext<CommandSourceStack>): Int {
        if (!game.isRunning) {
            context.reply(Messages.error("Death Roulette is not running."))
            return FAILURE
        }
        game.stop()
        context.reply(Messages.success("Death Roulette stopped."))
        return SUCCESS
    }

    private fun status(context: CommandContext<CommandSourceStack>): Int {
        if (!game.isRunning) {
            context.reply(
                Component.text("Status: ", NamedTextColor.GOLD)
                    .append(Component.text("stopped", NamedTextColor.RED))
            )
            return SUCCESS
        }
        val players = game.onlinePlayers
        context.reply(
            Component.text("Status: ", NamedTextColor.GOLD)
                .append(Component.text("running", NamedTextColor.GREEN))
                .append(Messages.field("Day", game.elapsedDays))
                .append(Messages.field("Ticks", game.ticksIntoCurrentDay))
                .append(Messages.field("Players", players.size))
                .append(
                    Messages.field(
                        "Online",
                        players.joinToString(", ") { it.name }.ifEmpty { "none" },
                    )
                )
        )
        return SUCCESS
    }

    private fun reload(context: CommandContext<CommandSourceStack>): Int {
        config.load()
        context.reply(Messages.success("Configuration reloaded."))
        return SUCCESS
    }

    private fun test(context: CommandContext<CommandSourceStack>, days: Int): Int {
        if (!game.isRunning) {
            context.reply(Messages.error("Death Roulette is not running."))
            return FAILURE
        }
        game.fastForward(days)
        context.reply(Messages.success("Fast forwarded $days day(s)."))
        return SUCCESS
    }

    // ----------------------------------------------------------------------- helpers

    private fun CommandSourceStack.permitted(permission: String): Boolean =
        config.canBypassPermissions(sender.isOp) || sender.hasPermission(permission)

    private fun CommandContext<CommandSourceStack>.reply(message: Component) {
        source.sender.sendMessage(Messages.prefixed(message))
    }

    private fun usage(): Component {
        val builder =
            Component.text().append(Component.text("Available commands:", NamedTextColor.GOLD))
        USAGE.forEach { (command, description) ->
            builder
                .append(Component.newline())
                .append(Component.text("  $command", NamedTextColor.GRAY))
                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                .append(Component.text(description, NamedTextColor.WHITE))
        }
        return builder.build()
    }

    private companion object {
        const val ROOT = "roulette"
        const val ALIAS = "deathroulette"
        const val DESCRIPTION = "Control the Death Roulette."
        const val ARG_DAYS = "days"

        const val PERMISSION_ROOT = "deathroulette.command"
        const val PERMISSION_START = "deathroulette.command.start"
        const val PERMISSION_STOP = "deathroulette.command.stop"
        const val PERMISSION_STATUS = "deathroulette.command.status"
        const val PERMISSION_RELOAD = "deathroulette.command.reload"
        const val PERMISSION_TEST = "deathroulette.command.test"

        const val SUCCESS = 1
        const val FAILURE = 0
        const val MIN_DAYS = 1
        const val MAX_DAYS = 1_000_000

        val USAGE =
            listOf(
                "/roulette start" to "Start a roulette",
                "/roulette stop" to "Stop the roulette",
                "/roulette status" to "Show the current status",
                "/roulette reload" to "Reload the configuration",
                "/roulette test <days>" to "Skip ahead by a number of days",
            )
    }
}
