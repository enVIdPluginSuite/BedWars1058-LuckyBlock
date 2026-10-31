package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.arena.LoadedArena;
import dev.etran.luckyblock.generator.GeneratorKind;
import dev.etran.luckyblock.generator.LoadedGenerator;
import dev.etran.luckyblock.utils.SetupUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubAddGenerator extends SubCommand {
  public String getName() {
    return "addgenerator";
  }

  public String getUsage() {
    return "addgenerator <arena> <team|diamond|emerald> [cooldown]";
  }

  public String getDescription() {
    return "Adds a lucky block generator where you stand (for a team: use its name)";
  }

  public boolean requiresPlayer() {
    return true;
  }

  public void execute(CommandSender sender, String[] args) {
    Player player = (Player)sender;
    if (args.length < 2 || args.length > 3) {
      sendUsage(sender);
      return;
    }
    ArenaContext arena = ArenaContext.load(sender, args[0]);
    if (arena == null)
      return;

    // "diamond" / "emerald", or the name of one of the arena's teams
    GeneratorKind kind = GeneratorKind.fromCommandWord(args[1]);
    String team = null;
    if (kind == null) {
      kind = GeneratorKind.TEAM;
      for (String name : SetupUtils.teamNames(arena.yml)) {
        if (name.equalsIgnoreCase(args[1]))
          team = name;
      }
      if (team == null) {
        sender.sendMessage(ChatColor.RED + "\"" + args[1] + "\" is not diamond, emerald or a team of " + arena.name + ".");
        sender.sendMessage(ChatColor.GRAY + "Teams: " + String.join(", ", (Iterable<? extends CharSequence>)SetupUtils.teamNames(arena.yml)));
        return;
      }
    }

    int cooldown = LuckyBlocks.getInstance().getCachedConfig().getKind(kind).getCooldown();
    if (args.length == 3) {
      Integer typed = parseCooldown(args[2]);
      if (typed == null) {
        sender.sendMessage(ChatColor.RED + "Please provide the cooldown as a number of seconds (1 or more).");
        return;
      }
      cooldown = typed.intValue();
    }

    // snap to the middle of the block the player stands in
    Location at = player.getLocation();
    double x = at.getBlockX() + 0.5D;
    double y = at.getBlockY();
    double z = at.getBlockZ() + 0.5D;
    LoadedArena manual = LoadedArena.byName(arena.name);
    LoadedGenerator existing = (manual == null) ? null : manual.getGeneratorAtBlock(LoadedGenerator.blockKey(x, y, z));
    if (existing != null) {
      sender.sendMessage(ChatColor.RED + "There is already a generator here: [" + existing.getDisplayId() + "] " + existing.describe() + ". Remove it first, or move.");
      return;
    }

    LoadedGenerator generator = SetupUtils.addGenerator(arena.name, kind, team, x, y, z, cooldown);
    sender.sendMessage(ChatColor.GREEN + "Created " + ChatColor.WHITE + generator.describe() + ChatColor.GREEN + " lucky block generator " + ChatColor.GRAY + "[" + arena.name + "-" + generator.getDisplayId() + "]" +
        ChatColor.GREEN + ", every " + cooldown + "s. It starts working in the next game.");
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1)
      return filter(SetupUtils.arenaNames(), args[0]);
    if (args.length == 2) {
      List<String> options = new ArrayList<>();
      options.add("diamond");
      options.add("emerald");
      String arenaName = SetupUtils.canonicalArenaName(args[0]);
      if (arenaName != null)
        options.addAll(SetupUtils.teamNames(SetupUtils.loadArenaYml(arenaName)));
      return filter(options, args[1].toLowerCase(Locale.ROOT));
    }
    return super.tabComplete(sender, args);
  }
}
