package dev.etran.luckyblock.drops;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * How likely each drop is, per rarity of lucky block. A drop with weight 20 comes up twice as often as one
 * with weight 10; weight 0 (or not listed) means never.
 */
public final class LootTable {
  private final Map<String, Map<Rarity, Integer>> weights = new LinkedHashMap<>();

  public void set(String id, Rarity rarity, int weight) {
    this.weights.computeIfAbsent(id.toLowerCase(Locale.ROOT), key -> new EnumMap<>(Rarity.class)).put(rarity, Integer.valueOf(weight));
  }

  /** Ids that have a weight in config.yml for any rarity. */
  public Set<String> getIds() {
    return this.weights.keySet();
  }

  public int getWeight(Rarity rarity, String id) {
    Map<Rarity, Integer> byRarity = this.weights.get(id.toLowerCase(Locale.ROOT));
    Integer weight = (byRarity == null) ? null : byRarity.get(rarity);
    return (weight == null) ? 0 : Math.max(0, weight.intValue());
  }

  /**
   * Picks one of the drops at random, weighted. If every weight for this rarity is 0 all drops are
   * equally likely, so a config mistake never leaves a lucky block empty.
   *
   * @return null only if there are no drops at all
   */
  public String pick(Rarity rarity, Collection<String> ids, Random random) {
    if (ids.isEmpty())
      return null;
    int total = totalWeight(rarity, ids);
    if (total <= 0) {
      int index = random.nextInt(ids.size());
      for (String id : ids) {
        if (index-- == 0)
          return id;
      }
    }
    int roll = random.nextInt(total);
    for (String id : ids) {
      roll -= getWeight(rarity, id);
      if (roll < 0)
        return id;
    }
    return ids.iterator().next();
  }

  /** The chance (0-100) that a lucky block of this rarity gives this drop. */
  public double chance(Rarity rarity, String id, Collection<String> ids) {
    int total = totalWeight(rarity, ids);
    if (total <= 0)
      return ids.isEmpty() ? 0.0D : (100.0D / ids.size());
    return 100.0D * getWeight(rarity, id) / total;
  }

  public int totalWeight(Rarity rarity, Collection<String> ids) {
    int total = 0;
    for (String id : ids)
      total += getWeight(rarity, id);
    return total;
  }
}
