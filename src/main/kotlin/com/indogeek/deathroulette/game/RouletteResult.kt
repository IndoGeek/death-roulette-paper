package com.indogeek.deathroulette.game

import org.bukkit.entity.EntityType

/** Outcome of a single roulette round. */
sealed interface RouletteResult {

    /** [name] was picked and killed. */
    data class PlayerKilled(val name: String) : RouletteResult

    /** A mob of [type] was picked and killed. */
    data class MobKilled(val type: EntityType) : RouletteResult

    /** Nobody was eligible, so the round was skipped. */
    data object NoPlayer : RouletteResult

    /** No eligible mob was in range, so the round was skipped. */
    data object NoMob : RouletteResult
}
