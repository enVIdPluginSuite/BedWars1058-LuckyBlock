package dev.etran.luckyblock.drops;

import dev.etran.luckyblock.generator.GeneratorKind;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * How likely each drop is, per lucky block tier (team / diamond / emerald).
 * A drop with weight 20 comes up twice as often as one with weight 10; weight 0 means never.
 */
public final class LootTable {
  /** Used for a drop that has no weight in config.yml. */
  public static final int DEFAULT_WEIGHT = 10;

  private final Map<String, Map<GeneratorKind, Integer>> weights = new LinkedHashMap<>();

  public void set(String id, GeneratorKind tier, int weight) {
    this.weights.computeIfAbsent(id.toLowerCase(Locale.ROOT), key -> new EnumMap<>(GeneratorKind.class)).put(tier, Integer.valueOf(weight));
  }

  /** Ids that have weights in config.yml. */
  public Set<String> getIds() {
    return this.weights.keySet();
  }

  public int getWeight(GeneratorKind tier, String id) {
    Map<GeneratorKind, Integer> byTier = this.weights.get(id.toLowerCase(Locale.ROOT));
    Integer weight = (byTier == null) ? null : byTier.get(tier);
    return (weight == null) ? DEFAULT_WEIGHT : Math.max(0, weight.intValue());
  }

  /**
   * Picks one of the drops at random, weighted. If every weight for this tier is 0 all drops are
   * equally likely, so a config mistake never leaves a lucky block empty.
   *
   * @return null only if there are no drops at all
   */
  public String pick(GeneratorKind tier, Collection<String> ids, Random random) {
    if (ids.isEmpty())
      return null;
    int total = totalWeight(tier, ids);
    if (total <= 0) {
      int index = random.nextInt(ids.size());
      for (String id : ids) {
        if (index-- == 0)
          return id;
      }
    }
    int roll = random.nextInt(total);
    for (String id : ids) {
      roll -= getWeight(tier, id);
      if (roll < 0)
        return id;
    }
    return ids.iterator().next();
  }

  /** The chance (0-100) that a lucky block of this tier gives this drop. */
  public double chance(GeneratorKind tier, String id, Collection<String> ids) {
    int total = totalWeight(tier, ids);
    if (total <= 0)
      return ids.isEmpty() ? 0.0D : (100.0D / ids.size());
    return 100.0D * getWeight(tier, id) / total;
  }

  private int totalWeight(GeneratorKind tier, Collection<String> ids) {
    int total = 0;
    for (String id : ids)
      total += getWeight(tier, id);
    return total;
  }
}
