package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.arena.LoadedArena
import dev.etran.luckyblock.generator.GeneratorKind
import dev.etran.luckyblock.generator.LoadedGenerator
import dev.etran.luckyblock.utils.SetupUtils
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

class SubAddGenerator : SubCommand() {
    override val name = "addgenerator"
    override val usage = "addgenerator <arena> <team|diamond|emerald> [cooldown]"
    override val description = "Adds a lucky block generator where you stand (for a team: use its name)"

    override fun requiresPlayer(): Boolean = true

    override fun execute(sender: CommandSender, args: Array<String>) {
        val player = sender as Player
        if (args.size < 2 || args.size > 3) {
            sendUsage(sender)
            return
        }
        val arena = ArenaContext.load(sender, args[0]) ?: return

        // "diamond" / "emerald", or the name of one of the arena's teams
        var kind = GeneratorKind.fromCommandWord(args[1])
        var team: String? = null
        if (kind == null) {
            kind = GeneratorKind.TEAM
            for (teamName in SetupUtils.teamNames(arena.yml)) {
                if (teamName.equals(args[1], ignoreCase = true)) team = teamName
            }
            if (team == null) {
                sender.sendMessage(ChatColor.RED.toString() + "\"" + args[1] + "\" is not diamond, emerald or a team of " + arena.name + ".")
                sender.sendMessage(ChatColor.GRAY.toString() + "Teams: " + SetupUtils.teamNames(arena.yml).joinToString(", "))
                return
            }
        }

        var cooldown = LuckyBlocks.getInstance().cachedConfig.getKind(kind).cooldown
        if (args.size == 3) {
            val typed = parseCooldown(args[2])
            if (typed == null) {
                sender.sendMessage(ChatColor.RED.toString() + "Please provide the cooldown as a number of seconds (1 or more).")
                return
            }
            cooldown = typed
        }

        // snap to the middle of the block the player stands in
        val at = player.location
        val x = at.blockX + 0.5
        val y = at.blockY.toDouble()
        val z = at.blockZ + 0.5
        val existing = LoadedArena.byName(arena.name)?.getGeneratorAtBlock(LoadedGenerator.blockKey(x, y, z))
        if (existing != null) {
            sender.sendMessage(ChatColor.RED.toString() + "There is already a generator here: [" + existing.displayId + "] " + existing.describe() + ". Remove it first, or move.")
            return
        }

        val generator = SetupUtils.addGenerator(arena.name, kind, team, x, y, z, cooldown)
        sender.sendMessage(
            ChatColor.GREEN.toString() + "Created " + ChatColor.WHITE + generator.describe() + ChatColor.GREEN + " lucky block generator " +
                ChatColor.GRAY + "[" + arena.name + "-" + generator.displayId + "]" +
                ChatColor.GREEN + ", every " + cooldown + "s. It starts working in the next game."
        )
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) return filter(SetupUtils.arenaNames(), args[0])
        if (args.size == 2) {
            val options = ArrayList<String>()
            options.add("diamond")
            options.add("emerald")
            val arenaName = SetupUtils.canonicalArenaName(args[0])
            if (arenaName != null) options.addAll(SetupUtils.teamNames(SetupUtils.loadArenaYml(arenaName)))
            return filter(options, args[1].lowercase(Locale.ROOT))
        }
        return super.tabComplete(sender, args)
    }
}
