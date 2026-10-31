package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.generator.GeneratorPlan
import dev.etran.luckyblock.utils.SetupUtils
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import java.util.Locale

class SubList : SubCommand() {
    override val name = "list"
    override val usage = "list <arena>"
    override val description = "Shows every lucky block generator of an arena"

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.size != 1) {
            sendUsage(sender)
            return
        }
        val arena = ArenaContext.load(sender, args[0]) ?: return
        val plan = arena.plan
        sender.sendMessage(ChatColor.GOLD.toString() + "Lucky block generators of " + ChatColor.AQUA + arena.name + ChatColor.GRAY + " (BedWars group: " + arena.group + ")")
        sender.sendMessage(
            ChatColor.GRAY.toString() + "Automatic generators: " +
                (if (plan.isAutoEnabled) ChatColor.GREEN.toString() + "ON" else ChatColor.RED.toString() + "OFF") +
                ChatColor.GRAY + " - " + plan.autoReason
        )
        if (plan.entries.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW.toString() + "This arena has no generators yet. Add one with /bwlb addgenerator " + arena.name + " <team|diamond|emerald>")
            return
        }
        for (entry in plan.entries) sendEntry(sender, arena.name, entry)
        sender.sendMessage(ChatColor.GRAY.toString() + "Tip: /bwlb show " + arena.name + " labels them in the world.")
    }

    private fun sendEntry(sender: CommandSender, arena: String, entry: GeneratorPlan.Entry) {
        val generator = entry.spec
        val color = if (entry.isActive) ChatColor.GREEN else if (entry.status == GeneratorPlan.Status.EXCLUDED) ChatColor.RED else ChatColor.DARK_GRAY
        val text = color.toString() + " [" + generator.displayId + "] " + ChatColor.WHITE + generator.describe() +
            ChatColor.GRAY + String.format(Locale.ROOT, " at %.1f %.1f %.1f", generator.x, generator.y, generator.z) +
            ChatColor.GRAY + ", every " + generator.cooldown + "s" +
            (if (generator.isAuto) "" else ChatColor.AQUA.toString() + " (manual)") +
            (if (entry.isActive) "" else ChatColor.GRAY.toString() + " - " + entry.status.text) + " "
        val line = TextComponent(*TextComponent.fromLegacyText(text))
        if (entry.status == GeneratorPlan.Status.EXCLUDED) {
            line.addExtra(button(ChatColor.GREEN.toString() + "[Restore]", "/bwlb restore $arena ${generator.displayId}", "Bring this generator back"))
        } else if (entry.isActive) {
            line.addExtra(button(ChatColor.RED.toString() + "[Remove]", "/bwlb removegenerator $arena ${generator.displayId}", "Remove this generator"))
        }
        send(sender, line)
    }

    private fun button(label: String, command: String, hover: String): TextComponent {
        val button = TextComponent(*TextComponent.fromLegacyText(label))
        button.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, command)
        button.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover))
        return button
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> =
        if (args.size == 1) filter(SetupUtils.arenaNames(), args[0]) else super.tabComplete(sender, args)
}
