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

You don't have to place anything. Every arena in a **lucky block group** gets lucky block generators on its own when a game starts:

- one at each **team** island (next to the team's iron generator)
- one at every **diamond** generator
- one at every **emerald** generator

To make a lucky block group in BedWars1058: `/bw arenaGroup create LuckyBlocks`, then `/bw arenaGroup set <arena> LuckyBlocks`. Tell the plugin about it with `/bwlb group add LuckyBlocks`.

```yaml
generators:
  team:    { enabled: true, cooldown: 20 }   # seconds between lucky blocks at normal speed
  diamond: { enabled: true, cooldown: 30 }
  emerald: { enabled: true, cooldown: 45 }
sync-upgrades: true      # lucky blocks speed up by the same % as the real generator's upgrades
```

The first lucky block of every generator appears after about 5 seconds.

### Synced with generator upgrades

With `sync-upgrades: true` each lucky block generator follows the BedWars1058 generator next to it (iron for a team, diamond or emerald otherwise). When forge or diamond/emerald upgrades make that generator 50% faster, the lucky blocks come 50% faster too.

### Timers above the generator

A hologram shows the real generator's timer for its item, and under it the time until the next lucky block and the **rarity it will have**:

```
Diamond spawns in 12s
✦ Rare Lucky Block in 21s
```

Turn it off with `hologram.enabled: false`; the heights are `hologram.height-team` and `hologram.height-ore`.

## Rarities and loot

Every lucky block is **Common, Uncommon, Rare or Legendary**. The rarity is in the item name (`{rarity} Lucky Block`, set with `lucky-block-name`) and decides its color: each rarity has its own list of colors under `rarities:` in `config.yml`.

- **Roll per generator** (`rarity-chances`): team generators mostly drop Common, diamond ones Uncommon, emerald ones Rare and Legendary.
- **Closer to the middle = better**: the closer a generator is to the middle of the map, the more likely its roll is raised one step, up to `middle-upgrade` percent at the very center.
- **Loot per rarity** (`loot.<rarity>.<drop>`): each rarity has its own table with a weight for every drop. There are 49 drops: resources, team-colored blocks, tools, swords, armor, bows, potions, golden apples, the original special items (Sleeping Dust, Ice Bridge, Bridge Egg, Inventory Copier, Lightning Scroll, Player Launcher, Rainbow Wool, Fireball, TNT) and new ones:
    - **Swap Scroll**: swap places with a random enemy
    - **Jump Feather**: leap into the air
    - **Healing Totem**: heal yourself and teammates nearby

`/bwlb rarity` shows the roll chances and `/bwlb loot <rarity>` the chance of every drop, and both can change a weight.

## Changing the generators of an arena

Use `/bwlb list <arena>` to see every generator with clickable **[Remove]** / **[Restore]** buttons, and `/bwlb show <arena>` to float a label above each one in the world. Automatic generators are numbered `a1`, `a2`...; ones you add yourself get plain numbers.

## Commands

Everything is under **`/bwlb`** (also `/bw-luckyblock`, and the old `/bwlb` still works).

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

## Credits
Original plugin by **PmzHero**, modified and improved by **NGT_YT**.
1.8 - 1.20.6 support, automatic generators and tiered loot by **enVId Tech**.

## Developer note
This plugin is coded with AI assistance and architected completely by me. Regardless, use at your own risk.

## License
This plugin is under the MIT License, check the [License](LICENSE) file for more details.
