package dev.etran.luckyblock.data

/**
 * The BedWars1058 arena groups added with /bwlb group add (groups.yml). They work together with the
 * "groups" list in config.yml: an arena in either gets automatic lucky block generators.
 */
class GroupStore(private val file: YmlFile) {

    /** Re-reads groups.yml from disk. */
    fun reload() {
        file.reloadConfig()
    }

    fun getGroups(): MutableList<String> =
        file.getConfig().getStringList("groups").filterNotNull().map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()

    fun contains(group: String): Boolean = getGroups().any { it.equals(group.trim(), ignoreCase = true) }

    /** Returns false if the group was already there. */
    fun add(group: String): Boolean {
        if (contains(group)) return false
        val groups = getGroups()
        groups.add(group.trim())
        save(groups)
        return true
    }

    /** Returns false if the group was not in the list. */
    fun remove(group: String): Boolean {
        val groups = getGroups()
        val removed = groups.removeAll { it.equals(group.trim(), ignoreCase = true) }
        if (removed) save(groups)
        return removed
    }

    private fun save(groups: List<String>) {
        file.getConfig().set("groups", groups)
        file.saveConfig()
    }
}
