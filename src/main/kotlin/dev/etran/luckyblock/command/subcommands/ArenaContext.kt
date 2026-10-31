package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.generator.GeneratorPlan
import dev.etran.luckyblock.utils.SetupUtils
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration

/** An arena as the setup commands see it: its BedWars1058 file plus which generators it gets. */
internal class ArenaContext private constructor(
    val name: String,
    val yml: FileConfiguration,
    val group: String?,
    val plan: GeneratorPlan.Result
) {
    companion object {
        /** Loads the arena, or tells the sender it doesn't exist and returns null. */
        fun load(sender: CommandSender, typedName: String): ArenaContext? {
            val name = SetupUtils.canonicalArenaName(typedName)
            val yml = if (name == null) null else SetupUtils.loadArenaYml(name)
            if (name == null || yml == null) {
                sender.sendMessage(ChatColor.RED.toString() + "Arena not found! use /bw arenaList")
                return null
            }
            val group = SetupUtils.groupOf(name, yml)
            return ArenaContext(name, yml, group, GeneratorPlan.forArena(name, group, yml))
        }
    }
}
