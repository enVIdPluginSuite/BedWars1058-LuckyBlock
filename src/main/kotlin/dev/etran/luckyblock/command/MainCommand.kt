package dev.etran.luckyblock.command

import dev.etran.luckyblock.command.subcommands.SubAddGenerator
import dev.etran.luckyblock.command.subcommands.SubAuto
import dev.etran.luckyblock.command.subcommands.SubAutoSetup
import dev.etran.luckyblock.command.subcommands.SubCommand
import dev.etran.luckyblock.command.subcommands.SubConfig
import dev.etran.luckyblock.command.subcommands.SubDrops
import dev.etran.luckyblock.command.subcommands.SubGroup
import dev.etran.luckyblock.command.subcommands.SubList
import dev.etran.luckyblock.command.subcommands.SubLoot
import dev.etran.luckyblock.command.subcommands.SubRarity
import dev.etran.luckyblock.command.subcommands.SubReload
import dev.etran.luckyblock.command.subcommands.SubRemoveGenerator
import dev.etran.luckyblock.command.subcommands.SubRestore
import dev.etran.luckyblock.command.subcommands.SubShow
import dev.etran.luckyblock.command.subcommands.SubStatus
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import java.util.Locale

class MainCommand : TabExecutor {
    private val subCommands: MutableMap<String, SubCommand> = LinkedHashMap()

    init {
        register(SubGroup())
        register(SubStatus())
        register(SubList())
        register(SubShow())
        register(SubAddGenerator())
        register(SubRemoveGenerator())
        register(SubRestore())
        register(SubAuto())
        register(SubAutoSetup())
        register(SubLoot())
        register(SubRarity())
        register(SubConfig())
        register(SubDrops())
        register(SubReload())
    }

    private fun register(subCommand: SubCommand) {
        subCommands[subCommand.name] = subCommand
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val subCommand = if (args.isEmpty()) null else subCommands[args[0].lowercase(Locale.ROOT)]
        if (subCommand == null) {
            sendHelp(sender)
            return true
        }
        if (subCommand.requiresPlayer() && sender !is Player) {
            sender.sendMessage(ChatColor.RED.toString() + "Cannot execute this command as console")
            return true
        }
        subCommand.execute(sender, args.copyOfRange(1, args.size))
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (args.size <= 1) {
            val typed = if (args.isEmpty()) "" else args[0].lowercase(Locale.ROOT)
            return subCommands.keys.filter { it.startsWith(typed) }
        }
        val subCommand = subCommands[args[0].lowercase(Locale.ROOT)] ?: return emptyList()
        return subCommand.tabComplete(sender, args.copyOfRange(1, args.size))
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage(ChatColor.GOLD.toString() + "LuckyBlocks " + ChatColor.GRAY + "- lucky blocks spawn automatically in arenas of the lucky block groups.")
        for (subCommand in subCommands.values) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "/bwlb " + subCommand.usage + ChatColor.GRAY + " - " + subCommand.description)
        }
    }
}
