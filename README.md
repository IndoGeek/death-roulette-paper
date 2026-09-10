<p align="center">
  <img src="assets/death-roulette-banner.png" alt="Death Roulette Banner">
</p>

# Death Roulette

<p align="center">
  <a href="https://github.com/IndoGeek/death-roulette/releases">
    <img src="https://img.shields.io/github/downloads/IndoGeek/death-roulette/total?style=flat-square&label=downloads" alt="Downloads">
  </a>
  <a href="https://github.com/IndoGeek/death-roulette/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/IndoGeek/death-roulette?style=flat-square" alt="License">
  </a>
  <img src="https://img.shields.io/badge/Minecraft-26.2-62B47A?style=flat-square&logo=minecraft&logoColor=white" alt="Minecraft">
  <img src="https://img.shields.io/badge/Paper-plugin-0288D1?style=flat-square" alt="Paper">
  <img src="https://img.shields.io/badge/Java-25-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Kotlin-2.4-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
</p>

<p align="center">
  <a href="https://github.com/IndoGeek/death-roulette/stargazers">
    <img src="https://img.shields.io/github/stars/IndoGeek/death-roulette?style=flat-square" alt="Stars">
  </a>
  <a href="https://github.com/IndoGeek/death-roulette/network/members">
    <img src="https://img.shields.io/github/forks/IndoGeek/death-roulette?style=flat-square" alt="Forks">
  </a>
</p>

Every so many in-game days the wheel spins: a random survival player or a mob near
them is chosen and killed. Rounds are announced with a title, a ten second
countdown, particles and sounds, and the whole thing is driven by a single
`config.yml`.

## Features

- Automatic rounds on a configurable Minecraft-day interval
- Weighted player-versus-mob selection
- Independent toggles for hostile and peaceful mobs
- Configurable mob search radius
- Optional titles, action bar results, particles and sounds
- Brigadier commands with tab completion and argument validation
- Per-subcommand permission nodes
- Runtime state survives a server restart
- Live configuration reload

## Requirements

| | |
|---|---|
| Server | Paper 26.2 |
| Java | 25 or newer |

## Installation

1. Download `DeathRoulette-<version>.jar` from the releases page.
2. Drop it into the server's `plugins/` folder.
3. Start the server.

The Kotlin runtime is shaded into the jar and relocated, so there is nothing else
to install.

## Commands

`/roulette` (alias `/deathroulette`)

| Command | Description |
|---|---|
| `/roulette` | Show the command list |
| `/roulette start` | Start a roulette |
| `/roulette stop` | Stop the roulette |
| `/roulette status` | Show the current status |
| `/roulette reload` | Reload the configuration |
| `/roulette test <days>` | Skip ahead by a number of days |

## Permissions

Every node defaults to operators only.

| Permission | Grants |
|---|---|
| `deathroulette.command` | Access to `/roulette` and, as a parent, every node below |
| `deathroulette.command.start` | `/roulette start` |
| `deathroulette.command.stop` | `/roulette stop` |
| `deathroulette.command.status` | `/roulette status` |
| `deathroulette.command.reload` | `/roulette reload` |
| `deathroulette.command.test` | `/roulette test` |

Setting `commands.allow-non-operators` to `true` in the configuration bypasses these
checks and lets every player run the commands.

## Configuration

The plugin writes `plugins/DeathRoulette/config.yml` from a bundled template on first
start. Keys missing from an older file fall back to that template, so an update never
leaves an incomplete configuration behind.

| Key | Default | Description |
|---|---|---|
| `enabled` | `true` | Schedule rounds automatically |
| `interval-days` | `10` | Minecraft days between rounds |
| `player-chance` | `30.0` | Percent chance of picking a player instead of a mob |
| `mob-search-radius` | `32.0` | Blocks searched around the anchor player |
| `mobs.passive` | `true` | Allow peaceful mobs to be picked |
| `mobs.hostile` | `true` | Allow hostile mobs to be picked |
| `commands.allow-non-operators` | `false` | Let every player run `/roulette` |
| `titles.start` | `true` | Title announcing a new round |
| `titles.completion` | `true` | Title announcing the result |
| `effects.result-action-bar` | `true` | Action bar result message |
| `effects.start-particles` | `true` | Totem particles when a round starts |
| `sounds.start` | `true` | Sound when a round starts |
| `sounds.countdown` | `true` | Note block tick during the countdown |
| `sounds.player-death` | `true` | Sound when a player is picked |
| `sounds.mob-death` | `true` | Sound when a mob is picked |

Run `/roulette reload` to apply manual edits without restarting.

Runtime progress is kept separately in `plugins/DeathRoulette/state.yml`. That file is
managed by the plugin and should not be edited.

### Upgrading from the properties format

Earlier builds used `deathroulette.properties` and `state.properties`. Both are converted
to their YAML equivalents automatically on first start and then removed, so existing
settings and any roulette in progress carry over untouched.

## Logging

The console gets exactly one line per event: one on enable, one on disable, one per
command, and one per round result. Titles, action bars and broadcasts are sent to players
only, so nothing is echoed twice into the server log. Commands issued by players are
covered by Paper's own `log-admin-commands` setting.

## Development

```sh
./gradlew build      # produce build/libs/DeathRoulette-<version>.jar
./gradlew runServer  # boot a Paper test server in run/
```

Toolchain versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).
Gradle provisions the required JDK automatically through the Foojay resolver.

Kotlin sources are formatted with [ktfmt](https://github.com/facebook/ktfmt) using
the Kotlin language style:

```sh
ktfmt --kotlinlang-style $(find src -name '*.kt')
```

Compiler warnings are treated as errors, so the build fails on any deprecated or
suspicious API use.

## License

Released under the MIT License. Made by IndoGeek.
