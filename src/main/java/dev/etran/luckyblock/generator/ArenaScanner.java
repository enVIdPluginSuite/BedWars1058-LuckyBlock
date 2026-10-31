package dev.etran.luckyblock.generator;

import dev.etran.luckyblock.data.CachedConfig;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Finds the places where a lucky block generator belongs by reading a BedWars1058 arena file:
 * one per team island, plus every diamond and emerald generator.
 *
 * The result is always in the same order (teams in file order, then diamond, then emerald), and the
 * generators get the ids a1, a2, a3... in that order.
 */
public final class ArenaScanner {

  private ArenaScanner() {}

  public static List<LoadedGenerator> scan(String arenaName, FileConfiguration arenaYml, CachedConfig config) {
    List<LoadedGenerator> spots = new ArrayList<>();

    // Team islands: next to the team's iron generator (falls back to gold, then the team spawn)
    ConfigurationSection teams = arenaYml.getConfigurationSection("Team");
    if (teams != null) {
      for (String team : teams.getKeys(false)) {
        double[] position = firstLocation(arenaYml, "Team." + team + ".Iron");
        if (position == null)
          position = firstLocation(arenaYml, "Team." + team + ".Gold");
        if (position == null)
          position = firstLocation(arenaYml, "Team." + team + ".Spawn");
        if (position == null)
          continue;
        spots.add(new LoadedGenerator(arenaName, GeneratorKind.TEAM, team, 0, position[0], position[1], position[2], config.getKind(GeneratorKind.TEAM).getCooldown()));
      }
    }

    // Diamond and emerald generators
    addOreSpots(spots, arenaName, arenaYml, config, GeneratorKind.DIAMOND, "generator.Diamond");
    addOreSpots(spots, arenaName, arenaYml, config, GeneratorKind.EMERALD, "generator.Emerald");

    // number everything in order: a1, a2, a3...
    List<LoadedGenerator> numbered = new ArrayList<>();
    for (int i = 0; i < spots.size(); i++) {
      LoadedGenerator spot = spots.get(i);
      numbered.add(new LoadedGenerator(arenaName, spot.getKind(), spot.getTeam(), -(i + 1), spot.getX(), spot.getY(), spot.getZ(), spot.getCooldown()));
    }
    return numbered;
  }

  private static void addOreSpots(List<LoadedGenerator> spots, String arenaName, FileConfiguration yml, CachedConfig config, GeneratorKind kind, String path) {
    for (String raw : yml.getStringList(path)) {
      double[] position = parseLocation(raw);
      if (position == null)
        continue;
      spots.add(new LoadedGenerator(arenaName, kind, null, 0, position[0], position[1], position[2], config.getKind(kind).getCooldown()));
    }
  }

  /** BedWars1058 stores one location as a string, or a list of them. Returns the first one. */
  private static double[] firstLocation(FileConfiguration yml, String path) {
    Object value = yml.get(path);
    if (value instanceof String)
      return parseLocation((String)value);
    if (value instanceof List) {
      for (Object entry : (List<?>)value) {
        if (entry instanceof String) {
          double[] position = parseLocation((String)entry);
          if (position != null)
            return position;
        }
      }
    }
    return null;
  }

  /** "x,y,z,yaw,pitch" -> {x, y, z}. Returns null if it can't be read. */
  static double[] parseLocation(String raw) {
    if (raw == null)
      return null;
    String[] split = raw.split(",");
    if (split.length < 3)
      return null;
    try {
      return new double[] { Double.parseDouble(split[0].trim()), Double.parseDouble(split[1].trim()), Double.parseDouble(split[2].trim()) };
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
