package dev.etran.luckyblock.generator

import dev.etran.luckyblock.data.CachedConfig
import org.bukkit.configuration.file.FileConfiguration

/**
 * Finds the places where a lucky block generator belongs by reading a BedWars1058 arena file:
 * one per team island, plus every diamond and emerald generator.
 *
 * The result is always in the same order (teams in file order, then diamond, then emerald), and the
 * generators get the ids a1, a2, a3... in that order.
 */
object ArenaScanner {

    @JvmStatic
    fun scan(arenaName: String, arenaYml: FileConfiguration, config: CachedConfig): List<LoadedGenerator> {
        val spots = ArrayList<LoadedGenerator>()

        // Team islands: next to the team's iron generator (falls back to gold, then the team spawn)
        val teams = arenaYml.getConfigurationSection("Team")
        if (teams != null) {
            for (team in teams.getKeys(false)) {
                val position = firstLocation(arenaYml, "Team.$team.Iron")
                    ?: firstLocation(arenaYml, "Team.$team.Gold")
                    ?: firstLocation(arenaYml, "Team.$team.Spawn")
                    ?: continue
                spots.add(
                    LoadedGenerator(arenaName, GeneratorKind.TEAM, team, 0, position[0], position[1], position[2],
                        config.getKind(GeneratorKind.TEAM).cooldown)
                )
            }
        }

        // Diamond and emerald generators
        addOreSpots(spots, arenaName, arenaYml, config, GeneratorKind.DIAMOND, "generator.Diamond")
        addOreSpots(spots, arenaName, arenaYml, config, GeneratorKind.EMERALD, "generator.Emerald")

        // number everything in order: a1, a2, a3...
        return spots.mapIndexed { i, spot ->
            LoadedGenerator(arenaName, spot.kind, spot.team, -(i + 1), spot.x, spot.y, spot.z, spot.cooldown)
        }
    }

    private fun addOreSpots(
        spots: MutableList<LoadedGenerator>, arenaName: String, yml: FileConfiguration,
        config: CachedConfig, kind: GeneratorKind, path: String
    ) {
        for (raw in yml.getStringList(path)) {
            val position = parseLocation(raw) ?: continue
            spots.add(LoadedGenerator(arenaName, kind, null, 0, position[0], position[1], position[2], config.getKind(kind).cooldown))
        }
    }

    /** BedWars1058 stores one location as a string, or a list of them. Returns the first one. */
    private fun firstLocation(yml: FileConfiguration, path: String): DoubleArray? {
        val value = yml.get(path)
        if (value is String) return parseLocation(value)
        if (value is List<*>) {
            for (entry in value) {
                if (entry is String) {
                    val position = parseLocation(entry)
                    if (position != null) return position
                }
            }
        }
        return null
    }

    /** "x,y,z,yaw,pitch" -> {x, y, z}. Returns null if it can't be read. */
    @JvmStatic
    fun parseLocation(raw: String?): DoubleArray? {
        if (raw == null) return null
        val split = raw.split(",")
        if (split.size < 3) return null
        return try {
            doubleArrayOf(split[0].trim().toDouble(), split[1].trim().toDouble(), split[2].trim().toDouble())
        } catch (e: NumberFormatException) {
            null
        }
    }
}
