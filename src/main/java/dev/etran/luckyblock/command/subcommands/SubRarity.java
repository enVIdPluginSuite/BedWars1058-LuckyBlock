package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.data.CachedConfig;
import dev.etran.luckyblock.drops.Rarity;
import dev.etran.luckyblock.generator.GeneratorKind;
import dev.etran.luckyblock.utils.ColorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Shows the chance of each rarity for team, diamond and emerald lucky blocks. */
public class SubRarity extends SubCommand {
  public String getName() {
    return "rarity";
  }

  public String getUsage() {
    return "rarity [<team|diamond|emerald> <rarity> <weight>]";
  }

  public String getDescription() {
    return "Shows the rarity roll of each generator kind, or changes one";
  }

  public void execute(CommandSender sender, String[] args) {
    if (args.length == 3) {
      String problem = ConfigChanges.setRarityChance(args[0], args[1], args[2]);
      if (problem != null) {
        sender.sendMessage(ChatColor.RED + problem);
        return;
      }
      sender.sendMessage(ChatColor.GREEN + "Saved to config.yml. " + ChatColor.GRAY + "It applies right away.");
    } else if (args.length != 0) {
      sendUsage(sender);
      return;
    }
    CachedConfig config = LuckyBlocks.getInstance().getCachedConfig();
    sender.sendMessage(ChatColor.GOLD + "Rarity of lucky blocks " + ChatColor.GRAY + "(roll weights, % of total)");
    for (GeneratorKind kind : GeneratorKind.values()) {
      StringBuilder line = new StringBuilder(ChatColor.YELLOW + " " + kind.getConfigKey() + ChatColor.GRAY + ":");
      for (Rarity rarity : Rarity.values()) {
        double percent = config.getRarityChance(kind, rarity);
        line.append(' ').append(ColorUtils.colorize(config.getRarity(rarity).getDisplay())).append(ChatColor.WHITE)
            .append(' ').append(Math.round(percent * 10.0D) / 10.0D).append('%').append(ChatColor.GRAY).append(',');
      }
      sender.sendMessage(line.substring(0, line.length() - 1));
    }
    sender.sendMessage(ChatColor.GRAY + "Lucky blocks near the middle of a map roll a better rarity: up to "
        + config.getMiddleUpgrade() + "% chance of a one-step upgrade (set middle-upgrade).");
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      List<String> kinds = new ArrayList<>();
      for (GeneratorKind kind : GeneratorKind.values())
        kinds.add(kind.getConfigKey());
      return filter(kinds, args[0]);
    }
    if (args.length == 2) {
      List<String> keys = new ArrayList<>();
      for (Rarity rarity : Rarity.values())
        keys.add(rarity.getKey());
      return filter(keys, args[1]);
    }
    return super.tabComplete(sender, args);
  }
}
