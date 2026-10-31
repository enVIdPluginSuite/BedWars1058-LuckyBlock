package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.data.CachedConfig;
import dev.etran.luckyblock.drops.Drop;
import dev.etran.luckyblock.generator.GeneratorKind;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Shows and changes config.yml settings from in-game. */
public class SubConfig extends SubCommand {
  public String getName() {
    return "config";
  }

  public String getUsage() {
    return "config [setting] [value]";
  }

  public String getDescription() {
    return "Shows the settings, or changes one (saved to config.yml and applied right away)";
  }

  public void execute(CommandSender sender, String[] args) {
    if (args.length == 0) {
      showAll(sender);
      return;
    }
    String setting = ConfigChanges.normalize(args[0]);
    if (args.length == 1) {
      show(sender, args[0]);
      return;
    }
    String problem;
    if (ConfigChanges.textureSettings().contains(args[0].toLowerCase(Locale.ROOT)) || setting.endsWith(".textures")) {
      if (args.length != 3 || !(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
        sender.sendMessage(ChatColor.RED + "/bwlb config " + args[0] + " <add|remove> <color>");
        return;
      }
      problem = ConfigChanges.editTextures(args[0], args[1].equalsIgnoreCase("add"), args[2]);
    } else if (setting.startsWith("loot.")) {
      String[] parts = setting.split("\\.");
      if (parts.length != 3 || args.length != 2) {
        sender.sendMessage(ChatColor.RED + "/bwlb config loot.<drop>.<team|diamond|emerald> <weight>");
        return;
      }
      problem = ConfigChanges.setLoot(parts[1], parts[2], args[1]);
    } else if (args.length != 2) {
      sendUsage(sender);
      return;
    } else {
      problem = ConfigChanges.set(args[0], args[1]);
    }
    if (problem != null) {
      sender.sendMessage(ChatColor.RED + problem);
      return;
    }
    sender.sendMessage(ChatColor.GREEN + "Saved to config.yml. " + ChatColor.GRAY + "It applies to games that start from now on.");
    show(sender, args[0]);
  }

  private void showAll(CommandSender sender) {
    CachedConfig config = LuckyBlocks.getInstance().getCachedConfig();
    sender.sendMessage(ChatColor.GOLD + "Settings " + ChatColor.GRAY + "(change with /bwlb config <setting> <value>)");
    sender.sendMessage(ChatColor.YELLOW + " generator-limit" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.getGeneratorLimit());
    sender.sendMessage(ChatColor.YELLOW + " do-not-spawn-if-team-dead" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.isDoNotSpawnIfTeamDead());
    for (GeneratorKind kind : GeneratorKind.values()) {
      CachedConfig.KindSettings settings = config.getKind(kind);
      sender.sendMessage(ChatColor.YELLOW + " " + kind.getConfigKey() + ChatColor.GRAY + ": enabled " + ChatColor.WHITE + settings.isEnabled() +
          ChatColor.GRAY + ", cooldown " + ChatColor.WHITE + settings.getCooldown() + "s" +
          ChatColor.GRAY + ", colors " + ChatColor.WHITE + String.join(", ", settings.getTextures()));
    }
    sender.sendMessage(ChatColor.YELLOW + " groups" + ChatColor.GRAY + ": " + ChatColor.WHITE + String.join(", ", config.getLuckyGroups()) +
        ChatColor.GRAY + "  (change with /bwlb group)");
    sender.sendMessage(ChatColor.YELLOW + " loot" + ChatColor.GRAY + ": see /bwlb loot");
  }

  private void show(CommandSender sender, String typed) {
    CachedConfig config = LuckyBlocks.getInstance().getCachedConfig();
    String setting = ConfigChanges.normalize(typed);
    String value;
    if (setting.equals("generator-limit")) {
      value = String.valueOf(config.getGeneratorLimit());
    } else if (setting.equals("do-not-spawn-if-team-dead")) {
      value = String.valueOf(config.isDoNotSpawnIfTeamDead());
    } else if (setting.startsWith("generators.") && setting.split("\\.").length == 3 && ConfigChanges.kindOf(setting.split("\\.")[1]) != null) {
      CachedConfig.KindSettings settings = config.getKind(ConfigChanges.kindOf(setting.split("\\.")[1]));
      String field = setting.split("\\.")[2];
      if (field.equals("enabled")) {
        value = String.valueOf(settings.isEnabled());
      } else if (field.equals("cooldown")) {
        value = settings.getCooldown() + " seconds";
      } else if (field.equals("textures")) {
        value = String.join(", ", settings.getTextures());
      } else {
        value = null;
      }
    } else if (setting.startsWith("loot.") && setting.split("\\.").length == 3 && ConfigChanges.kindOf(setting.split("\\.")[2]) != null) {
      String[] parts = setting.split("\\.");
      value = "weight " + config.getLoot().getWeight(ConfigChanges.kindOf(parts[2]), parts[1]) + " (" +
          String.format(Locale.ROOT, "%.1f%%", new Object[] { Double.valueOf(config.getLoot().chance(ConfigChanges.kindOf(parts[2]), parts[1], Drop.ids())) }) + " of drops)";
    } else {
      value = null;
    }
    if (value == null) {
      sender.sendMessage(ChatColor.RED + "Unknown setting \"" + typed + "\". Run /bwlb config to see them.");
      return;
    }
    sender.sendMessage(ChatColor.YELLOW + typed.toLowerCase(Locale.ROOT) + ChatColor.GRAY + ": " + ChatColor.WHITE + value);
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1) {
      List<String> options = new ArrayList<>(ConfigChanges.scalarSettings());
      options.addAll(ConfigChanges.textureSettings());
      for (String drop : Drop.ids()) {
        for (GeneratorKind tier : GeneratorKind.values())
          options.add("loot." + drop + "." + tier.getConfigKey());
      }
      return filter(options, args[0]);
    }
    String setting = ConfigChanges.normalize(args[0]);
    if (args.length == 2) {
      if (setting.equals("do-not-spawn-if-team-dead") || setting.endsWith(".enabled"))
        return filter(Arrays.asList(new String[] { "true", "false" }), args[1]);
      if (setting.endsWith(".textures"))
        return filter(Arrays.asList(new String[] { "add", "remove" }), args[1]);
    }
    if (args.length == 3 && setting.endsWith(".textures")) {
      GeneratorKind kind = ConfigChanges.kindOf(setting.split("\\.")[1]);
      if (kind == null)
        return super.tabComplete(sender, args);
      List<String> current = LuckyBlocks.getInstance().getCachedConfig().getKind(kind).getTextures();
      if (args[1].equalsIgnoreCase("remove"))
        return filter(current, args[2]);
      List<String> others = new ArrayList<>(LuckyBlockTexture.allNames());
      others.removeAll(current);
      return filter(others, args[2]);
    }
    return super.tabComplete(sender, args);
  }
}
