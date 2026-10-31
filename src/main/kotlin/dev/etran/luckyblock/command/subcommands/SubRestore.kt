package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.generator.GeneratorPlan
import dev.etran.luckyblock.utils.SetupUtils
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

class SubRestore : SubCommand() {
    override val name = "restore"
    override val usage = "restore <arena> <id|all>"
    override val description = "Brings back automatic generators you removed"

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.size != 2) {
            sendUsage(sender)
            return
        }
        if (args[1].equals("all", ignoreCase = true)) {
            val restored = LuckyBlocks.getInstance().arenaOverrides.restoreAll(SetupUtils.canonicalArenaName(args[0]) ?: args[0])
            sender.sendMessage(
                if (restored == 0) ChatColor.YELLOW.toString() + "No removed generators to restore in " + args[0] + "."
                else ChatColor.GREEN.toString() + "Restored " + restored + " automatic generator" + (if (restored == 1) "" else "s") + " in " + args[0] + "."
            )
            return
        }
        val arena = ArenaContext.load(sender, args[0]) ?: return
        val entry = arena.plan.byDisplayId(args[1])
        if (entry == null || entry.status != GeneratorPlan.Status.EXCLUDED ||
            !LuckyBlocks.getInstance().arenaOverrides.restore(arena.name, entry.spec.blockKey)
        ) {
            sender.sendMessage(
                ChatColor.RED.toString() + "\"" + args[1] + "\" is not a removed generator of " + arena.name +
                    ". Removed ones are shown in red in /bwlb list " + arena.name
            )
            return
        }
        sender.sendMessage(
            ChatColor.GREEN.toString() + "Restored " + ChatColor.WHITE + entry.spec.describe() + ChatColor.GREEN + " generator " +
                ChatColor.GRAY + "[" + arena.name + "-" + entry.spec.displayId + "]" + ChatColor.GREEN + "."
        )
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) return filter(SetupUtils.arenaNames(), args[0])
        if (args.size == 2) {
            val ids = ArrayList<String>()
            ids.add("all")
            val name = SetupUtils.canonicalArenaName(args[0])
            if (name != null) {
                val yml = SetupUtils.loadArenaYml(name)
                val plan = GeneratorPlan.forArena(name, SetupUtils.groupOf(name, yml), yml)
                for (entry in plan.entries) {
                    if (entry.status == GeneratorPlan.Status.EXCLUDED) ids.add(entry.spec.displayId)
                }
            }
            return filter(ids, args[1])
        }
        return super.tabComplete(sender, args)
    }
}
