package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.generator.GeneratorPlan;
import dev.etran.luckyblock.generator.LoadedGenerator;
import dev.etran.luckyblock.utils.SetupUtils;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Copies the automatic generators of an arena into data.yml as manual generators. You only need this
 * for arenas outside the lucky block groups, or to take over an arena and edit every generator yourself.
 */
public class SubAutoSetup extends SubCommand {
  public String getName() {
    return "autosetup";
  }

  public String getUsage() {
    return "autosetup <arena> [cooldown]";
  }

  public String getDescription() {
    return "Turns an arena's team/diamond/emerald generators into manual ones (works for any group)";
  }

  public void execute(CommandSender sender, String[] args) {
    if (args.length < 1 || args.length > 2) {
      sendUsage(sender);
      return;
    }
    Integer cooldown = null;
    if (args.length == 2) {
      cooldown = parseCooldown(args[1]);
      if (cooldown == null) {
        sender.sendMessage(ChatColor.RED + "Please provide the cooldown as a number of seconds (1 or more).");
        return;
      }
    }
    ArenaContext arena = ArenaContext.load(sender, args[0]);
    if (arena == null)
      return;
    int added = 0;
    for (GeneratorPlan.Entry entry : arena.plan.getEntries()) {
      LoadedGenerator spot = entry.getSpec();
      // only the automatic spots that aren't removed and don't already have a manual generator
      if (!spot.isAuto() || entry.getStatus() == GeneratorPlan.Status.EXCLUDED || entry.getStatus() == GeneratorPlan.Status.REPLACED)
        continue;
      SetupUtils.addGenerator(arena.name, spot.getKind(), spot.getTeam(), spot.getX(), spot.getY(), spot.getZ(), (cooldown == null) ? spot.getCooldown() : cooldown.intValue());
      added++;
    }
    if (added == 0) {
      sender.sendMessage(ChatColor.YELLOW + "Nothing to set up: " + arena.name + " has no generators that aren't already manual or removed.");
      return;
    }
    sender.sendMessage(ChatColor.GREEN + "Auto-Setup completed for " + ChatColor.AQUA + arena.name + ChatColor.GREEN + ": " + added + " manual generators created. See /bwlb list " + arena.name);
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    return (args.length == 1) ? filter(SetupUtils.arenaNames(), args[0]) : super.tabComplete(sender, args);
  }
}
