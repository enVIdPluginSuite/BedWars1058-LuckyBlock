package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.utils.SetupUtils
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import java.util.Locale

class SubAuto : SubCommand() {
    override val name = "auto"
    override val usage = "auto <arena> <on|off|default>"
    override val description = "Forces automatic generators on/off for one arena (default = follow its group)"

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.size != 2) {
            sendUsage(sender)
            return
        }
        val arena = ArenaContext.load(sender, args[0]) ?: return
        val value: Boolean? = when (args[1].lowercase(Locale.ROOT)) {
            "on" -> true
            "off" -> false
            "default" -> null
            else -> {
                sendUsage(sender)
                return
            }
        }
        LuckyBlocks.getInstance().arenaOverrides.setAuto(arena.name, value)
        if (value == null) {
            val lucky = LuckyBlocks.getInstance().cachedConfig.isLuckyGroup(arena.group)
            sender.sendMessage(
                ChatColor.GREEN.toString() + "Automatic generators in " + ChatColor.AQUA + arena.name + ChatColor.GREEN +
                    " now follow its group (" + arena.group + "): " + (if (lucky) "ON" else "OFF") + "."
            )
        } else {
            sender.sendMessage(
                ChatColor.GREEN.toString() + "Automatic generators in " + ChatColor.AQUA + arena.name + ChatColor.GREEN +
                    " are now forced " + (if (value) "ON" else "OFF") + "." +
                    (if (value) "" else ChatColor.GRAY.toString() + " Manually added generators still run.")
            )
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) return filter(SetupUtils.arenaNames(), args[0])
        if (args.size == 2) return filter(listOf("on", "off", "default"), args[1])
        return super.tabComplete(sender, args)
    }
}
