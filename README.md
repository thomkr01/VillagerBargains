# Villager Bargains

A Fabric mod that takes the luck out of villager and wandering trader prices. By default every
trade costs the cheapest price vanilla can produce. It can also make trades as expensive as
possible, or leave them vanilla. Optional settings pin the level of traded enchanted books and
the enchanting strength of traded gear.

## Features

- Cheapest, vanilla or most expensive trade prices, for every profession and the wandering trader.
- Optional fixed level for traded enchanted books and fixed strength for traded enchanted gear.
- Everything else stays vanilla: which trades appear, stock, XP, and reputation and Hero of the
  Village discounts.
- Settings change live with `/bargain` or Mod Menu, no restart needed.
- Players do not need the mod to join a server that has it. Fabric API is not required.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/).
2. Download the jar from [Releases](../../releases) and put it in your `mods/` folder.

On a server, only the server needs the mod.

## Usage

The mod works as soon as it is installed. Use `/bargain` to view or change the settings in game.

| Command | What it does |
|---------|--------------|
| `/bargain` | Shows the current settings. |
| `/bargain reload` | Re-reads the config file. |
| `/bargain price <minimum\|normal\|maximum>` | Sets `pricing`. |
| `/bargain books <minimum\|normal\|maximum>` | Sets `bookLevels`. |
| `/bargain gear <minimum\|normal\|maximum>` | Sets `gearStrength`. |
| `/bargain demand <vanilla\|pinned>` | Sets `vanillaDemand`. |
| `/bargain trader <vanilla\|pinned>` | Sets `vanillaWanderingTrader`. |
| `/bargain refresh [villagers\|traders\|<uuid>]` | Applies the current demand rule to existing offers now, instead of at the next restock. |

Commands need operator. With [LuckPerms](https://luckperms.net/) each one can be granted per
player or group through its `villagerbargains.command.<name>` node, for example:

```
/lp group mods permission set villagerbargains.command.price true
```

## Configuration

Change settings in game with [Mod Menu](https://modrinth.com/mod/modmenu), with the `/bargain`
command, or in `config/villagerbargains.json`. A change applies to trades villagers unlock after
it.

| Setting | Values | What it controls |
|---------|--------|------------------|
| `pricing` | `MINIMUM` *(default)*, `NORMAL`, `MAXIMUM` | Trade prices and the demand surcharge. |
| `bookLevels` | `MINIMUM`, `NORMAL` *(default)*, `MAXIMUM` | Level of traded enchanted books. |
| `gearStrength` | `MINIMUM`, `NORMAL` *(default)*, `MAXIMUM` | Enchanting strength of traded tools, weapons and armor. |
| `vanillaDemand` | `true`, `false` *(default)* | Keep the vanilla demand surcharge. |
| `vanillaWanderingTrader` | `true`, `false` *(default)* | Keep vanilla prices for wandering traders. |

## Contributing

Issues and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for the workflow and
[CHANGELOG.md](CHANGELOG.md) for what changed in each version. To build and test:

```bash
./gradlew build        # jar + unit tests + game tests
./gradlew runClient    # dev client
```

## License

[MIT](LICENSE)
