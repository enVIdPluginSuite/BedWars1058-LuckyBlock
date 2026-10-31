# BedWars1058-LuckyBlock

A [BedWars1058](https://www.spigotmc.org/resources/bedwars1058-bedwars-mini-game-since-2019.50942/) addon that adds lucky blocks to your games. Breaking one drops a random item: Sleeping Dust, Ice Bridge, Bridge Egg, Inventory Copier, Lightning Scroll, Player Launcher, Fireball, TNT, team-color Wool or Rainbow Wool. Every match gets more chaotic.

**Works on Minecraft 1.8 up to 1.20.6**, the versions BedWars1058 has version support for, with one jar. It needs [BedWars1058](https://www.spigotmc.org/resources/bedwars1058-bedwars-mini-game-since-2019.50942/) and a Spigot or Paper server. Java 8 or newer, whatever your server version needs.

## Install

1. Put `BedWars1058-LuckyBlock-<version>.jar` in `plugins/`.
2. Make a BedWars1058 group for your lucky block maps (see below).
3. Start the server. That's it.

Coming from the old `LuckyBlocks` plugin? On the first start the files in `plugins/LuckyBlocks/` are copied to `plugins/BedWars1058-LuckyBlock/`, so your settings and generators carry over. Remove the old jar.

## Build

```
./gradlew build
```

The jar is written to `build/libs/`. The project is compiled against the Spigot 1.8.8 API so nothing newer can be used by accident; the few things that differ between versions (colored blocks, skull textures, item tags, off-hand events) are in `dev.etran.luckyblock.compat`.

---

## Automatic generators

You don't have to place anything. Every arena in a **lucky block group** gets lucky block generators on its own when a game starts:

- one at each **team** island (next to the team's iron generator)
- one at every **diamond** generator
- one at every **emerald** generator

Each kind spawns its own set of lucky block colors, with its own cooldown. All of it is set in `config.yml`:

```yaml
groups:            # BedWars1058 arena groups that get lucky blocks
  - LuckyBlocks

generators:
  team:
    enabled: true
    cooldown: 40   # seconds between lucky blocks
    textures: [white, light_gray, gray, brown, black]   # names from textures.yml
  diamond:
    cooldown: 60
    textures: [light_blue, cyan, blue, purple, magenta]
  emerald:
    cooldown: 90
    textures: [green, lime, yellow, orange, red, pink]
```

### Better loot from diamond and emerald blocks

A lucky block's loot depends on its tier, which comes from its color (the lists above). Diamond and emerald blocks roll on better loot tables: the strong items (Lightning Scroll, Inventory Copier, Player Launcher, Sleeping Dust) are much more likely and plain wool is rarer.

| | Team | Diamond | Emerald |
| --- | --- | --- | --- |
| Strong items (combined) | 17% | 36% | 47% |
| Team-color wool | 27% | 9% | 5% |

Every drop has a weight per tier under `loot:` in `config.yml` (bigger = more likely, `0` = never). `/luckyblock loot` shows the resulting percentages.

To make a lucky block group in BedWars1058: `/bw arenaGroup create LuckyBlocks`, then `/bw arenaGroup set <arena> LuckyBlocks`.
Existing `data.yml` generators keep working.

## Changing the generators of an arena

Use `/luckyblock list <arena>` to see every generator with clickable **[Remove]** / **[Restore]** buttons, and `/luckyblock show <arena>` to float a label above each one in the world. Automatic generators are numbered `a1`, `a2`...; ones you add yourself get plain numbers.

## Commands

Permission: `luckyblock.admin`. All commands tab-complete.

| Command | What it does |
| --- | --- |
| `/luckyblock list <arena>` | Shows every generator, whether it runs, and why not |
| `/luckyblock show <arena>` | Labels the generators in the world for 15 seconds |
| `/luckyblock addgenerator <arena> <team\|diamond\|emerald> [cooldown]` | Adds a generator where you stand. For a team, use its name (e.g. `Red`) |
| `/luckyblock removegenerator <arena> [id]` | Removes a generator by id (`a3` or `2`), or the one within 4 blocks of you if you give no id |
| `/luckyblock restore <arena> <id\|all>` | Brings back automatic generators you removed |
| `/luckyblock auto <arena> <on\|off\|default>` | Forces automatic generators on/off for one arena (`default` follows its group) |
| `/luckyblock loot` | Shows the chance of every drop for team, diamond and emerald lucky blocks |
| `/luckyblock autosetup <arena> [cooldown]` | Copies an arena's generators into manual ones (for arenas outside the lucky block groups) |

Changes apply from the next game.

---

---

## Gameplay

- Place or break lucky blocks
- Drops random fun Items
- Control chaos in BedWars arenas
- Every match feels unique

---

## Notes for other versions

- Item tags are stored in the item's display name as invisible color codes, because 1.8 has no item data container. Renaming a lucky item in an anvil removes its function.
- Heads use the PlayerProfile API on 1.18.1+ and a GameProfile on older versions.

## Developer note
This plugin is coded with AI assistance and architected completely by me. Regardless, use at your own risk.

## License
This plugin is under the MIT License, check the [License](LICENSE) file for more details.
