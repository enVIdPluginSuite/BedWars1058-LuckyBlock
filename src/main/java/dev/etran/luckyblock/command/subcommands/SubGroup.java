package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.data.CachedConfig;
import dev.etran.luckyblock.generator.ArenaScanner;
import dev.etran.luckyblock.generator.GeneratorKind;
import dev.etran.luckyblock.generator.LoadedGenerator;
import dev.etran.luckyblock.utils.SetupUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Picks which BedWars1058 arena groups get lucky blocks. Every arena in such a group gets generators at
 * its team islands and its diamond and emerald generators, found from the arena files.
 */
public class SubGroup extends SubCommand {
  public String getName() {
    return "group";
  }

  public String getUsage() {
    return "group <add|remove|scan|list> [group]";
  }

  public String getDescription() {
    return "Choose the BedWars1058 arena group(s) whose maps get lucky block generators automatically";
  }

  public void execute(CommandSender sender, String[] args) {
    String action = (args.length == 0) ? "list" : args[0].toLowerCase(Locale.ROOT);
    if (action.equals("list")) {
      list(sender);
      return;
    }
    if (args.length != 2 || !Arrays.asList("add", "remove", "scan").contains(action)) {
      sendUsage(sender);
      return;
    }
    String group = args[1];
    LuckyBlocks plugin = LuckyBlocks.getInstance();
    CachedConfig config = plugin.getCachedConfig();
    if (action.equals("add")) {
      if (config.isLuckyGroup(group)) {
        sender.sendMessage(ChatColor.YELLOW + "The group " + ChatColor.AQUA + group + ChatColor.YELLOW + " already gets lucky blocks.");
      } else {
        plugin.getGroupStore().add(group);
        config.setExtraGroups(plugin.getGroupStore().getGroups());
        sender.sendMessage(ChatColor.GREEN + "Arenas in the group " + ChatColor.AQUA + group + ChatColor.GREEN + " now get lucky block generators from their next game.");
      }
      report(sender, group);
    } else if (action.equals("remove")) {
      if (plugin.getGroupStore().remove(group)) {
        config.setExtraGroups(plugin.getGroupStore().getGroups());
        sender.sendMessage(ChatColor.GREEN + "The group " + ChatColor.AQUA + group + ChatColor.GREEN + " no longer gets lucky blocks from its next game.");
      } else if (config.getConfigGroups().contains(group.toLowerCase(Locale.ROOT))) {
        sender.sendMessage(ChatColor.RED + "That group is listed in config.yml (\"groups\"). Remove it there and restart.");
      } else {
        sender.sendMessage(ChatColor.RED + "The group " + group + " was not added. See " + ChatColor.YELLOW + "/bwlb group list");
      }
    } else {
      report(sender, group);
    }
  }

  private void list(CommandSender sender) {
    CachedConfig config = LuckyBlocks.getInstance().getCachedConfig();
    Map<String, List<String>> found = SetupUtils.arenasByGroup();
    sender.sendMessage(ChatColor.GOLD + "BedWars1058 arena groups " + ChatColor.GRAY + "(green = gets lucky blocks)");
    if (found.isEmpty())
      sender.sendMessage(ChatColor.RED + " No BedWars1058 arenas were found.");
    for (Map.Entry<String, List<String>> entry : found.entrySet()) {
      boolean lucky = config.isLuckyGroup(entry.getKey());
      sender.sendMessage((lucky ? ChatColor.GREEN : ChatColor.GRAY) + " " + entry.getKey() + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + entry.getValue().size() + " arena(s): " + String.join(", ", entry.getValue()));
    }
    List<String> unmatched = new ArrayList<>();
    for (String group : config.getLuckyGroups()) {
      boolean exists = false;
      for (String name : found.keySet())
        exists |= name.equalsIgnoreCase(group);
      if (!exists)
        unmatched.add(group);
    }
    if (!unmatched.isEmpty())
      sender.sendMessage(ChatColor.YELLOW + " Lucky groups with no arena in them yet: " + String.join(", ", unmatched));
    sender.sendMessage(ChatColor.GRAY + "Add one with /bwlb group add <group>.");
  }

  /** Lists the arenas in the group and the generators found in each one. */
  private void report(CommandSender sender, String group) {
    CachedConfig config = LuckyBlocks.getInstance().getCachedConfig();
    List<String> arenas = SetupUtils.arenasInGroup(group);
    if (arenas.isEmpty()) {
      sender.sendMessage(ChatColor.RED + "No arena is in a BedWars1058 group called \"" + group + "\" yet.");
      sender.sendMessage(ChatColor.GRAY + "Create the group with /bw arenaGroup create " + group + " and put arenas in it with /bw arenaGroup set <arena> " + group);
      return;
    }
    sender.sendMessage(ChatColor.GOLD + "Generators found in the group " + ChatColor.AQUA + group + ChatColor.GOLD + " (" + arenas.size() + " arena(s)):");
    for (String arena : arenas) {
      FileConfiguration yml = SetupUtils.loadArenaYml(arena);
      List<LoadedGenerator> spots = (yml == null) ? new ArrayList<LoadedGenerator>() : ArenaScanner.scan(arena, yml, config);
      int team = 0;
      int diamond = 0;
      int emerald = 0;
      for (LoadedGenerator spot : spots) {
        if (spot.getKind() == GeneratorKind.TEAM) {
          team++;
        } else if (spot.getKind() == GeneratorKind.DIAMOND) {
          diamond++;
        } else {
          emerald++;
        }
      }
      if (spots.isEmpty()) {
        sender.sendMessage(ChatColor.RED + " " + arena + ChatColor.GRAY + ": no generators found (is the arena fully set up?)");
      } else {
        sender.sendMessage(ChatColor.GREEN + " " + arena + ChatColor.GRAY + ": " + team + " team, " + diamond + " diamond, " + emerald + " emerald");
      }
    }
    sender.sendMessage(ChatColor.GRAY + "See them with /bwlb list <arena>; remove or add single ones there.");
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1)
      return filter(Arrays.asList(new String[] { "add", "remove", "scan", "list" }), args[0]);
    if (args.length == 2 && !args[0].equalsIgnoreCase("list")) {
      List<String> groups = new ArrayList<>(SetupUtils.arenasByGroup().keySet());
      if (args[0].equalsIgnoreCase("remove"))
        groups = LuckyBlocks.getInstance().getGroupStore().getGroups();
      return filter(groups, args[1]);
    }
    return super.tabComplete(sender, args);
  }
}
