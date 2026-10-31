package dev.etran.luckyblock.data;

import dev.etran.luckyblock.drops.LootTable;
import dev.etran.luckyblock.drops.Rarity;
import dev.etran.luckyblock.generator.GeneratorKind;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CachedConfig {

  /** config.yml "generators.<kind>" section. */
  public static final class KindSettings {
    private final boolean enabled;

    private final int cooldown;

    public KindSettings(boolean enabled, int cooldown) {
      this.enabled = enabled;
      this.cooldown = cooldown;
    }

    public boolean isEnabled() {
      return this.enabled;
    }

    /** Seconds between lucky blocks while the real generator runs at its starting speed. */
    public int getCooldown() {
      return this.cooldown;
    }
  }

  /** config.yml "rarities.<rarity>" section. */
  public static final class RaritySettings {
    private final String display;

    private final List<String> textures;

    public RaritySettings(String display, List<String> textures) {
      this.display = display;
      this.textures = Collections.unmodifiableList(textures);
    }

    /** The name with color codes as written in config.yml (e.g. "&9Rare"). */
    public String getDisplay() {
      return this.display;
    }

    /** Names (keys of textures.yml, lower case) of the lucky block colors of this rarity. */
    public List<String> getTextures() {
      return this.textures;
    }
  }

  /** config.yml "hologram" section. */
  public static final class HologramSettings {
    private final boolean enabled;

    private final double teamHeight;

    private final double oreHeight;

    public HologramSettings(boolean enabled, double teamHeight, double oreHeight) {
      this.enabled = enabled;
      this.teamHeight = teamHeight;
      this.oreHeight = oreHeight;
    }

    public boolean isEnabled() {
      return this.enabled;
    }

    /** Height above a team generator. */
    public double getTeamHeight() {
      return this.teamHeight;
    }

    /** Height above a diamond / emerald generator (above BedWars1058's own hologram). */
    public double getOreHeight() {
      return this.oreHeight;
    }
  }

  private final int generatorLimit;

  private final boolean doNotSpawnIfTeamDead;

  private final Set<String> luckyGroups;

  /** Groups added in-game with /bwlb group add (groups.yml), lower case. */
  private volatile Set<String> extraGroups = Collections.emptySet();

  private final Map<GeneratorKind, KindSettings> kinds;

  private final LootTable loot;

  private final Map<Rarity, RaritySettings> rarities;

  /** Weights for the rarity roll of each kind of generator. */
  private final Map<GeneratorKind, Map<Rarity, Integer>> rarityChances;

  private final int middleUpgrade;

  private final boolean syncUpgrades;

  private final HologramSettings hologram;

  private final String luckyBlockName;

  public CachedConfig(int generatorLimit, boolean doNotSpawnIfTeamDead, Set<String> luckyGroups, Map<GeneratorKind, KindSettings> kinds, LootTable loot,
      Map<Rarity, RaritySettings> rarities, Map<GeneratorKind, Map<Rarity, Integer>> rarityChances, int middleUpgrade, boolean syncUpgrades,
      HologramSettings hologram, String luckyBlockName) {
    this.generatorLimit = generatorLimit;
    this.doNotSpawnIfTeamDead = doNotSpawnIfTeamDead;
    this.luckyGroups = Collections.unmodifiableSet(luckyGroups);
    this.kinds = new EnumMap<>(kinds);
    this.loot = loot;
    this.rarities = new EnumMap<>(rarities);
    this.rarityChances = new EnumMap<>(rarityChances);
    this.middleUpgrade = middleUpgrade;
    this.syncUpgrades = syncUpgrades;
    this.hologram = hologram;
    this.luckyBlockName = luckyBlockName;
  }

  /** How likely each drop is for each rarity of lucky block ("loot:" in config.yml). */
  public LootTable getLoot() {
    return this.loot;
  }

  public RaritySettings getRarity(Rarity rarity) {
    return this.rarities.get(rarity);
  }

  /**
   * The rarity of a lucky block color. A color listed under several rarities counts as the best of
   * them, and a color listed under none counts as Common.
   */
  public Rarity rarityOfTexture(String textureName) {
    if (textureName != null) {
      String name = textureName.toLowerCase(Locale.ROOT);
      Rarity[] all = Rarity.values();
      for (int i = all.length - 1; i >= 0; i--) {
        if (this.rarities.get(all[i]).getTextures().contains(name))
          return all[i];
      }
    }
    return Rarity.COMMON;
  }

  /** Weight of a rarity in the roll of a generator kind. */
  public int getRarityWeight(GeneratorKind kind, Rarity rarity) {
    Integer weight = this.rarityChances.get(kind).get(rarity);
    return (weight == null) ? 0 : Math.max(0, weight.intValue());
  }

  /** The chance in percent (0-100) that a generator of this kind rolls this rarity, before the middle bonus. */
  public double getRarityChance(GeneratorKind kind, Rarity rarity) {
    int total = 0;
    for (Rarity r : Rarity.values())
      total += getRarityWeight(kind, r);
    if (total <= 0)
      return (rarity == Rarity.COMMON) ? 100.0D : 0.0D;
    return 100.0D * getRarityWeight(kind, rarity) / total;
  }

  /**
   * Rolls the rarity of the next lucky block of a generator.
   *
   * @param centrality 0 at the edge of the map, 1 in the very middle: the closer to the middle, the more likely
   *                   the rarity is upgraded one step (up to "middle-upgrade" percent)
   */
  public Rarity rollRarity(GeneratorKind kind, double centrality, Random random) {
    int total = 0;
    for (Rarity r : Rarity.values())
      total += getRarityWeight(kind, r);
    Rarity rolled = Rarity.COMMON;
    if (total > 0) {
      int roll = random.nextInt(total);
      for (Rarity r : Rarity.values()) {
        roll -= getRarityWeight(kind, r);
        if (roll < 0) {
          rolled = r;
          break;
        }
      }
    }
    double upgrade = this.middleUpgrade * Math.max(0.0D, Math.min(1.0D, centrality));
    if (upgrade > 0.0D && random.nextDouble() * 100.0D < upgrade)
      rolled = rolled.better();
    return rolled;
  }

  /** Percent chance (at the exact middle of the map) that a lucky block is upgraded one rarity. */
  public int getMiddleUpgrade() {
    return this.middleUpgrade;
  }

  /** True if lucky blocks speed up with the upgrades of the real generator next to them. */
  public boolean isSyncUpgrades() {
    return this.syncUpgrades;
  }

  public HologramSettings getHologram() {
    return this.hologram;
  }

  /** Name of a lucky block item; "{rarity}" is replaced by the rarity name. */
  public String getLuckyBlockName() {
    return this.luckyBlockName;
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
