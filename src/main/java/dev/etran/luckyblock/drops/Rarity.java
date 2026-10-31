package dev.etran.luckyblock.drops;

import java.util.Locale;

/** How good a lucky block is. Each rarity has its own look (colors), loot table and name. */
public enum Rarity {
  COMMON("common"),
  UNCOMMON("uncommon"),
  RARE("rare"),
  LEGENDARY("legendary");

  private final String key;

  Rarity(String key) {
    this.key = key;
  }

  /** The key used in config.yml. */
  public String getKey() {
    return this.key;
  }

  /** The next better rarity (Legendary stays Legendary). */
  public Rarity better() {
    Rarity[] all = values();
    return all[Math.min(ordinal() + 1, all.length - 1)];
  }

  /** Null if the text is not a rarity. */
  public static Rarity fromKey(String text) {
    if (text == null)
      return null;
    String lower = text.trim().toLowerCase(Locale.ROOT);
    for (Rarity rarity : values()) {
      if (rarity.key.equals(lower))
        return rarity;
    }
    return null;
  }
}
