package dev.etran.luckyblock.utils;

import com.andrei1058.bedwars.api.arena.IArena;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.arena.LoadedArena;
import dev.etran.luckyblock.data.YmlFile;
import dev.etran.luckyblock.generator.GeneratorKind;
import dev.etran.luckyblock.generator.LoadedGenerator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/** Helpers for the setup commands: reading BedWars1058 arena files and editing data.yml. */
public class SetupUtils {

  private static File arenasFolder() {
    Plugin bedWars = Bukkit.getPluginManager().getPlugin("BedWars1058");
    return (bedWars == null) ? null : new File(bedWars.getDataFolder(), "Arenas");
  }

  /** Names of the arenas that have a BedWars1058 arena file. */
  public static List<String> arenaNames() {
    List<String> names = new ArrayList<>();
    File folder = arenasFolder();
    File[] files = (folder == null) ? null : folder.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isFile() && file.getName().endsWith(".yml"))
          names.add(file.getName().substring(0, file.getName().length() - 4));
      }
    }
    Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
    return names;
  }

  /** The arena's real name, whatever capitalization was typed. Null if there is no such arena. */
  public static String canonicalArenaName(String typed) {
    for (String name : arenaNames()) {
      if (name.equalsIgnoreCase(typed))
        return name;
    }
    return null;
  }

  /** The arena's BedWars1058 file, or null if there is no such arena. */
  public static FileConfiguration loadArenaYml(String arenaName) {
    File folder = arenasFolder();
    if (folder == null || arenaName.contains("/") || arenaName.contains("\\") || arenaName.contains(".."))
      return null;
    File file = new File(folder, arenaName + ".yml");
    return file.isFile() ? YamlConfiguration.loadConfiguration(file) : null;
  }

  /** The arena's BedWars1058 group (what a running game reports, or what its file says). */
  public static String groupOf(String arenaName, FileConfiguration arenaYml) {
    IArena live = LuckyBlocks.getBedWars().getArenaUtil().getArenaByName(arenaName);
    if (live != null && live.getGroup() != null)
      return live.getGroup();
    return (arenaYml == null) ? "Default" : arenaYml.getString("group", "Default");
  }

  /** The BedWars1058 groups found in the arena files, each with the arenas in it (groups compared ignoring case). */
  public static java.util.Map<String, List<String>> arenasByGroup() {
    java.util.Map<String, List<String>> groups = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    for (String arena : arenaNames()) {
      FileConfiguration yml = loadArenaYml(arena);
      String group = (yml == null) ? "Default" : yml.getString("group", "Default");
      if (group == null || group.trim().isEmpty())
        group = "Default";
      List<String> list = groups.get(group.trim());
      if (list == null) {
        list = new ArrayList<>();
        groups.put(group.trim(), list);
      }
      list.add(arena);
    }
    return groups;
  }

  /** Names of the arenas whose file says they are in the group (ignoring case). */
  public static List<String> arenasInGroup(String group) {
    List<String> arenas = arenasByGroup().get(group.trim());
    return arlist(arenas);
  }

  private static List<String> arlist(List<String> arenas) {
    return (arenas == null) ? new ArrayList<String>() : arenas;
  }

  /** Team names of an arena (the keys under "Team" in its arena file). */
  public static List<String> teamNames(FileConfiguration arenaYml) {
    ConfigurationSection teams = (arenaYml == null) ? null : arenaYml.getConfigurationSection("Team");
    return (teams == null) ? new ArrayList<String>() : new ArrayList<>(teams.getKeys(false));
  }

  /** Adds a manual generator: remembered in memory and saved to data.yml. */
  public static LoadedGenerator addGenerator(String arenaName, GeneratorKind kind, String team, double x, double y, double z, int cooldown) {
    YmlFile file = LuckyBlocks.getInstance().getDataFile();
    LoadedArena arena = LoadedArena.getOrCreate(arenaName);
    LoadedGenerator generator = arena.addGenerator(arena.nextId(), kind, team, x, y, z, cooldown);
    file.getConfig().set(arenaName + "." + generator.getId(), generator.serialize());
    file.saveConfig();
    return generator;
  }

  /** Removes a manual generator from memory and data.yml. */
  public static boolean removeGenerator(String arenaName, int id) {
    LoadedArena arena = LoadedArena.byName(arenaName);
    if (arena == null || !arena.removeGenerator(id))
      return false;
    YmlFile file = LuckyBlocks.getInstance().getDataFile();
    FileConfiguration data = file.getConfig();
    data.set(arenaName + "." + id, null);
    ConfigurationSection section = data.getConfigurationSection(arenaName);
    if (section != null && section.getKeys(false).isEmpty())
      data.set(arenaName, null);
    file.saveConfig();
    return true;
  }
}
