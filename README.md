# Immersive Deposit Scanner

Immersive Deposit Scanner records deposits revealed by valid Immersive Engineering core samples and Immersive Petroleum survey data, persists them server-side, synchronizes visible entries to clients, and displays owned runtime markers in JourneyMap.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.228 or newer 21.1.x
- Java 21
- JourneyMap `journeymap-neoforge-1.21.1-6.0.0-beta.85` or compatible newer
- Immersive Engineering `1.21.1-12.4.2-194` or compatible newer
- Immersive Petroleum `1.21.1-4.4.1-37` or compatible newer

JourneyMap, Immersive Engineering, and Immersive Petroleum are declared as required NeoForge dependencies. The mod jar does not include or shade those dependencies.

## Commands

- `/mst list`
- `/mst list ie`
- `/mst list ip`
- `/mst list currentchunk`
- `/mst scanheld`
- `/mst resync`
- `/mst remove currentchunk`
- `/mst clear mine`
- `/mst clear all` - operator level 2
- `/mst debug heldsample` - operator level 2

Alias: `/immersivedepositscanner`.

## Build

```powershell
.\gradlew.bat clean build
```

The runtime jar is produced under `build/libs` as `immersive_deposit_scanner-1.21.1-<version>.jar`.

For a verified Prism LAB install:

```powershell
.\install-mod.ps1
```

## Clean-Room Policy

This project does not copy code, textures, language files, class names, or assets from Mineral Tracker, JourneyMap, Immersive Engineering, or Immersive Petroleum. See [docs/CLEAN_ROOM.md](docs/CLEAN_ROOM.md).

