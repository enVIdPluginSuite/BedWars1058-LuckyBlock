package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.generator.GeneratorPlan;
import dev.etran.luckyblock.utils.SetupUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

/** An arena as the setup commands see it: its BedWars1058 file plus which generators it gets. */
final class ArenaContext {
  final String name;

  final FileConfiguration yml;

  final String group;

  final GeneratorPlan.Result plan;

  private ArenaContext(String name, FileConfiguration yml, String group, GeneratorPlan.Result plan) {
    this.name = name;
    this.yml = yml;
    this.group = group;
    this.plan = plan;
  }

  /** Loads the arena, or tells the sender it doesn't exist and returns null. */
  static ArenaContext load(CommandSender sender, String typedName) {
    String name = SetupUtils.canonicalArenaName(typedName);
    FileConfiguration yml = (name == null) ? null : SetupUtils.loadArenaYml(name);
    if (yml == null) {
      sender.sendMessage(ChatColor.RED + "Arena not found! use /bw arenaList");
      return null;
    }
    String group = SetupUtils.groupOf(name, yml);
    return new ArenaContext(name, yml, group, GeneratorPlan.forArena(name, group, yml));
  }
}
