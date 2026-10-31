package dev.etran.luckyblock.generator

import kotlin.math.floor

/**
 * A lucky block generator definition (not a running one).
 *
 * Manual generators come from data.yml and have an id >= 0.
 * Automatic generators are detected from the BedWars1058 arena file and have a negative id.
 */
class LoadedGenerator(
    val arenaName: String,
    val kind: GeneratorKind,
    team: String?,
    val id: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val cooldown: Int
) {
    /** Team name, only set when kind == TEAM. */
    val team: String? = if (kind == GeneratorKind.TEAM) team else null

    val isAuto: Boolean get() = id < 0

    /** What the player types in commands: "3" for manual generator 3, "a2" for automatic generator 2. */
    val displayId: String get() = if (isAuto) "a${-id}" else id.toString()

    /** "TEAM Red", "DIAMOND", "EMERALD" */
    fun describe(): String = if (kind == GeneratorKind.TEAM) "TEAM $team" else kind.name

    /** The block this generator sits in, e.g. "12,64,-30". Used to spot duplicates and exclusions. */
    val blockKey: String get() = blockKey(x, y, z)

    /** data.yml value: "x,y,z,cooldown,team" (team column holds @diamond / @emerald for those kinds). */
    fun serialize(): String {
        val column = if (kind == GeneratorKind.TEAM) team else kind.token
        return "$x,$y,$z,$cooldown,$column"
    }

    companion object {
        @JvmStatic
        fun blockKey(x: Double, y: Double, z: Double): String =
            "${floor(x).toInt()},${floor(y).toInt()},${floor(z).toInt()}"

        /**
         * Reads a data.yml entry.
         *
         * @throws IllegalArgumentException if the value is malformed
         */
        @JvmStatic
        fun parse(arenaName: String, id: String, raw: String?): LoadedGenerator {
            if (raw == null) throw IllegalArgumentException("empty value")
            val split = raw.split(",")
            if (split.size < 5) throw IllegalArgumentException("expected \"x,y,z,cooldown,team\" but got \"$raw\"")
            val cooldown = split[3].trim().toInt()
            if (cooldown < 1) throw IllegalArgumentException("cooldown must be at least 1 second")
            val column = split[4].trim()
            return LoadedGenerator(
                arenaName,
                GeneratorKind.fromTeamColumn(column),
                column,
                id.trim().toInt(),
                split[0].trim().toDouble(),
                split[1].trim().toDouble(),
                split[2].trim().toDouble(),
                cooldown
            )
        }
    }
}
