package dev.etran.luckyblock.data;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * The BedWars1058 arena groups added with /luckyblock group add (groups.yml). They work together with the
 * "groups" list in config.yml: an arena in either gets automatic lucky block generators.
 */
public class GroupStore {
  private final YmlFile file;

  public GroupStore(YmlFile file) {
    this.file = file;
  }

  public List<String> getGroups() {
    FileConfiguration config = this.file.getConfig();
    List<String> groups = new ArrayList<>();
    for (String group : config.getStringList("groups")) {
      if (group != null && !group.trim().isEmpty())
        groups.add(group.trim());
    }
    return groups;
  }

  public boolean contains(String group) {
    for (String existing : getGroups()) {
      if (existing.equalsIgnoreCase(group.trim()))
        return true;
    }
    return false;
  }

  /** Returns false if the group was already there. */
  public boolean add(String group) {
    if (contains(group))
      return false;
    List<String> groups = getGroups();
    groups.add(group.trim());
    save(groups);
    return true;
  }

  /** Returns false if the group was not in the list. */
  public boolean remove(String group) {
    List<String> groups = getGroups();
    boolean removed = groups.removeIf(existing -> existing.equalsIgnoreCase(group.trim()));
    if (removed)
      save(groups);
    return removed;
  }

  private void save(List<String> groups) {
    this.file.getConfig().set("groups", groups);
    this.file.saveConfig();
  }
}
