package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.arena.LuckyBlockArena
import dev.etran.luckyblock.generator.GeneratorPlan
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

/** Shows the BedWars1058 games that are running right now and whether lucky blocks are running in them. */
class SubStatus : SubCommand() {
    override val name = "status"
    override val usage = "status"
    override val description = "Shows the running games and what their lucky block generators are doing"

    override fun execute(sender: CommandSender, args: Array<String>) {
        val games = LuckyBlocks.getBedWars().arenaUtil.arenas
        sender.sendMessage(ChatColor.GOLD.toString() + "Lucky blocks in running BedWars1058 games:")
        if (games.isEmpty()) sender.sendMessage(ChatColor.GRAY.toString() + " No games are loaded right now.")
        for (game in games) {
            val gameName = game.arenaName
            val running = LuckyBlockArena.byWorldName(game.worldName)
            if (running != null) {
                val lying = running.getGenerators().sumOf { it.getCount() }
                sender.sendMessage(
                    ChatColor.GREEN.toString() + " " + gameName + ChatColor.GRAY + " [" + game.status + "]: " + running.getGenerators().size +
                        " generators running, " + lying + " lucky block(s) lying around"
                )
                continue
            }
            val plan = GeneratorPlan.forArena(gameName, game.group, game.config.yml)
            sender.sendMessage(
                ChatColor.GRAY.toString() + " " + gameName + " [" + game.status + "]: not running - " + plan.autoReason +
                    (if (plan.isAutoEnabled) "" else ChatColor.YELLOW.toString() + " (fix: /bwlb group add " + plan.group + ")")
            )
        }
    }
}
