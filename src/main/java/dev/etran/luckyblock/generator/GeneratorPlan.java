package dev.etran.luckyblock.generator;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.arena.LoadedArena;
import dev.etran.luckyblock.data.ArenaOverrides;
import dev.etran.luckyblock.data.CachedConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Decides which lucky block generators an arena gets:
 *
 *  - manual generators from data.yml always run,
 *  - automatic generators (detected from the arena file) run when the arena's BedWars1058 group is a
 *    lucky block group (or automatic generators were forced on for the arena),
 *    unless that generator type is disabled in config.yml, the generator was removed with
 *    /luckyblock removegenerator, or a manual generator already sits in the same block.
 */
public final class GeneratorPlan {

  public enum Status {
    /** The generator runs. */
    ACTIVE("active"),
    /** Removed by an admin; can be brought back with /luckyblock restore. */
    EXCLUDED("removed"),
    /** Automatic generators are off for this arena. */
    AUTO_OFF("automatic generators off"),
    /** This generator type is disabled in config.yml. */
    KIND_DISABLED("disabled in config.yml"),
    /** A manual generator in the same block takes its place. */
    REPLACED("replaced by manual generator");

    private final String text;

    Status(String text) {
      this.text = text;
    }

    public String getText() {
      return this.text;
    }
  }

  public static final class Entry {
    private final LoadedGenerator spec;

    private final Status status;

    Entry(LoadedGenerator spec, Status status) {
      this.spec = spec;
      this.status = status;
    }

    public LoadedGenerator getSpec() {
      return this.spec;
    }

    public Status getStatus() {
      return this.status;
    }

    public boolean isActive() {
      return this.status == Status.ACTIVE;
    }
  }

  public static final class Result {
    private final String group;

    private final boolean autoEnabled;

    private final String autoReason;

    private final List<Entry> entries;

    Result(String group, boolean autoEnabled, String autoReason, List<Entry> entries) {
      this.group = group;
      this.autoEnabled = autoEnabled;
      this.autoReason = autoReason;
      this.entries = entries;
    }

    public String getGroup() {
      return this.group;
    }

    public boolean isAutoEnabled() {
      return this.autoEnabled;
    }

    public String getAutoReason() {
      return this.autoReason;
    }

    /** Manual generators first (by id), then the automatic ones (a1, a2, ...). */
    public List<Entry> getEntries() {
      return this.entries;
    }

    public List<Entry> getActive() {
      List<Entry> active = new ArrayList<>();
      for (Entry entry : this.entries) {
        if (entry.isActive())
          active.add(entry);
      }
      return active;
    }

    /** Finds an entry by what the player types: "3" (manual) or "a2" (automatic). */
    public Entry byDisplayId(String id) {
      for (Entry entry : this.entries) {
        if (entry.getSpec().getDisplayId().equalsIgnoreCase(id))
          return entry;
      }
      return null;
    }
  }

  private GeneratorPlan() {}

  /** Builds the plan for an arena from the plugin's live state (config.yml, data.yml, arena-settings.yml). */
  public static Result forArena(String arenaName, String group, FileConfiguration arenaYml) {
    LuckyBlocks plugin = LuckyBlocks.getInstance();
    CachedConfig config = plugin.getCachedConfig();
    ArenaOverrides overrides = plugin.getArenaOverrides();

    Boolean forced = overrides.getAuto(arenaName);
    boolean autoEnabled;
    String reason;
    if (forced != null) {
      autoEnabled = forced.booleanValue();
      reason = autoEnabled ? "forced on for this arena" : "forced off for this arena";
    } else if (config.isLuckyGroup(group)) {
      autoEnabled = true;
      reason = "group \"" + group + "\" is a lucky block group";
    } else {
      autoEnabled = false;
      reason = "group \"" + group + "\" is not a lucky block group";
    }

    LoadedArena manualArena = LoadedArena.byName(arenaName);
    List<LoadedGenerator> manual = (manualArena == null) ? new ArrayList<LoadedGenerator>() : new ArrayList<>(manualArena.getGenerators());
    List<LoadedGenerator> spots = (arenaYml == null) ? new ArrayList<LoadedGenerator>() : ArenaScanner.scan(arenaName, arenaYml, config);

    return new Result(group, autoEnabled, reason, build(manual, spots, autoEnabled, overrides.getExcluded(arenaName), config));
  }

  /** The pure decision logic (no plugin state), so it can be tested on its own. */
  public static List<Entry> build(Collection<LoadedGenerator> manual, List<LoadedGenerator> autoSpots, boolean autoEnabled, Set<String> excluded, CachedConfig config) {
    List<Entry> entries = new ArrayList<>();

    List<LoadedGenerator> sortedManual = new ArrayList<>(manual);
    sortedManual.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
    Set<String> manualBlocks = new HashSet<>();
    for (LoadedGenerator generator : sortedManual) {
      manualBlocks.add(generator.getBlockKey());
      entries.add(new Entry(generator, Status.ACTIVE));
    }

    for (LoadedGenerator spot : autoSpots) {
      Status status;
      if (excluded.contains(spot.getBlockKey())) {
        status = Status.EXCLUDED;
      } else if (!autoEnabled) {
        status = Status.AUTO_OFF;
      } else if (!config.getKind(spot.getKind()).isEnabled()) {
        status = Status.KIND_DISABLED;
      } else if (manualBlocks.contains(spot.getBlockKey())) {
        status = Status.REPLACED;
      } else {
        status = Status.ACTIVE;
      }
      entries.add(new Entry(spot, status));
    }
    return entries;
  }
}
