package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.drops.Drop
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Walks through every drop, one at a time, so each can be checked by hand: it runs the drop in front of the
 * player exactly as a broken lucky block would. Use it inside a running game (many drops need one).
 */
class SubDrops : SubCommand() {
    override val name = "drops"
    override val usage = "drops [next|prev|again|first|list|check|<drop>]"
    override val description = "Runs every drop one by one in front of you, to check that each one works"

    override fun requiresPlayer() = true

    /** Where each admin is in the list: the index of the drop run last (-1 = none yet). */
    private val position: MutableMap<UUID, Int> = HashMap()

    override fun execute(sender: CommandSender, args: Array<String>) {
        val player = sender as Player
        val ids = Drop.ids().toList()
        if (ids.isEmpty()) {
            sender.sendMessage(ChatColor.RED.toString() + "There are no drops registered.")
            return
        }
        val current = position[player.uniqueId] ?: -1
        when (val word = if (args.isEmpty()) "next" else args[0].lowercase()) {
            "next" -> run(player, ids, (current + 1).coerceAtMost(ids.size - 1))
            "prev" -> run(player, ids, (current - 1).coerceAtLeast(0))
            "again" -> run(player, ids, current.coerceAtLeast(0))
            "first" -> run(player, ids, 0)
            "list" -> {
                player.sendMessage(ChatColor.GOLD.toString() + "Drops (" + ids.size + "):")
                player.sendMessage(ChatColor.YELLOW.toString() + ids.joinToString(ChatColor.GRAY.toString() + ", " + ChatColor.YELLOW))
            }
            "check" -> check(player, ids)
            else -> {
                val index = ids.indexOf(word)
                if (index < 0) {
                    player.sendMessage(ChatColor.RED.toString() + "No drop called '" + word + "'. See /bwlb drops list")
                    return
                }
                run(player, ids, index)
            }
        }
    }

    /** A free spot two blocks in front of the player. */
    private fun spot(player: Player): Location {
        val direction = player.location.direction.setY(0.0)
        if (direction.lengthSquared() > 0.0) direction.normalize() else direction.setX(1.0)
        val at = player.location.clone().add(direction.multiply(2.0))
        at.x = at.blockX + 0.5
        at.z = at.blockZ + 0.5
        at.y = at.blockY.toDouble()
        return if (at.block.type.isSolid) player.location.clone() else at
    }

    /** Runs one drop. Returns the error, or null if it ran without throwing. */
    private fun fire(player: Player, id: String): Throwable? {
        val drop = Drop.get(id) ?: return IllegalStateException("not registered")
        return try {
            drop.drop(spot(player), player)
            null
        } catch (e: Throwable) {
            LuckyBlocks.getInstance().logger.log(java.util.logging.Level.SEVERE, "Drop '$id' failed", e)
            e
        }
    }

    private fun run(player: Player, ids: List<String>, index: Int) {
        position[player.uniqueId] = index
        val id = ids[index]
        player.sendMessage(ChatColor.GOLD.toString() + "Drop " + (index + 1) + "/" + ids.size + ": " + ChatColor.YELLOW + id)
        val error = fire(player, id)
        if (error != null) {
            player.sendMessage(ChatColor.RED.toString() + "  FAILED: " + error.javaClass.simpleName + " " + (error.message ?: "") + " (see the console)")
        }
        val line = TextComponent("")
        line.addExtra(button("[<< Prev]", "/bwlb drops prev", "Run the previous drop"))
        line.addExtra(TextComponent("  "))
        line.addExtra(button("[Again]", "/bwlb drops again", "Run this drop once more"))
        line.addExtra(TextComponent("  "))
        line.addExtra(button("[Next >>]", "/bwlb drops next", "Run the next drop"))
        send(player, line)
    }

    /** Runs every drop once and reports the ones that threw. Nothing is undone, so use it on a test arena. */
    private fun check(player: Player, ids: List<String>) {
        var failed = 0
        for (id in ids) {
            val error = fire(player, id)
            if (error != null) {
                failed++
                player.sendMessage(ChatColor.RED.toString() + id + ": " + error.javaClass.simpleName + " " + (error.message ?: ""))
            }
        }
        player.sendMessage(ChatColor.GOLD.toString() + "Ran " + ids.size + " drops: " + (ids.size - failed) + " without errors, " + failed + " failed.")
    }

    private fun button(label: String, command: String, hover: String): TextComponent {
        val button = TextComponent(*TextComponent.fromLegacyText(ChatColor.AQUA.toString() + label))
        button.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
        button.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover))
        return button
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) return filter(listOf("next", "prev", "again", "first", "list", "check") + Drop.ids(), args[0])
        return super.tabComplete(sender, args)
    }
}
