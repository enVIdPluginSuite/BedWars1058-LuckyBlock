package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.generator.ArenaScanner
import dev.etran.luckyblock.generator.GeneratorKind
import dev.etran.luckyblock.utils.SetupUtils
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import java.util.Locale

/**
 * Picks which BedWars1058 arena groups get lucky blocks. Every arena in such a group gets generators at
 * its team islands and its diamond and emerald generators, found from the arena files.
 */
class SubGroup : SubCommand() {
    override val name = "group"
    override val usage = "group <add|remove|scan|list> [group]"
    override val description = "Choose the BedWars1058 arena group(s) whose maps get lucky block generators automatically"

    override fun execute(sender: CommandSender, args: Array<String>) {
        val action = if (args.isEmpty()) "list" else args[0].lowercase(Locale.ROOT)
        if (action == "list") {
            list(sender)
            return
        }
        if (args.size != 2 || action !in listOf("add", "remove", "scan")) {
            sendUsage(sender)
            return
        }
        val group = args[1]
        val plugin = LuckyBlocks.getInstance()
        val config = plugin.cachedConfig
        if (action == "add") {
            if (config.isLuckyGroup(group)) {
                sender.sendMessage(ChatColor.YELLOW.toString() + "The group " + ChatColor.AQUA + group + ChatColor.YELLOW + " already gets lucky blocks.")
            } else {
                plugin.groupStore.add(group)
                config.setExtraGroups(plugin.groupStore.getGroups())
                sender.sendMessage(
                    ChatColor.GREEN.toString() + "Arenas in the group " + ChatColor.AQUA + group + ChatColor.GREEN +
                        " now get lucky block generators from their next game."
                )
            }
            report(sender, group)
        } else if (action == "remove") {
            if (plugin.groupStore.remove(group)) {
                config.setExtraGroups(plugin.groupStore.getGroups())
                sender.sendMessage(
                    ChatColor.GREEN.toString() + "The group " + ChatColor.AQUA + group + ChatColor.GREEN +
                        " no longer gets lucky blocks from its next game."
                )
            } else if (config.getConfigGroups().contains(group.lowercase(Locale.ROOT))) {
                sender.sendMessage(ChatColor.RED.toString() + "That group is listed in config.yml (\"groups\"). Remove it there and restart.")
            } else {
                sender.sendMessage(ChatColor.RED.toString() + "The group " + group + " was not added. See " + ChatColor.YELLOW + "/bwlb group list")
            }
        } else {
            report(sender, group)
        }
    }

    private fun list(sender: CommandSender) {
        val config = LuckyBlocks.getInstance().cachedConfig
        val found = SetupUtils.arenasByGroup()
        sender.sendMessage(ChatColor.GOLD.toString() + "BedWars1058 arena groups " + ChatColor.GRAY + "(green = gets lucky blocks)")
        if (found.isEmpty()) sender.sendMessage(ChatColor.RED.toString() + " No BedWars1058 arenas were found.")
        for ((group, arenas) in found) {
            val lucky = config.isLuckyGroup(group)
            sender.sendMessage(
                (if (lucky) ChatColor.GREEN else ChatColor.GRAY).toString() + " " + group + ChatColor.DARK_GRAY + " - " +
                    ChatColor.GRAY + arenas.size + " arena(s): " + arenas.joinToString(", ")
            )
        }
        val unmatched = config.getLuckyGroups().filter { group -> found.keys.none { it.equals(group, ignoreCase = true) } }
        if (unmatched.isNotEmpty()) {
            sender.sendMessage(ChatColor.YELLOW.toString() + " Lucky groups with no arena in them yet: " + unmatched.joinToString(", "))
        }
        sender.sendMessage(ChatColor.GRAY.toString() + "Add one with /bwlb group add <group>.")
    }

    /** Lists the arenas in the group and the generators found in each one. */
    private fun report(sender: CommandSender, group: String) {
        val config = LuckyBlocks.getInstance().cachedConfig
        val arenas = SetupUtils.arenasInGroup(group)
        if (arenas.isEmpty()) {
            sender.sendMessage(ChatColor.RED.toString() + "No arena is in a BedWars1058 group called \"" + group + "\" yet.")
            sender.sendMessage(
                ChatColor.GRAY.toString() + "Create the group with /bw arenaGroup create " + group +
                    " and put arenas in it with /bw arenaGroup set <arena> " + group
            )
            return
        }
        sender.sendMessage(
            ChatColor.GOLD.toString() + "Generators found in the group " + ChatColor.AQUA + group + ChatColor.GOLD +
                " (" + arenas.size + " arena(s)):"
        )
        for (arena in arenas) {
            val yml = SetupUtils.loadArenaYml(arena)
            val spots = if (yml == null) emptyList() else ArenaScanner.scan(arena, yml, config)
            val team = spots.count { it.kind == GeneratorKind.TEAM }
            val diamond = spots.count { it.kind == GeneratorKind.DIAMOND }
            val emerald = spots.size - team - diamond
            if (spots.isEmpty()) {
                sender.sendMessage(ChatColor.RED.toString() + " " + arena + ChatColor.GRAY + ": no generators found (is the arena fully set up?)")
            } else {
                sender.sendMessage(ChatColor.GREEN.toString() + " " + arena + ChatColor.GRAY + ": " + team + " team, " + diamond + " diamond, " + emerald + " emerald")
            }
        }
        sender.sendMessage(ChatColor.GRAY.toString() + "See them with /bwlb list <arena>; remove or add single ones there.")
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) return filter(listOf("add", "remove", "scan", "list"), args[0])
        if (args.size == 2 && !args[0].equals("list", true)) {
            val groups: List<String> = if (args[0].equals("remove", true)) {
                LuckyBlocks.getInstance().groupStore.getGroups()
            } else {
                ArrayList(SetupUtils.arenasByGroup().keys)
            }
            return filter(groups, args[1])
        }
        return super.tabComplete(sender, args)
    }
}
