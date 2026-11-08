package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.drops.Drop
import dev.etran.luckyblock.drops.Rarity
import dev.etran.luckyblock.utils.ColorUtils
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import java.util.Locale

/** Shows the loot table of each lucky block rarity with the percentage of every drop, or changes a weight. */
class SubLoot : SubCommand() {
    override val name = "loot"
    override val usage = "loot [<rarity> [<drop> <weight>]]"
    override val description = "Shows how likely each drop is for a rarity (" + Rarity.keysText() + "), or changes a weight"

    override fun execute(sender: CommandSender, args: Array<String>) {
        var config = LuckyBlocks.getInstance().cachedConfig
        var loot = config.loot
        if (args.isEmpty()) {
            sender.sendMessage(ChatColor.GOLD.toString() + "Lucky block loot " + ChatColor.GRAY + "(" + Drop.ids().size + " drops)")
            for (rarity in Rarity.values()) {
                val usable = Drop.ids().count { loot.getWeight(rarity, it) > 0 }
                sender.sendMessage(" " + ColorUtils.colorize(config.getRarity(rarity).display) + ChatColor.GRAY + ": " + usable + " drops")
            }
            sender.sendMessage(ChatColor.GRAY.toString() + "See one with /bwlb loot <rarity>, change with /bwlb loot <rarity> <drop> <weight> (0 = never).")
            return
        }
        val rarity = Rarity.fromKey(args[0])
        if (rarity == null) {
            sender.sendMessage(ChatColor.RED.toString() + "The rarity must be ${Rarity.keysText()}.")
            return
        }
        if (args.size == 3) {
            val problem = ConfigChanges.setLoot(rarity, args[1], args[2])
            if (problem != null) {
                sender.sendMessage(ChatColor.RED.toString() + problem)
                return
            }
            sender.sendMessage(ChatColor.GREEN.toString() + "Saved to config.yml. " + ChatColor.GRAY + "It applies right away.")
            config = LuckyBlocks.getInstance().cachedConfig
            loot = config.loot
        } else if (args.size != 1) {
            sendUsage(sender)
            return
        }
        sender.sendMessage(ColorUtils.colorize(config.getRarity(rarity).display) + ChatColor.GOLD + " loot " + ChatColor.GRAY + "(chance per lucky block)")
        val ids = Drop.ids().sortedByDescending { loot.getWeight(rarity, it) }
        for (id in ids) {
            val weight = loot.getWeight(rarity, id)
            if (weight <= 0) continue
            sender.sendMessage(
                ChatColor.YELLOW.toString() + " " + id + ChatColor.GRAY + ": " + ChatColor.WHITE +
                    String.format(Locale.ROOT, "%.1f%%", loot.chance(rarity, id, Drop.ids())) + ChatColor.GRAY + " (weight " + weight + ")"
            )
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) return filter(Rarity.values().map { it.key }, args[0])
        if (args.size == 2) return filter(Drop.ids(), args[1])
        return super.tabComplete(sender, args)
    }
}
