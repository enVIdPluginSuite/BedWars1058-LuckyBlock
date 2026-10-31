package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.drops.Rarity
import dev.etran.luckyblock.generator.GeneratorKind
import dev.etran.luckyblock.utils.ColorUtils
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import kotlin.math.roundToLong

/** Shows the chance of each rarity for team, diamond and emerald lucky blocks. */
class SubRarity : SubCommand() {
    override val name = "rarity"
    override val usage = "rarity [<team|diamond|emerald> <rarity> <weight>]"
    override val description = "Shows the rarity roll of each generator kind, or changes one"

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.size == 3) {
            val problem = ConfigChanges.setRarityChance(args[0], args[1], args[2])
            if (problem != null) {
                sender.sendMessage(ChatColor.RED.toString() + problem)
                return
            }
            sender.sendMessage(ChatColor.GREEN.toString() + "Saved to config.yml. " + ChatColor.GRAY + "It applies right away.")
        } else if (args.isNotEmpty()) {
            sendUsage(sender)
            return
        }
        val config = LuckyBlocks.getInstance().cachedConfig
        sender.sendMessage(ChatColor.GOLD.toString() + "Rarity of lucky blocks " + ChatColor.GRAY + "(roll weights, % of total)")
        for (kind in GeneratorKind.values()) {
            val line = StringBuilder(ChatColor.YELLOW.toString() + " " + kind.configKey + ChatColor.GRAY + ":")
            for (rarity in Rarity.values()) {
                val percent = config.getRarityChance(kind, rarity)
                line.append(' ').append(ColorUtils.colorize(config.getRarity(rarity).display)).append(ChatColor.WHITE)
                    .append(' ').append((percent * 10.0).roundToLong() / 10.0).append('%').append(ChatColor.GRAY).append(',')
            }
            sender.sendMessage(line.substring(0, line.length - 1))
        }
        sender.sendMessage(
            ChatColor.GRAY.toString() + "Lucky blocks near the middle of a map roll a better rarity: up to " +
                config.middleUpgrade + "% chance of a one-step upgrade (set middle-upgrade)."
        )
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) return filter(GeneratorKind.values().map { it.configKey }, args[0])
        if (args.size == 2) return filter(Rarity.values().map { it.key }, args[1])
        return super.tabComplete(sender, args)
    }
}
