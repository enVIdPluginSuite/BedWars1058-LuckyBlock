package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.data.ConfigEditor;
import dev.etran.luckyblock.drops.Drop;
import dev.etran.luckyblock.drops.Rarity;
import dev.etran.luckyblock.generator.GeneratorKind;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Changes values of config.yml from in-game commands. The file is edited in place (comments stay), then
 * re-read, so the change is active at once for the games that start from now on.
 */
final class ConfigChanges {
  private ConfigChanges() {}

  /** Short names accepted next to the full path: "diamond.cooldown" means "generators.diamond.cooldown". */
  static String normalize(String path) {
    String lower = path.toLowerCase(Locale.ROOT);
    for (GeneratorKind kind : GeneratorKind.values()) {
      if (lower.startsWith(kind.getConfigKey() + "."))
        return "generators." + lower;
    }
    return lower;
  }

  /** Every setting that holds a single value, in the short form. */
  static List<String> scalarSettings() {
    List<String> list = new ArrayList<>();
    list.add("generator-limit");
    list.add("do-not-spawn-if-team-dead");
    list.add("sync-upgrades");
    list.add("middle-upgrade");
    list.add("hologram.enabled");
    for (GeneratorKind kind : GeneratorKind.values()) {
      list.add(kind.getConfigKey() + ".enabled");
      list.add(kind.getConfigKey() + ".cooldown");
    }
    return list;
  }

  /** The per-rarity color lists: "common.textures" ... */
  static List<String> textureSettings() {
    List<String> list = new ArrayList<>();
    for (Rarity rarity : Rarity.values())
      list.add("rarities." + rarity.getKey() + ".textures");
    return list;
  }

  static GeneratorKind kindOf(String name) {
    for (GeneratorKind kind : GeneratorKind.values()) {
      if (kind.getConfigKey().equalsIgnoreCase(name))
        return kind;
    }
    return null;
  }

  /** Accepts "rare.textures" as well as "rarities.rare.textures". */
  static String textureKey(String typed) {
    String lower = typed.toLowerCase(Locale.ROOT);
    if (lower.matches("(common|uncommon|rare|legendary)\\.textures"))
      return "rarities." + lower;
    return lower;
  }

  /** Sets a single value. Returns null on success, otherwise the problem. */
  static String set(String typedPath, String typedValue) {
    String path = normalize(typedPath);
    String value = typedValue.toLowerCase(Locale.ROOT);
    if (path.equals("generator-limit") || path.matches("generators\\.(team|diamond|emerald)\\.cooldown")) {
      Integer number = parseInt(value);
      if (number == null || number.intValue() < 1)
        return typedPath + " needs a whole number of 1 or more.";
      return write(path, String.valueOf(number), number);
    }
    if (path.equals("middle-upgrade")) {
      Integer number = parseInt(value);
      if (number == null || number.intValue() < 0 || number.intValue() > 100)
        return typedPath + " needs a whole number from 0 to 100.";
      return write(path, String.valueOf(number), number);
    }
    if (path.equals("do-not-spawn-if-team-dead") || path.equals("sync-upgrades") || path.equals("hologram.enabled")
        || path.matches("generators\\.(team|diamond|emerald)\\.enabled")) {
      if (!value.equals("true") && !value.equals("false"))
        return typedPath + " needs true or false.";
      return write(path, value, Boolean.valueOf(value));
    }
    if (textureKey(typedPath).matches("rarities\\.[a-z]+\\.textures"))
      return typedPath + " is a list: use \"" + typedPath + " add <color>\" or \"" + typedPath + " remove <color>\".";
    return "Unknown setting \"" + typedPath + "\". Run /bwlb config to see them.";
  }

  /** Adds a lucky block color to (or removes it from) what a rarity uses. */
  static String editTextures(String typedPath, boolean add, String color) {
    String path = textureKey(typedPath);
    if (!path.matches("rarities\\.(common|uncommon|rare|legendary)\\.textures"))
      return typedPath + " is not a list of colors. Use common.textures, uncommon.textures, rare.textures or legendary.textures.";
    String name = color.toLowerCase(Locale.ROOT);
    Rarity rarity = Rarity.fromKey(path.split("\\.")[1]);
    List<String> items = new ArrayList<>(LuckyBlocks.getInstance().getCachedConfig().getRarity(rarity).getTextures());
    if (add) {
      if (!LuckyBlockTexture.allNames().contains(name))
        return "There is no lucky block color \"" + color + "\". They are: " + String.join(", ", LuckyBlockTexture.allNames());
      if (items.contains(name))
        return rarity.getKey() + " already uses " + name + ".";
      items.add(name);
    } else {
      if (!items.remove(name))
        return rarity.getKey() + " does not use " + name + ".";
      if (items.isEmpty())
        return "That would leave " + rarity.getKey() + " with no colors. Add another one first.";
    }
    return write(path, null, items, items);
  }

  /** Sets how likely a drop is within one rarity. */
  static String setLoot(Rarity rarity, String drop, String weight) {
    String id = drop.toLowerCase(Locale.ROOT);
    if (!Drop.ids().contains(id))
      return "There is no drop \"" + drop + "\". They are: " + String.join(", ", Drop.ids());
    Integer number = parseInt(weight);
    if (number == null || number.intValue() < 0)
      return "The weight must be a whole number, 0 or more (0 = never).";
    return write("loot." + rarity.getKey() + "." + id, String.valueOf(number), number);
  }

  /** Sets the weight of a rarity in the roll of one generator kind. */
  static String setRarityChance(String kindName, String rarityName, String weight) {
    GeneratorKind kind = kindOf(kindName);
    if (kind == null)
      return "The generator must be team, diamond or emerald.";
    Rarity rarity = Rarity.fromKey(rarityName);
    if (rarity == null)
      return "The rarity must be common, uncommon, rare or legendary.";
    Integer number = parseInt(weight);
    if (number == null || number.intValue() < 0)
      return "The weight must be a whole number, 0 or more.";
    return write("rarity-chances." + kind.getConfigKey() + "." + rarity.getKey(), String.valueOf(number), number);
  }

  private static Integer parseInt(String text) {
    try {
      return Integer.valueOf(Integer.parseInt(text.trim()));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String write(String path, String text, Object value) {
    return write(path, text, null, value);
  }

  private static String write(String path, String text, List<String> list, Object value) {
    LuckyBlocks plugin = LuckyBlocks.getInstance();
    File file = new File(plugin.getDataFolder(), "config.yml");
    try {
      List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
      boolean edited;
      if (list != null) {
        edited = ConfigEditor.setList(lines, path, list);
      } else if (path.startsWith("rarity-chances.")) {
        String[] parts = path.split("\\.");
        edited = ConfigEditor.setFlowEntry(lines, "rarity-chances." + parts[1], parts[2], text);
      } else {
        edited = ConfigEditor.setScalar(lines, path, text);
        if (!edited && path.startsWith("loot.")) {
          int dot = path.lastIndexOf('.');
          edited = ConfigEditor.insertChild(lines, path.substring(0, dot), path.substring(dot + 1), text);
        }
      }
      if (edited) {
        Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
      } else {
        // the key is not in the file (an older config.yml): let Bukkit write it, which drops the comments
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
      }
    } catch (IOException e) {
      return "Could not write config.yml: " + e.getMessage();
    }
    plugin.reloadSettings();
    return null;
  }
}
