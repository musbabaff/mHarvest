# mHarvest

Coal-fuelled auto-harvesting machines for Spigot/Paper servers. Players buy a machine, place it, feed it coal, and it harvests mature crops around it — storing them or selling them automatically through Vault.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Minecraft](https://img.shields.io/badge/Minecraft-1.20%2B-green)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

## Features

- **Auto-harvest area** around each machine, scaling with its level
- **3 upgrade levels** with configurable cost, range, speed and storage size
- **Built-in storage** opened by clicking the machine
- **Auto-sell** through Vault, with a configurable tax percentage
- **Coal fuel**: machines only run while they have fuel
- **Shared access**: machine owners can add friends
- **SQLite or MySQL** persistence
- Fully configurable menus, messages (`messages.yml`) and crop prices (`prices.yml`)

### Default levels

| Level | Upgrade cost | Harvest area | Items sold per cycle | Storage |
|---|---|---|---|---|
| 1 | — | 5×5 | 10 | 27 slots |
| 2 | 25,000 | 9×9 | 20 | 36 slots |
| 3 | 50,000 | 13×13 | 40 | 54 slots |

A new machine costs `10,000` from the market by default (`settings.machine-cost`).

## Requirements

- Spigot or Paper **1.20+**
- Java **21**
- [Vault](https://www.spigotmc.org/resources/vault.34315/) plus an economy plugin (required — mHarvest will not enable without Vault)

## Installation

1. Download the latest jar from [Releases](https://github.com/musbabaff/mHarvest/releases) or build it yourself (see below).
2. Drop it into your server's `plugins/` folder together with Vault.
3. Start the server once to generate `config.yml`, `messages.yml` and `prices.yml`.
4. Adjust the configuration and run `/mharvest reload`.

## Commands & permissions

Aliases: `/mh`, `/harvest`

| Command | Description | Permission |
|---|---|---|
| `/mharvest` or `/mharvest help` | Show help | `mharvest.use` |
| `/mharvest market` | Open the machine market | `mharvest.use` |
| `/mharvest reload` | Reload configuration files | `mharvest.admin` |

## Configuration

- **`config.yml`** — database (`SQLITE` or `MYSQL`), machine cost, tax, menu filler and per-level settings (`cost`, `range`, `speed`, `storage`)
- **`prices.yml`** — sell price per item, keyed by Minecraft material name (e.g. `WHEAT: 5.0`)
- **`messages.yml`** — all player-facing messages

## Building from source

```bash
git clone https://github.com/musbabaff/mHarvest.git
cd mHarvest
mvn package
```

The plugin jar is written to `target/`.

## License

[Apache License 2.0](LICENSE)
