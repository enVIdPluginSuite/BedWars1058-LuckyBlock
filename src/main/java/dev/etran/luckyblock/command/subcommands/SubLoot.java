package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.data.CachedConfig;
import dev.etran.luckyblock.drops.Drop;
import dev.etran.luckyblock.drops.LootTable;
import dev.etran.luckyblock.drops.Rarity;
import dev.etran.luckyblock.utils.ColorUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Shows the loot table of each lucky block rarity with the percentage of every drop, or changes a weight. */
public class SubLoot extends SubCommand {
  public String getName() {
    return "loot";
  }

  public String getUsage() {
    return "loot [<rarity> [<drop> <weight>]]";
  }

  public String getDescription() {
    return "Shows how likely each drop is for a rarity (common, uncommon, rare, legendary), or changes a weight";
  }

  public void execute(CommandSender sender, String[] args) {
    CachedConfig config = LuckyBlocks.getInstance().getCachedConfig();
    LootTable loot = config.getLoot();
    if (args.length == 0) {
      sender.sendMessage(ChatColor.GOLD + "Lucky block loot " + ChatColor.GRAY + "(" + Drop.ids().size() + " drops)");
      for (Rarity rarity : Rarity.values()) {
        int usable = 0;
        for (String id : Drop.ids()) {
          if (loot.getWeight(rarity, id) > 0)
            usable++;
        }
        sender.sendMessage(" " + ColorUtils.colorize(config.getRarity(rarity).getDisplay()) + ChatColor.GRAY + ": " + usable + " drops");
      }
      sender.sendMessage(ChatColor.GRAY + "See one with /bwlb loot <rarity>, change with /bwlb loot <rarity> <drop> <weight> (0 = never).");
      return;
    }
    Rarity rarity = Rarity.fromKey(args[0]);
    if (rarity == null) {
      sender.sendMessage(ChatColor.RED + "The rarity must be common, uncommon, rare or legendary.");
      return;
    }
    if (args.length == 3) {
      String problem = ConfigChanges.setLoot(rarity, args[1], args[2]);
      if (problem != null) {
        sender.sendMessage(ChatColor.RED + problem);
        return;
      }
      sender.sendMessage(ChatColor.GREEN + "Saved to config.yml. " + ChatColor.GRAY + "It applies right away.");
      config = LuckyBlocks.getInstance().getCachedConfig();
      loot = config.getLoot();
    } else if (args.length != 1) {
      sendUsage(sender);
      return;
    }
    sender.sendMessage(ColorUtils.colorize(config.getRarity(rarity).getDisplay()) + ChatColor.GOLD + " loot " + ChatColor.GRAY + "(chance per lucky block)");
    List<String> ids = new ArrayList<>(Drop.ids());
    final LootTable table = loot;
    final Rarity r = rarity;
    ids.sort((a, b) -> Integer.compare(table.getWeight(r, b), table.getWeight(r, a)));
    for (String id : ids) {
      int weight = loot.getWeight(rarity, id);
      if (weight <= 0)
        continue;
      sender.sendMessage(ChatColor.YELLOW + " " + id + ChatColor.GRAY + ": " + ChatColor.WHITE +
          String.format(Locale.ROOT, "%.1f%%", Double.valueOf(loot.chance(rarity, id, Drop.ids()))) + ChatColor.GRAY + " (weight " + weight + ")");
    }
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      List<String> keys = new ArrayList<>();
      for (Rarity rarity : Rarity.values())
        keys.add(rarity.getKey());
      return filter(keys, args[0]);
    }
    if (args.length == 2)
      return filter(Drop.ids(), args[1]);
    return super.tabComplete(sender, args);
  }
}
