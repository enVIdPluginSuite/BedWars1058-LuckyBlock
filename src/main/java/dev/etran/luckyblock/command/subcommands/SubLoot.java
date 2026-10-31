package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.data.CachedConfig;
import dev.etran.luckyblock.drops.Drop;
import dev.etran.luckyblock.drops.LootTable;
import dev.etran.luckyblock.generator.GeneratorKind;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Shows the chance of every drop for team, diamond and emerald lucky blocks, to help tune config.yml. */
public class SubLoot extends SubCommand {
  public String getName() {
    return "loot";
  }

  public String getUsage() {
    return "loot";
  }

  public String getDescription() {
    return "Shows how likely each drop is for team, diamond and emerald lucky blocks";
  }

  public void execute(CommandSender sender, String[] args) {
    CachedConfig config = LuckyBlocks.getInstance().getCachedConfig();
    LootTable loot = config.getLoot();
    sender.sendMessage(ChatColor.GOLD + "Lucky block loot " + ChatColor.GRAY + "(chance per block)  " +
        ChatColor.WHITE + "team " + ChatColor.GRAY + "/ " + ChatColor.AQUA + "diamond " + ChatColor.GRAY + "/ " + ChatColor.GREEN + "emerald");
    for (String id : Drop.ids()) {
      sender.sendMessage(ChatColor.YELLOW + " " + id + ChatColor.GRAY + ": " +
          ChatColor.WHITE + percent(loot, GeneratorKind.TEAM, id) + ChatColor.GRAY + " / " +
          ChatColor.AQUA + percent(loot, GeneratorKind.DIAMOND, id) + ChatColor.GRAY + " / " +
          ChatColor.GREEN + percent(loot, GeneratorKind.EMERALD, id));
    }
    sender.sendMessage(ChatColor.GRAY + "Change the weights under \"loot:\" in config.yml (needs a restart).");
  }

  private static String percent(LootTable loot, GeneratorKind tier, String id) {
    return String.format(Locale.ROOT, "%.1f%%", new Object[] { Double.valueOf(loot.chance(tier, id, Drop.ids())) });
  }
}
