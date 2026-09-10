<p align="center">
  <img src="assets/death-roulette-banner.png" alt="Death Roulette Banner">
</p>

# Death Roulette

<p align="center">
  </a>
  <a href="https://github.com/IndoGeek/death-roulette/releases">
    <img src="https://img.shields.io/github/downloads/IndoGeek/death-roulette/total?style=flat-square&label=downloads" alt="Downloads">
  </a>
  <a href="https://github.com/IndoGeek/death-roulette/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/IndoGeek/death-roulette?style=flat-square" alt="License">
  </a>
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-62B47A?style=flat-square&logo=minecraft&logoColor=white" alt="Minecraft">
  <img src="https://img.shields.io/badge/Fabric-Loader-DBD0B4?style=flat-square&logo=fabric&logoColor=black" alt="Fabric">
</p>

<p align="center">
  <a href="https://github.com/IndoGeek/death-roulette/stargazers">
    <img src="https://img.shields.io/github/stars/IndoGeek/death-roulette?style=flat-square" alt="Stars">
  </a>
  <a href="https://github.com/IndoGeek/death-roulette/network/members">
    <img src="https://img.shields.io/github/forks/IndoGeek/death-roulette?style=flat-square" alt="Forks">
  </a>
</p>

# Features

*  Automatic roulette rounds based on Minecraft days
*  Random player or mob selection
*  Configurable chance of selecting a player
*  Separate control over passive and hostile mobs
*  Configurable mob search radius
*  Optional countdown and result sounds
*  Optional titles, action bar messages, and particles
*  Simple .properties configuration
*  Reload configuration without restarting the server
*  Built-in commands for starting, stopping, checking, and testing roulette rounds

# Commands

The main command is:
```
/roulette
```
Available subcommands:
```
/roulette start
/roulette stop
/roulette status
/roulette reload
/roulette test <days>
```
By default, roulette commands require operator permissions.

# Configuration

After the first launch, the plugin creates:

```
plugins/DeathRoulette/deathroulette.properties
```

The configuration controls things such as:

* How often roulette runs
* Player selection chance
* Mob search radius
* Passive/hostile mob selection
* Titles and action bar messages
* Particles
* Roulette and death sounds
* Command permissions

After changing the configuration, use:
```
/roulette reload
```
to apply it.

# Installation

Death Roulette is a Paper plugin built for Minecraft 1.20.1.

1. Install Paper 1.20.1.
2. Download the Death Roulette .jar.
3. Put it in the server’s plugins folder.
4. Start (or restart) the server.

The plugin is primarily intended for server-side use. The Kotlin runtime is
bundled inside the plugin jar, so no extra dependencies are required.

# Development

This project is a Kotlin Paper plugin built with Gradle.

Clone the repository and run:
```
./gradlew build
```
The compiled plugin (a self-contained "fat" jar including the relocated Kotlin
runtime) will be placed in:
```
build/libs/
```
# License

Death Roulette is released under the MIT License.

⸻

Made by IndoGeek.
