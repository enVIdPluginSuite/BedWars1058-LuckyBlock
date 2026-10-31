package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.arena.LoadedArena;
import dev.etran.luckyblock.generator.GeneratorPlan;
import dev.etran.luckyblock.generator.LoadedGenerator;
import dev.etran.luckyblock.utils.SetupUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubRemoveGenerator extends SubCommand {
  private static final double NEAREST_RADIUS_SQUARED = 16.0D;

  public String getName() {
    return "removegenerator";
  }

  public String getUsage() {
    return "removegenerator <arena> [id]";
  }

  public String getDescription() {
    return "Removes a generator by id (see list), or the one within 4 blocks of you";
  }

  public void execute(CommandSender sender, String[] args) {
    if (args.length < 1 || args.length > 2) {
      sendUsage(sender);
      return;
    }

    // a manual generator by number: works even if the arena file is gone
    if (args.length == 2 && args[1].matches("\\d+")) {
      String name = SetupUtils.canonicalArenaName(args[0]);
      String arenaName = (name == null) ? args[0] : name;
      int id = Integer.parseInt(args[1]);
      LoadedArena manual = LoadedArena.byName(arenaName);
      LoadedGenerator generator = (manual == null) ? null : manual.getGeneratorById(id);
      if (generator == null || !SetupUtils.removeGenerator(arenaName, id)) {
        sender.sendMessage(ChatColor.RED + "There is no manual generator " + id + " in " + arenaName + ". Look at /luckyblock list " + arenaName);
        return;
      }
      sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.WHITE + generator.describe() + ChatColor.GREEN + " generator " + ChatColor.GRAY + "[" + arenaName + "-" + id + "]");
      return;
    }

    ArenaContext arena = ArenaContext.load(sender, args[0]);
    if (arena == null)
      return;
    GeneratorPlan.Entry entry;
    if (args.length == 2) {
      entry = arena.plan.byDisplayId(args[1]);
      if (entry == null || !entry.getSpec().isAuto()) {
        sender.sendMessage(ChatColor.RED + "There is no generator \"" + args[1] + "\" in " + arena.name + ". Look at /luckyblock list " + arena.name);
        return;
      }
      if (entry.getStatus() == GeneratorPlan.Status.EXCLUDED) {
        sender.sendMessage(ChatColor.RED + "[" + args[1] + "] is already removed. Bring it back with /luckyblock restore " + arena.name + " " + entry.getSpec().getDisplayId());
        return;
      }
    } else {
      if (!(sender instanceof Player)) {
        sendUsage(sender);
        return;
      }
      entry = nearest(arena.plan, ((Player)sender).getLocation());
      if (entry == null) {
        sender.sendMessage(ChatColor.RED + "No generator within 4 blocks of you. Use /luckyblock show " + arena.name + " to find them, or /luckyblock list " + arena.name);
        return;
      }
    }

    LoadedGenerator generator = entry.getSpec();
    if (generator.isAuto()) {
      LuckyBlocks.getInstance().getArenaOverrides().exclude(arena.name, generator.getBlockKey());
      sender.sendMessage(ChatColor.GREEN + "Removed automatic " + ChatColor.WHITE + generator.describe() + ChatColor.GREEN + " generator " + ChatColor.GRAY + "[" + arena.name + "-" + generator.getDisplayId() + "]" +
          ChatColor.GREEN + ". Bring it back with /luckyblock restore " + arena.name + " " + generator.getDisplayId());
    } else {
      SetupUtils.removeGenerator(arena.name, generator.getId());
      sender.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.WHITE + generator.describe() + ChatColor.GREEN + " generator " + ChatColor.GRAY + "[" + arena.name + "-" + generator.getDisplayId() + "]");
    }
  }

  /** The closest running generator within 4 blocks. Coordinates only, so it works in the arena's setup world too. */
  private static GeneratorPlan.Entry nearest(GeneratorPlan.Result plan, Location from) {
    GeneratorPlan.Entry best = null;
    double bestDistance = NEAREST_RADIUS_SQUARED;
    for (GeneratorPlan.Entry entry : plan.getEntries()) {
      if (!entry.isActive())
        continue;
      LoadedGenerator generator = entry.getSpec();
      double dx = generator.getX() - from.getX();
      double dy = generator.getY() - from.getY();
      double dz = generator.getZ() - from.getZ();
      double distance = dx * dx + dy * dy + dz * dz;
      if (distance < bestDistance) {
        bestDistance = distance;
        best = entry;
      }
    }
    return best;
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1)
      return filter(SetupUtils.arenaNames(), args[0]);
    if (args.length == 2) {
      List<String> ids = new ArrayList<>();
      String name = SetupUtils.canonicalArenaName(args[0]);
      if (name != null) {
        GeneratorPlan.Result plan = GeneratorPlan.forArena(name, SetupUtils.groupOf(name, SetupUtils.loadArenaYml(name)), SetupUtils.loadArenaYml(name));
        for (GeneratorPlan.Entry entry : plan.getEntries()) {
          if (entry.isActive())
            ids.add(entry.getSpec().getDisplayId());
        }
      }
      return filter(ids, args[1]);
    }
    return super.tabComplete(sender, args);
  }
}
