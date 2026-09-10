package com.indogeek.deathroulette.message

import java.time.Duration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title

/** Shared building blocks for every player facing message the plugin sends. */
object Messages {

    /** Chat prefix shown in front of all chat and action bar messages. */
    val PREFIX: Component =
        Component.text()
            .append(Component.text("[", NamedTextColor.DARK_GRAY))
            .append(Component.text("Death Roulette", NamedTextColor.GOLD))
            .append(Component.text("] ", NamedTextColor.DARK_GRAY))
            .build()

    private val TITLE_TIMES: Title.Times =
        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(600), Duration.ofMillis(200))

    /** Prepends [PREFIX] to [message]. */
    fun prefixed(message: Component): Component = PREFIX.append(message)

    /** Builds a title using the plugin's shared fade in, stay and fade out timings. */
    fun title(main: Component, subtitle: Component = Component.empty()): Title =
        Title.title(main, subtitle, TITLE_TIMES)

    /** Green confirmation text. */
    fun success(message: String): Component = Component.text(message, NamedTextColor.GREEN)

    /** Red failure text. */
    fun error(message: String): Component = Component.text(message, NamedTextColor.RED)

    /** A `| Label: value` fragment used to build single line status readouts. */
    fun field(label: String, value: Any): Component =
        Component.text(" | ", NamedTextColor.DARK_GRAY)
            .append(Component.text("$label: ", NamedTextColor.GRAY))
            .append(Component.text(value.toString(), NamedTextColor.YELLOW))
}
