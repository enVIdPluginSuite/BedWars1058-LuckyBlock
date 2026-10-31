package dev.etran.luckyblock.data;

import dev.etran.luckyblock.drops.LootTable;
import dev.etran.luckyblock.generator.GeneratorKind;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CachedConfig {

  /** config.yml "generators.<kind>" section. */
  public static final class KindSettings {
    private final boolean enabled;

    private final int cooldown;

    private final List<String> textures;

    public KindSettings(boolean enabled, int cooldown, List<String> textures) {
      this.enabled = enabled;
      this.cooldown = cooldown;
      this.textures = Collections.unmodifiableList(textures);
    }

    public boolean isEnabled() {
      return this.enabled;
    }

    /** Seconds between lucky blocks. */
    public int getCooldown() {
      return this.cooldown;
    }

    /** Names (keys of textures.yml, lower case) of the lucky blocks this generator may spawn. */
    public List<String> getTextures() {
      return this.textures;
    }
  }

  private final int generatorLimit;

  private final boolean doNotSpawnIfTeamDead;

  private final Set<String> luckyGroups;

  /** Groups added in-game with /luckyblock group add (groups.yml), lower case. */
  private volatile Set<String> extraGroups = Collections.emptySet();

  private final Map<GeneratorKind, KindSettings> kinds;

  private final LootTable loot;

  public CachedConfig(int generatorLimit, boolean doNotSpawnIfTeamDead, Set<String> luckyGroups, Map<GeneratorKind, KindSettings> kinds, LootTable loot) {
    this.generatorLimit = generatorLimit;
    this.doNotSpawnIfTeamDead = doNotSpawnIfTeamDead;
    this.luckyGroups = Collections.unmodifiableSet(luckyGroups);
    this.kinds = new EnumMap<>(kinds);
    this.loot = loot;
  }

  /** How likely each drop is for each tier of lucky block ("loot:" in config.yml). */
  public LootTable getLoot() {
    return this.loot;
  }

  /**
   * The tier of a lucky block, which decides its loot. It comes from the color: a color listed under
   * generators.emerald.textures is an emerald block, under diamond a diamond block. A color listed in
   * several counts as the best of them, and a color listed nowhere counts as a team block.
   */
  public GeneratorKind tierOf(String textureName) {
    if (textureName != null) {
      String name = textureName.toLowerCase(Locale.ROOT);
      if (this.kinds.get(GeneratorKind.EMERALD).getTextures().contains(name))
        return GeneratorKind.EMERALD;
      if (this.kinds.get(GeneratorKind.DIAMOND).getTextures().contains(name))
        return GeneratorKind.DIAMOND;
    }
    return GeneratorKind.TEAM;
  }

  /** Max lucky blocks lying around uncollected per generator. */
  public int getGeneratorLimit() {
    return this.generatorLimit;
  }

  public boolean isDoNotSpawnIfTeamDead() {
    return this.doNotSpawnIfTeamDead;
  }

  /** Lower case names of the BedWars1058 arena groups that get automatic lucky block generators (config.yml and groups.yml). */
  public Set<String> getLuckyGroups() {
    Set<String> all = new LinkedHashSet<>(this.luckyGroups);
    all.addAll(this.extraGroups);
    return all;
  }

  /** The groups from config.yml only. */
  public Set<String> getConfigGroups() {
    return this.luckyGroups;
  }

  /** Replaces the groups added in-game. */
  public void setExtraGroups(Collection<String> groups) {
    Set<String> lower = new LinkedHashSet<>();
    for (String group : groups)
      lower.add(group.trim().toLowerCase(Locale.ROOT));
    this.extraGroups = Collections.unmodifiableSet(lower);
  }

  public boolean isLuckyGroup(String group) {
    if (group == null)
      return false;
    String key = group.trim().toLowerCase(Locale.ROOT);
    return this.luckyGroups.contains(key) || this.extraGroups.contains(key);
  }

  public KindSettings getKind(GeneratorKind kind) {
    return this.kinds.get(kind);
  }
}
