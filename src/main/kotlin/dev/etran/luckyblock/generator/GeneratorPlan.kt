package dev.etran.luckyblock.generator

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.arena.LoadedArena
import dev.etran.luckyblock.data.CachedConfig
import org.bukkit.configuration.file.FileConfiguration

/**
 * Decides which lucky block generators an arena gets:
 *
 *  - manual generators from data.yml always run,
 *  - automatic generators (detected from the arena file) run when the arena's BedWars1058 group is a
 *    lucky block group (or automatic generators were forced on for the arena),
 *    unless that generator type is disabled in config.yml, the generator was removed with
 *    /bwlb removegenerator, or a manual generator already sits in the same block.
 */
object GeneratorPlan {

    enum class Status(val text: String) {
        /** The generator runs. */
        ACTIVE("active"),

        /** Removed by an admin; can be brought back with /bwlb restore. */
        EXCLUDED("removed"),

        /** Automatic generators are off for this arena. */
        AUTO_OFF("automatic generators off"),

        /** This generator type is disabled in config.yml. */
        KIND_DISABLED("disabled in config.yml"),

        /** A manual generator in the same block takes its place. */
        REPLACED("replaced by manual generator")
    }

    class Entry(val spec: LoadedGenerator, val status: Status) {
        val isActive: Boolean get() = status == Status.ACTIVE
    }

    class Result(
        val group: String?,
        val isAutoEnabled: Boolean,
        val autoReason: String,
        /** Manual generators first (by id), then the automatic ones (a1, a2, ...). */
        val entries: List<Entry>
    ) {
        fun getActive(): List<Entry> = entries.filter { it.isActive }

        /** Finds an entry by what the player types: "3" (manual) or "a2" (automatic). */
        fun byDisplayId(id: String): Entry? = entries.firstOrNull { it.spec.displayId.equals(id, ignoreCase = true) }
    }

    /** Builds the plan for an arena from the plugin's live state (config.yml, data.yml, arena-settings.yml). */
    @JvmStatic
    fun forArena(arenaName: String, group: String?, arenaYml: FileConfiguration?): Result {
        val plugin = LuckyBlocks.getInstance()
        val config = plugin.cachedConfig
        val overrides = plugin.arenaOverrides
        val forced = overrides.getAuto(arenaName)
        val autoEnabled: Boolean
        val reason: String
        if (forced != null) {
            autoEnabled = forced
            reason = if (autoEnabled) "forced on for this arena" else "forced off for this arena"
        } else if (config.isLuckyGroup(group)) {
            autoEnabled = true
            reason = "group \"$group\" is a lucky block group"
        } else {
            autoEnabled = false
            reason = "group \"$group\" is not a lucky block group"
        }
        val manualArena = LoadedArena.byName(arenaName)
        val manual: List<LoadedGenerator> = if (manualArena == null) ArrayList() else ArrayList(manualArena.getGenerators())
        val spots: List<LoadedGenerator> = if (arenaYml == null) ArrayList() else ArenaScanner.scan(arenaName, arenaYml, config)
        return Result(group, autoEnabled, reason, build(manual, spots, autoEnabled, overrides.getExcluded(arenaName), config))
    }

    /** The pure decision logic (no plugin state), so it can be tested on its own. */
    @JvmStatic
    fun build(
        manual: Collection<LoadedGenerator>, autoSpots: List<LoadedGenerator>, autoEnabled: Boolean,
        excluded: Set<String>, config: CachedConfig
    ): List<Entry> {
        val entries = ArrayList<Entry>()
        val manualBlocks = HashSet<String>()
        for (generator in manual.sortedBy { it.id }) {
            manualBlocks.add(generator.blockKey)
            entries.add(Entry(generator, Status.ACTIVE))
        }
        for (spot in autoSpots) {
            val status = when {
                excluded.contains(spot.blockKey) -> Status.EXCLUDED
                !autoEnabled -> Status.AUTO_OFF
                !config.getKind(spot.kind).isEnabled -> Status.KIND_DISABLED
                manualBlocks.contains(spot.blockKey) -> Status.REPLACED
                else -> Status.ACTIVE
            }
            entries.add(Entry(spot, status))
        }
        return entries
    }
}
