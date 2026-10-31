package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.data.CachedConfig;
import dev.etran.luckyblock.drops.Drop;
import dev.etran.luckyblock.drops.Rarity;
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
    if (ConfigChanges.textureKey(args[0]).matches("rarities\\.[a-z]+\\.textures")) {
      if (args.length != 3 || !(args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
        sender.sendMessage(ChatColor.RED + "/bwlb config " + args[0] + " <add|remove> <color>");
        return;
      }
      problem = ConfigChanges.editTextures(args[0], args[1].equalsIgnoreCase("add"), args[2]);
    } else if (setting.startsWith("loot.")) {
      String[] parts = setting.split("\\.");
      Rarity rarity = parts.length == 3 ? Rarity.fromKey(parts[1]) : null;
      if (rarity == null || args.length != 2) {
        sender.sendMessage(ChatColor.RED + "/bwlb config loot.<rarity>.<drop> <weight>");
        return;
      }
      problem = ConfigChanges.setLoot(rarity, parts[2], args[1]);
    } else if (setting.startsWith("rarity-chances.") || setting.startsWith("rarity.")) {
      String[] parts = setting.split("\\.");
      if (parts.length != 3 || args.length != 2) {
        sender.sendMessage(ChatColor.RED + "/bwlb config rarity.<team|diamond|emerald>.<rarity> <weight>");
        return;
      }
      problem = ConfigChanges.setRarityChance(parts[1], parts[2], args[1]);
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
          ChatColor.GRAY + ", cooldown " + ChatColor.WHITE + settings.getCooldown() + "s");
    }
    sender.sendMessage(ChatColor.YELLOW + " sync-upgrades" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.isSyncUpgrades());
    sender.sendMessage(ChatColor.YELLOW + " middle-upgrade" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.getMiddleUpgrade() + "%");
    sender.sendMessage(ChatColor.YELLOW + " hologram.enabled" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.getHologram().isEnabled());
    for (Rarity rarity : Rarity.values()) {
      sender.sendMessage(ChatColor.YELLOW + " " + rarity.getKey() + ".textures" + ChatColor.GRAY + ": " + ChatColor.WHITE +
          String.join(", ", config.getRarity(rarity).getTextures()));
    }
    sender.sendMessage(ChatColor.YELLOW + " groups" + ChatColor.GRAY + ": " + ChatColor.WHITE + String.join(", ", config.getLuckyGroups()) +
        ChatColor.GRAY + "  (change with /bwlb group)");
    sender.sendMessage(ChatColor.YELLOW + " loot / rarity" + ChatColor.GRAY + ": see /bwlb loot and /bwlb rarity");
  }

  private void show(CommandSender sender, String typed) {
    CachedConfig config = LuckyBlocks.getInstance().getCachedConfig();
    String setting = ConfigChanges.normalize(typed);
    String value;
    if (setting.equals("generator-limit")) {
      value = String.valueOf(config.getGeneratorLimit());
    } else if (setting.equals("do-not-spawn-if-team-dead")) {
      value = String.valueOf(config.isDoNotSpawnIfTeamDead());
    } else if (setting.equals("sync-upgrades")) {
      value = String.valueOf(config.isSyncUpgrades());
    } else if (setting.equals("middle-upgrade")) {
      value = config.getMiddleUpgrade() + "%";
    } else if (setting.equals("hologram.enabled")) {
      value = String.valueOf(config.getHologram().isEnabled());
    } else if (ConfigChanges.textureKey(typed).matches("rarities\\.[a-z]+\\.textures")) {
      value = String.join(", ", config.getRarity(Rarity.fromKey(ConfigChanges.textureKey(typed).split("\\.")[1])).getTextures());
    } else if (setting.startsWith("generators.") && setting.split("\\.").length == 3 && ConfigChanges.kindOf(setting.split("\\.")[1]) != null) {
      CachedConfig.KindSettings settings = config.getKind(ConfigChanges.kindOf(setting.split("\\.")[1]));
      String field = setting.split("\\.")[2];
      if (field.equals("enabled")) {
        value = String.valueOf(settings.isEnabled());
      } else if (field.equals("cooldown")) {
        value = settings.getCooldown() + " seconds";
      } else {
        value = null;
      }
    } else if (setting.startsWith("loot.") && setting.split("\\.").length == 3 && Rarity.fromKey(setting.split("\\.")[1]) != null) {
      String[] parts = setting.split("\\.");
      Rarity rarity = Rarity.fromKey(parts[1]);
      value = "weight " + config.getLoot().getWeight(rarity, parts[2]) + " (" +
          String.format(Locale.ROOT, "%.1f%%", Double.valueOf(config.getLoot().chance(rarity, parts[2], Drop.ids()))) + " of " + rarity.getKey() + " drops)";
    } else if ((setting.startsWith("rarity.") || setting.startsWith("rarity-chances.")) && setting.split("\\.").length == 3
        && ConfigChanges.kindOf(setting.split("\\.")[1]) != null && Rarity.fromKey(setting.split("\\.")[2]) != null) {
      String[] parts = setting.split("\\.");
      value = "weight " + config.getRarityWeight(ConfigChanges.kindOf(parts[1]), Rarity.fromKey(parts[2])) + " (" +
          String.format(Locale.ROOT, "%.1f%%", Double.valueOf(config.getRarityChance(ConfigChanges.kindOf(parts[1]), Rarity.fromKey(parts[2])))) + ")";
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
      for (Rarity rarity : Rarity.values())
        options.add(rarity.getKey() + ".textures");
      for (GeneratorKind kind : GeneratorKind.values()) {
        for (Rarity rarity : Rarity.values())
          options.add("rarity." + kind.getConfigKey() + "." + rarity.getKey());
      }
      for (Rarity rarity : Rarity.values()) {
        for (String drop : Drop.ids())
          options.add("loot." + rarity.getKey() + "." + drop);
      }
      return filter(options, args[0]);
    }
    String setting = ConfigChanges.normalize(args[0]);
    boolean textures = ConfigChanges.textureKey(args[0]).matches("rarities\\.[a-z]+\\.textures");
    if (args.length == 2) {
      if (setting.equals("do-not-spawn-if-team-dead") || setting.equals("sync-upgrades") || setting.endsWith(".enabled"))
        return filter(Arrays.asList(new String[] { "true", "false" }), args[1]);
      if (textures)
        return filter(Arrays.asList(new String[] { "add", "remove" }), args[1]);
    }
    if (args.length == 3 && textures) {
      Rarity rarity = Rarity.fromKey(ConfigChanges.textureKey(args[0]).split("\\.")[1]);
      List<String> current = LuckyBlocks.getInstance().getCachedConfig().getRarity(rarity).getTextures();
      if (args[1].equalsIgnoreCase("remove"))
        return filter(current, args[2]);
      List<String> others = new ArrayList<>(LuckyBlockTexture.allNames());
      others.removeAll(current);
      return filter(others, args[2]);
    }
    return super.tabComplete(sender, args);
  }
}
