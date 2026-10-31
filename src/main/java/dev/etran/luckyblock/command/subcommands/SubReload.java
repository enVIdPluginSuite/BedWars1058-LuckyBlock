package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SubReload extends SubCommand {
  public String getName() {
    return "reload";
  }

  public String getUsage() {
    return "reload";
  }

  public String getDescription() {
    return "Re-reads config.yml, textures.yml, lang.yml, data.yml and the other files";
  }

  public void execute(CommandSender sender, String[] args) {
    LuckyBlocks plugin = LuckyBlocks.getInstance();
    String problem = plugin.reloadAll();
    if (problem != null) {
      sender.sendMessage(ChatColor.RED + "Reload failed: " + problem + ChatColor.GRAY + " (see the console)");
      return;
    }
    sender.sendMessage(ChatColor.GREEN + "Reloaded: " + LuckyBlockTexture.allNames().size() + " lucky block colors, " +
        plugin.getCachedConfig().getLuckyGroups().size() + " lucky group(s).");
    sender.sendMessage(ChatColor.GRAY + "Games that are already running keep their generators; the next game uses the new settings. Problems in the files are listed in the console.");
  }
}
