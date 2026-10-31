package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.generator.GeneratorPlan;
import dev.etran.luckyblock.utils.SetupUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SubRestore extends SubCommand {
  public String getName() {
    return "restore";
  }

  public String getUsage() {
    return "restore <arena> <id|all>";
  }

  public String getDescription() {
    return "Brings back automatic generators you removed";
  }

  public void execute(CommandSender sender, String[] args) {
    if (args.length != 2) {
      sendUsage(sender);
      return;
    }
    if (args[1].toLowerCase(Locale.ROOT).equals("all")) {
      String name = SetupUtils.canonicalArenaName(args[0]);
      int restored = LuckyBlocks.getInstance().getArenaOverrides().restoreAll((name == null) ? args[0] : name);
      sender.sendMessage((restored == 0) ? (ChatColor.YELLOW + "No removed generators to restore in " + args[0] + ".") : (ChatColor.GREEN + "Restored " + restored + " automatic generator" + ((restored == 1) ? "" : "s") + " in " + args[0] + "."));
      return;
    }
    ArenaContext arena = ArenaContext.load(sender, args[0]);
    if (arena == null)
      return;
    GeneratorPlan.Entry entry = arena.plan.byDisplayId(args[1]);
    if (entry == null || entry.getStatus() != GeneratorPlan.Status.EXCLUDED || !LuckyBlocks.getInstance().getArenaOverrides().restore(arena.name, entry.getSpec().getBlockKey())) {
      sender.sendMessage(ChatColor.RED + "\"" + args[1] + "\" is not a removed generator of " + arena.name + ". Removed ones are shown in red in /bwlb list " + arena.name);
      return;
    }
    sender.sendMessage(ChatColor.GREEN + "Restored " + ChatColor.WHITE + entry.getSpec().describe() + ChatColor.GREEN + " generator " + ChatColor.GRAY + "[" + arena.name + "-" + entry.getSpec().getDisplayId() + "]" + ChatColor.GREEN + ".");
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1)
      return filter(SetupUtils.arenaNames(), args[0]);
    if (args.length == 2) {
      List<String> ids = new ArrayList<>();
      ids.add("all");
      String name = SetupUtils.canonicalArenaName(args[0]);
      if (name != null) {
        GeneratorPlan.Result plan = GeneratorPlan.forArena(name, SetupUtils.groupOf(name, SetupUtils.loadArenaYml(name)), SetupUtils.loadArenaYml(name));
        for (GeneratorPlan.Entry entry : plan.getEntries()) {
          if (entry.getStatus() == GeneratorPlan.Status.EXCLUDED)
            ids.add(entry.getSpec().getDisplayId());
        }
      }
      return filter(ids, args[1]);
    }
    return super.tabComplete(sender, args);
  }
}
