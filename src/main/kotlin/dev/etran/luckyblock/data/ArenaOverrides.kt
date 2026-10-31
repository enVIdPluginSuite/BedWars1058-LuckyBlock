package dev.etran.luckyblock.data

/**
 * Per-arena settings for the automatic generators, stored in arena-settings.yml and changed with
 * /bwlb auto, /bwlb removegenerator and /bwlb restore.
 *
 *   MyArena:
 *     auto: true            # (optional) force automatic generators on/off, ignoring the group
 *     excluded:             # automatic generators removed by an admin (block coordinates)
 *       - "12,64,-30"
 */
class ArenaOverrides(private val file: YmlFile) {

    /** Re-reads arena-settings.yml from disk. */
    fun reload() {
        file.reloadConfig()
    }

    /** TRUE / FALSE if forced for this arena, null to follow the group. */
    fun getAuto(arena: String): Boolean? {
        val config = file.getConfig()
        val path = "$arena.auto"
        return if (config.isBoolean(path)) config.getBoolean(path) else null
    }

    fun setAuto(arena: String, value: Boolean?) {
        file.getConfig().set("$arena.auto", value)
        cleanup(arena)
        file.saveConfig()
    }

    fun getExcluded(arena: String): MutableSet<String> =
        LinkedHashSet(file.getConfig().getStringList("$arena.excluded"))

    /** @return false if it was already excluded */
    fun exclude(arena: String, blockKey: String): Boolean {
        val excluded = getExcluded(arena)
        if (!excluded.add(blockKey)) return false
        saveExcluded(arena, excluded)
        return true
    }

    /** @return false if it wasn't excluded */
    fun restore(arena: String, blockKey: String): Boolean {
        val excluded = getExcluded(arena)
        if (!excluded.remove(blockKey)) return false
        saveExcluded(arena, excluded)
        return true
    }

    /** @return how many were restored */
    fun restoreAll(arena: String): Int {
        val excluded = getExcluded(arena)
        saveExcluded(arena, LinkedHashSet())
        return excluded.size
    }

    private fun saveExcluded(arena: String, excluded: Set<String>) {
        file.getConfig().set("$arena.excluded", if (excluded.isEmpty()) null else ArrayList(excluded))
        cleanup(arena)
        file.saveConfig()
    }

    /** Removes the arena's section once nothing is left in it. */
    private fun cleanup(arena: String) {
        val config = file.getConfig()
        val section = config.getConfigurationSection(arena)
        if (section != null && section.getKeys(false).isEmpty()) config.set(arena, null)
    }
}
