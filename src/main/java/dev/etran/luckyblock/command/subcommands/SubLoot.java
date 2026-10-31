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
    return "loot [<drop> <team|diamond|emerald> <weight>]";
  }

  public String getDescription() {
    return "Shows how likely each drop is for team, diamond and emerald lucky blocks, or changes a weight";
  }

  public void execute(CommandSender sender, String[] args) {
    if (args.length == 3) {
      String problem = ConfigChanges.setLoot(args[0], args[1], args[2]);
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
    LootTable loot = config.getLoot();
    sender.sendMessage(ChatColor.GOLD + "Lucky block loot " + ChatColor.GRAY + "(chance per block)  " +
        ChatColor.WHITE + "team " + ChatColor.GRAY + "/ " + ChatColor.AQUA + "diamond " + ChatColor.GRAY + "/ " + ChatColor.GREEN + "emerald");
    for (String id : Drop.ids()) {
      sender.sendMessage(ChatColor.YELLOW + " " + id + ChatColor.GRAY + ": " +
          ChatColor.WHITE + percent(loot, GeneratorKind.TEAM, id) + ChatColor.GRAY + " / " +
          ChatColor.AQUA + percent(loot, GeneratorKind.DIAMOND, id) + ChatColor.GRAY + " / " +
          ChatColor.GREEN + percent(loot, GeneratorKind.EMERALD, id));
    }
    sender.sendMessage(ChatColor.GRAY + "Change one with /bwlb loot <drop> <team|diamond|emerald> <weight> (0 = never).");
  }

  public java.util.List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1)
      return filter(Drop.ids(), args[0]);
    if (args.length == 2) {
      java.util.List<String> tiers = new java.util.ArrayList<>();
      for (GeneratorKind tier : GeneratorKind.values())
        tiers.add(tier.getConfigKey());
      return filter(tiers, args[1]);
    }
    return super.tabComplete(sender, args);
  }

  private static String percent(LootTable loot, GeneratorKind tier, String id) {
    return String.format(Locale.ROOT, "%.1f%%", new Object[] { Double.valueOf(loot.chance(tier, id, Drop.ids())) });
  }
}
