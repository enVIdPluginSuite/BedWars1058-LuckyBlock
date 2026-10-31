package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.compat.Compat
import dev.etran.luckyblock.generator.GeneratorPlan
import dev.etran.luckyblock.utils.SetupUtils
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player

/** Floats a label above every generator of an arena for a few seconds, so you can see where they are. */
class SubShow : SubCommand() {
    override val name = "show"
    override val usage = "show <arena>"
    override val description = "Labels the generators in the world for 15 seconds (be in the arena's world)"

    override fun requiresPlayer(): Boolean = true

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.size != 1) {
            sendUsage(sender)
            return
        }
        val arena = ArenaContext.load(sender, args[0]) ?: return
        val player = sender as Player
        val world = player.world
        val labels = ArrayList<ArmorStand>()
        for (entry in arena.plan.entries) {
            val generator = entry.spec
            val color = if (entry.isActive) ChatColor.GREEN else if (entry.status == GeneratorPlan.Status.EXCLUDED) ChatColor.RED else ChatColor.GRAY
            val label = world.spawnEntity(Location(world, generator.x, generator.y + 1.2, generator.z), EntityType.ARMOR_STAND) as ArmorStand
            label.isVisible = false
            label.setGravity(false)
            label.setMarker(true)
            Compat.setIfPresent(label, "setInvulnerable", true)
            label.isSmall = true
            Compat.setIfPresent(label, "setPersistent", false)
            label.customName = color.toString() + "[" + generator.displayId + "] " + generator.describe() +
                (if (entry.isActive) "" else " (" + entry.status.text + ")")
            label.isCustomNameVisible = true
            labels.add(label)
        }
        if (labels.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW.toString() + "This arena has no generators to show.")
            return
        }
        player.sendMessage(
            ChatColor.GREEN.toString() + "Showing " + labels.size + " generator labels in " + ChatColor.AQUA + world.name + ChatColor.GREEN +
                " for 15 seconds." + ChatColor.GRAY + " (Green = running, red = removed, gray = inactive)"
        )
        LuckyBlocks.getInstance().server.scheduler.runTaskLater(LuckyBlocks.getInstance(), Runnable {
            for (label in labels) label.remove()
        }, SHOW_TICKS)
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> =
        if (args.size == 1) filter(SetupUtils.arenaNames(), args[0]) else super.tabComplete(sender, args)

    private companion object {
        const val SHOW_TICKS = 300L
    }
}
