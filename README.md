# BedWars1058-LuckyBlock

A [BedWars1058](https://www.spigotmc.org/resources/bedwars1058-bedwars-mini-game-since-2019.50942/) addon that adds lucky blocks to your games. Breaking one drops a random item: Sleeping Dust, Ice Bridge, Bridge Egg, Inventory Copier, Lightning Scroll, Player Launcher, Fireball, TNT, team-color Wool or Rainbow Wool. Every match gets more chaotic.

**Works on Minecraft 1.8 up to 1.20.6**, the versions BedWars1058 has version support for, with one jar. It needs [BedWars1058](https://www.spigotmc.org/resources/bedwars1058-bedwars-mini-game-since-2019.50942/) and a Spigot or Paper server. Java 8 or newer, whatever your server version needs.

## Install

1. Put `BedWars1058-LuckyBlock-<version>.jar` in `plugins/`.
2. Tell the plugin which BedWars1058 arena group gets lucky blocks: `/bwlb group add <group>` (see below).
3. Start a game on one of those maps. The generators place themselves.

Coming from the old `LuckyBlocks` plugin? On the first start the files in `plugins/LuckyBlocks/` are copied to `plugins/BedWars1058-LuckyBlock/`, so your settings and generators carry over. Remove the old jar.

## Build

```
./gradlew build
```

The jar is written to `build/libs/`. The project is compiled against the Spigot 1.8.8 API so nothing newer can be used by accident; the few things that differ between versions (colored blocks, skull textures, item tags, off-hand events) are in `dev.etran.luckyblock.compat`.

---

## Automatic generators

You don't have to place anything. Every arena in a **lucky block group** gets lucky block generators on its own when a game starts (the first lucky block drops 5 seconds in, then one every cooldown):

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

Every drop has a weight per tier under `loot:` in `config.yml` (bigger = more likely, `0` = never). `/bwlb loot` shows the resulting percentages.

### Choosing the arena group

1. In BedWars1058, make a group and put your maps in it: `/bw arenaGroup create LuckyBlocks`, then `/bw arenaGroup set <arena> LuckyBlocks` for each map.
2. Tell this plugin about it: **`/bwlb group add LuckyBlocks`**. It lists every map found in that group and how many team, diamond and emerald generators it found in each.
3. Start a game. `/bwlb status` shows what is running.

`/bwlb group list` shows all your BedWars1058 groups and which ones get lucky blocks, `/bwlb group scan <group>` repeats the report, and `/bwlb group remove <group>` turns a group off. You can also list groups in `config.yml` under `groups:`. Groups added with the command are saved in `groups.yml`.

Existing `data.yml` generators keep working.

## Changing the generators of an arena

Use `/bwlb list <arena>` to see every generator with clickable **[Remove]** / **[Restore]** buttons, and `/bwlb show <arena>` to float a label above each one in the world. Automatic generators are numbered `a1`, `a2`...; ones you add yourself get plain numbers.

## Commands

Everything is under **`/bwlb`** (also `/bw-luckyblock`, and the old `/luckyblock` still works).

Permission: `luckyblock.admin`. All commands tab-complete.

| Command | What it does |
| --- | --- |
| `/bwlb group <add\|remove\|scan\|list> [group]` | Chooses the BedWars1058 arena groups whose maps get generators, and reports what it found in them |
| `/bwlb status` | Shows the running games and whether their generators are running (and why not) |
| `/bwlb list <arena>` | Shows every generator, whether it runs, and why not |
| `/bwlb show <arena>` | Labels the generators in the world for 15 seconds |
| `/bwlb addgenerator <arena> <team\|diamond\|emerald> [cooldown]` | Adds a generator where you stand. For a team, use its name (e.g. `Red`) |
| `/bwlb removegenerator <arena> [id]` | Removes a generator by id (`a3` or `2`), or the one within 4 blocks of you if you give no id |
| `/bwlb restore <arena> <id\|all>` | Brings back automatic generators you removed |
| `/bwlb auto <arena> <on\|off\|default>` | Forces automatic generators on/off for one arena (`default` follows its group) |
| `/bwlb loot [<drop> <team\|diamond\|emerald> <weight>]` | Shows the chance of every drop for team, diamond and emerald lucky blocks, or changes one weight |
| `/bwlb config [setting] [value]` | Shows the settings, or changes one in game (see below) |
| `/bwlb reload` | Re-reads config.yml, textures.yml, lang.yml, data.yml and the other files |
| `/bwlb autosetup <arena> [cooldown]` | Copies an arena's generators into manual ones (for arenas outside the lucky block groups) |

Changes apply from the next game.

## Changing settings in game

`/bwlb config` shows every setting. Change one with `/bwlb config <setting> <value>`; it is written to `config.yml` (the comments stay) and used by every game that starts from then on. Games that are already running keep the generators they started with.

| Setting | Value |
| --- | --- |
| `generator-limit` | whole number, how many lucky blocks of one generator may lie around |
| `do-not-spawn-if-team-dead` | `true` / `false` |
| `team.enabled`, `diamond.enabled`, `emerald.enabled` | `true` / `false` |
| `team.cooldown`, `diamond.cooldown`, `emerald.cooldown` | seconds between lucky blocks |
| `team.textures`, `diamond.textures`, `emerald.textures` | `add <color>` or `remove <color>`: which colors that generator spawns |
| `loot.<drop>.<team\|diamond\|emerald>` | drop weight (`/bwlb loot <drop> <tier> <weight>` does the same) |

Arena groups are changed with `/bwlb group`. If you edit a file by hand, run `/bwlb reload` instead of restarting.

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
