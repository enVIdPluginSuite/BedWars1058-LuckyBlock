package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.arena.LoadedArena
import dev.etran.luckyblock.generator.GeneratorPlan
import dev.etran.luckyblock.utils.SetupUtils
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class SubRemoveGenerator : SubCommand() {
    override val name = "removegenerator"
    override val usage = "removegenerator <arena> [id]"
    override val description = "Removes a generator by id (see list), or the one within 4 blocks of you"

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty() || args.size > 2) {
            sendUsage(sender)
            return
        }

        // a manual generator by number: works even if the arena file is gone
        if (args.size == 2 && args[1].matches(Regex("\\d+"))) {
            val arenaName = SetupUtils.canonicalArenaName(args[0]) ?: args[0]
            val id = args[1].toInt()
            val generator = LoadedArena.byName(arenaName)?.getGeneratorById(id)
            if (generator == null || !SetupUtils.removeGenerator(arenaName, id)) {
                sender.sendMessage(ChatColor.RED.toString() + "There is no manual generator " + id + " in " + arenaName + ". Look at /bwlb list " + arenaName)
                return
            }
            sender.sendMessage(
                ChatColor.GREEN.toString() + "Removed " + ChatColor.WHITE + generator.describe() + ChatColor.GREEN + " generator " +
                    ChatColor.GRAY + "[" + arenaName + "-" + id + "]"
            )
            return
        }

        val arena = ArenaContext.load(sender, args[0]) ?: return
        val entry: GeneratorPlan.Entry
        if (args.size == 2) {
            val found = arena.plan.byDisplayId(args[1])
            if (found == null || !found.spec.isAuto) {
                sender.sendMessage(ChatColor.RED.toString() + "There is no generator \"" + args[1] + "\" in " + arena.name + ". Look at /bwlb list " + arena.name)
                return
            }
            if (found.status == GeneratorPlan.Status.EXCLUDED) {
                sender.sendMessage(
                    ChatColor.RED.toString() + "[" + args[1] + "] is already removed. Bring it back with /bwlb restore " + arena.name + " " + found.spec.displayId
                )
                return
            }
            entry = found
        } else {
            if (sender !is Player) {
                sendUsage(sender)
                return
            }
            entry = nearest(arena.plan, sender.location) ?: run {
                sender.sendMessage(ChatColor.RED.toString() + "No generator within 4 blocks of you. Use /bwlb show " + arena.name + " to find them, or /bwlb list " + arena.name)
                return
            }
        }

        val generator = entry.spec
        if (generator.isAuto) {
            LuckyBlocks.getInstance().arenaOverrides.exclude(arena.name, generator.blockKey)
            sender.sendMessage(
                ChatColor.GREEN.toString() + "Removed automatic " + ChatColor.WHITE + generator.describe() + ChatColor.GREEN + " generator " +
                    ChatColor.GRAY + "[" + arena.name + "-" + generator.displayId + "]" +
                    ChatColor.GREEN + ". Bring it back with /bwlb restore " + arena.name + " " + generator.displayId
            )
        } else {
            SetupUtils.removeGenerator(arena.name, generator.id)
            sender.sendMessage(
                ChatColor.GREEN.toString() + "Removed " + ChatColor.WHITE + generator.describe() + ChatColor.GREEN + " generator " +
                    ChatColor.GRAY + "[" + arena.name + "-" + generator.displayId + "]"
            )
        }
    }

    /** The closest running generator within 4 blocks. Coordinates only, so it works in the arena's setup world too. */
    private fun nearest(plan: GeneratorPlan.Result, from: Location): GeneratorPlan.Entry? {
        var best: GeneratorPlan.Entry? = null
        var bestDistance = NEAREST_RADIUS_SQUARED
        for (entry in plan.entries) {
            if (!entry.isActive) continue
            val generator = entry.spec
            val dx = generator.x - from.x
            val dy = generator.y - from.y
            val dz = generator.z - from.z
            val distance = dx * dx + dy * dy + dz * dz
            if (distance < bestDistance) {
                bestDistance = distance
                best = entry
            }
        }
        return best
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) return filter(SetupUtils.arenaNames(), args[0])
        if (args.size == 2) {
            val ids = ArrayList<String>()
            val name = SetupUtils.canonicalArenaName(args[0])
            if (name != null) {
                val yml = SetupUtils.loadArenaYml(name)
                val plan = GeneratorPlan.forArena(name, SetupUtils.groupOf(name, yml), yml)
                for (entry in plan.entries) {
                    if (entry.isActive) ids.add(entry.spec.displayId)
                }
            }
            return filter(ids, args[1])
        }
        return super.tabComplete(sender, args)
    }

    private companion object {
        const val NEAREST_RADIUS_SQUARED = 16.0
    }
}
