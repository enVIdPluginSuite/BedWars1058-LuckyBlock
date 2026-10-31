package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.generator.GeneratorPlan
import dev.etran.luckyblock.utils.SetupUtils
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

/**
 * Copies the automatic generators of an arena into data.yml as manual generators. You only need this
 * for arenas outside the lucky block groups, or to take over an arena and edit every generator yourself.
 */
class SubAutoSetup : SubCommand() {
    override val name = "autosetup"
    override val usage = "autosetup <arena> [cooldown]"
    override val description = "Turns an arena's team/diamond/emerald generators into manual ones (works for any group)"

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty() || args.size > 2) {
            sendUsage(sender)
            return
        }
        var cooldown: Int? = null
        if (args.size == 2) {
            cooldown = parseCooldown(args[1])
            if (cooldown == null) {
                sender.sendMessage(ChatColor.RED.toString() + "Please provide the cooldown as a number of seconds (1 or more).")
                return
            }
        }
        val arena = ArenaContext.load(sender, args[0]) ?: return
        var added = 0
        for (entry in arena.plan.entries) {
            val spot = entry.spec
            // only the automatic spots that aren't removed and don't already have a manual generator
            if (!spot.isAuto || entry.status == GeneratorPlan.Status.EXCLUDED || entry.status == GeneratorPlan.Status.REPLACED) continue
            SetupUtils.addGenerator(arena.name, spot.kind, spot.team, spot.x, spot.y, spot.z, cooldown ?: spot.cooldown)
            added++
        }
        if (added == 0) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "Nothing to set up: " + arena.name + " has no generators that aren't already manual or removed.")
            return
        }
        sender.sendMessage(
            ChatColor.GREEN.toString() + "Auto-Setup completed for " + ChatColor.AQUA + arena.name + ChatColor.GREEN + ": " +
                added + " manual generators created. See /bwlb list " + arena.name
        )
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> =
        if (args.size == 1) filter(SetupUtils.arenaNames(), args[0]) else super.tabComplete(sender, args)
}
