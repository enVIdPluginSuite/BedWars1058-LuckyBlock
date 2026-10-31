package dev.etran.luckyblock.command.subcommands

import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.Locale

abstract class SubCommand {
    /** The word after /bwlb, e.g. "list". */
    abstract val name: String

    /** Usage without the leading "/bwlb ", e.g. "list <arena>". */
    abstract val usage: String

    abstract val description: String

    /** True if the command uses the sender's location. */
    open fun requiresPlayer(): Boolean = false

    /** @param args the arguments after the sub command name */
    abstract fun execute(sender: CommandSender, args: Array<String>)

    /** @param args the arguments after the sub command name (the last one is being typed) */
    open fun tabComplete(sender: CommandSender, args: Array<String>): List<String> = emptyList()

    protected fun sendUsage(sender: CommandSender) {
        sender.sendMessage(ChatColor.RED.toString() + "/bwlb " + usage)
    }

    protected companion object {
        /** Sends a clickable message to players, or its plain text to the console. */
        @JvmStatic
        fun send(sender: CommandSender, component: BaseComponent) {
            if (sender is Player) {
                sender.spigot().sendMessage(component)
            } else {
                sender.sendMessage(component.toLegacyText())
            }
        }

        /** Options that start with what the player has typed so far. */
        @JvmStatic
        fun filter(options: Collection<String>, typed: String): MutableList<String> {
            val prefix = typed.lowercase(Locale.ROOT)
            return options.filterTo(ArrayList()) { it.lowercase(Locale.ROOT).startsWith(prefix) }
        }

        /** A cooldown in seconds typed by an admin, or null if it isn't a sensible number. */
        @JvmStatic
        fun parseCooldown(text: String): Int? {
            val seconds = text.trim().toIntOrNull() ?: return null
            return if (seconds in 1..86400) seconds else null
        }
    }
}
