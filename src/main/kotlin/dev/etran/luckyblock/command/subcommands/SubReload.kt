package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender

class SubReload : SubCommand() {
    override val name = "reload"
    override val usage = "reload"
    override val description = "Re-reads config.yml, textures.yml, lang.yml, data.yml and the other files"

    override fun execute(sender: CommandSender, args: Array<String>) {
        val plugin = LuckyBlocks.getInstance()
        val problem = plugin.reloadAll()
        if (problem != null) {
            sender.sendMessage(ChatColor.RED.toString() + "Reload failed: " + problem + ChatColor.GRAY + " (see the console)")
            return
        }
        sender.sendMessage(
            ChatColor.GREEN.toString() + "Reloaded: " + LuckyBlockTexture.allNames().size + " lucky block colors, " +
                plugin.cachedConfig.getLuckyGroups().size + " lucky group(s)."
        )
        sender.sendMessage(
            ChatColor.GRAY.toString() + "Games that are already running keep their generators; the next game uses the new settings. Problems in the files are listed in the console."
        )
    }
}
