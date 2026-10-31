package dev.etran.luckyblock.data;

import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Per-arena settings for the automatic generators, stored in arena-settings.yml and changed with
 * /bwlb auto, /bwlb removegenerator and /bwlb restore.
 *
 *   MyArena:
 *     auto: true            # (optional) force automatic generators on/off, ignoring the group
 *     excluded:             # automatic generators removed by an admin (block coordinates)
 *       - "12,64,-30"
 */
public class ArenaOverrides {
  private final YmlFile file;

  public ArenaOverrides(YmlFile file) {
    this.file = file;
  }

  /** Re-reads arena-settings.yml from disk. */
  public void reload() {
    this.file.reloadConfig();
  }

  /** TRUE / FALSE if forced for this arena, null to follow the group. */
  public Boolean getAuto(String arena) {
    FileConfiguration config = this.file.getConfig();
    String path = arena + ".auto";
    return config.isBoolean(path) ? Boolean.valueOf(config.getBoolean(path)) : null;
  }

  public void setAuto(String arena, Boolean value) {
    this.file.getConfig().set(arena + ".auto", value);
    cleanup(arena);
    this.file.saveConfig();
  }

  public Set<String> getExcluded(String arena) {
    return new LinkedHashSet<>(this.file.getConfig().getStringList(arena + ".excluded"));
  }

  /** @return false if it was already excluded */
  public boolean exclude(String arena, String blockKey) {
    Set<String> excluded = getExcluded(arena);
    if (!excluded.add(blockKey))
      return false;
    saveExcluded(arena, excluded);
    return true;
  }

  /** @return false if it wasn't excluded */
  public boolean restore(String arena, String blockKey) {
    Set<String> excluded = getExcluded(arena);
    if (!excluded.remove(blockKey))
      return false;
    saveExcluded(arena, excluded);
    return true;
  }

  /** @return how many were restored */
  public int restoreAll(String arena) {
    Set<String> excluded = getExcluded(arena);
    saveExcluded(arena, new LinkedHashSet<String>());
    return excluded.size();
  }

  private void saveExcluded(String arena, Set<String> excluded) {
    this.file.getConfig().set(arena + ".excluded", excluded.isEmpty() ? null : new java.util.ArrayList<>(excluded));
    cleanup(arena);
    this.file.saveConfig();
  }

  /** Removes the arena's section once nothing is left in it. */
  private void cleanup(String arena) {
    FileConfiguration config = this.file.getConfig();
    if (config.getConfigurationSection(arena) != null && config.getConfigurationSection(arena).getKeys(false).isEmpty())
      config.set(arena, null);
  }
}
