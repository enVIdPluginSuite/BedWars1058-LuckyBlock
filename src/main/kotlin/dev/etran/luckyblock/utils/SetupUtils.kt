package dev.etran.luckyblock.utils

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.arena.LoadedArena
import dev.etran.luckyblock.generator.GeneratorKind
import dev.etran.luckyblock.generator.LoadedGenerator
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.TreeMap

/** Helpers for the setup commands: reading BedWars1058 arena files and editing data.yml. */
object SetupUtils {

    private fun arenasFolder(): File? {
        val bedWars = Bukkit.getPluginManager().getPlugin("BedWars1058") ?: return null
        return File(bedWars.dataFolder, "Arenas")
    }

    /** Names of the arenas that have a BedWars1058 arena file. */
    @JvmStatic
    fun arenaNames(): List<String> {
        val files = arenasFolder()?.listFiles() ?: return emptyList()
        return files.filter { it.isFile && it.name.endsWith(".yml") }
            .map { it.name.substring(0, it.name.length - 4) }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    /** The arena's real name, whatever capitalization was typed. Null if there is no such arena. */
    @JvmStatic
    fun canonicalArenaName(typed: String): String? = arenaNames().firstOrNull { it.equals(typed, ignoreCase = true) }

    /** The arena's BedWars1058 file, or null if there is no such arena. */
    @JvmStatic
    fun loadArenaYml(arenaName: String): FileConfiguration? {
        val folder = arenasFolder()
        if (folder == null || arenaName.contains("/") || arenaName.contains("\\") || arenaName.contains("..")) return null
        val file = File(folder, "$arenaName.yml")
        return if (file.isFile) YamlConfiguration.loadConfiguration(file) else null
    }

    /** The arena's BedWars1058 group (what a running game reports, or what its file says). */
    @JvmStatic
    fun groupOf(arenaName: String, arenaYml: FileConfiguration?): String? {
        val live = LuckyBlocks.getBedWars().arenaUtil.getArenaByName(arenaName)
        if (live != null && live.group != null) return live.group
        return if (arenaYml == null) "Default" else arenaYml.getString("group", "Default")
    }

    /** The BedWars1058 groups found in the arena files, each with the arenas in it (groups compared ignoring case). */
    @JvmStatic
    fun arenasByGroup(): MutableMap<String, MutableList<String>> {
        val groups = TreeMap<String, MutableList<String>>(String.CASE_INSENSITIVE_ORDER)
        for (arena in arenaNames()) {
            val yml = loadArenaYml(arena)
            var group = if (yml == null) "Default" else yml.getString("group", "Default")
            if (group == null || group.trim().isEmpty()) group = "Default"
            groups.getOrPut(group.trim()) { ArrayList() }.add(arena)
        }
        return groups
    }

    /** Names of the arenas whose file says they are in the group (ignoring case). */
    @JvmStatic
    fun arenasInGroup(group: String): MutableList<String> = arenasByGroup()[group.trim()] ?: ArrayList()

    /** Team names of an arena (the keys under "Team" in its arena file). */
    @JvmStatic
    fun teamNames(arenaYml: FileConfiguration?): MutableList<String> {
        val teams = arenaYml?.getConfigurationSection("Team")
        return if (teams == null) ArrayList() else ArrayList(teams.getKeys(false))
    }

    /** Adds a manual generator: remembered in memory and saved to data.yml. */
    @JvmStatic
    fun addGenerator(arenaName: String, kind: GeneratorKind, team: String?, x: Double, y: Double, z: Double, cooldown: Int): LoadedGenerator {
        val file = LuckyBlocks.getInstance().dataFile
        val arena = LoadedArena.getOrCreate(arenaName)
        val generator = arena.addGenerator(arena.nextId(), kind, team, x, y, z, cooldown)
        file.getConfig().set("$arenaName.${generator.id}", generator.serialize())
        file.saveConfig()
        return generator
    }

    /** Removes a manual generator from memory and data.yml. */
    @JvmStatic
    fun removeGenerator(arenaName: String, id: Int): Boolean {
        val arena = LoadedArena.byName(arenaName)
        if (arena == null || !arena.removeGenerator(id)) return false
        val file = LuckyBlocks.getInstance().dataFile
        val data = file.getConfig()
        data.set("$arenaName.$id", null)
        val section = data.getConfigurationSection(arenaName)
        if (section != null && section.getKeys(false).isEmpty()) data.set(arenaName, null)
        file.saveConfig()
        return true
    }
}
