package dev.etran.luckyblock.drops;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.generator.GeneratorKind;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public abstract class Drop {
  private static final Random random = new Random();

  /** Every drop by id (the names used under "loot:" in config.yml), in registration order. */
  private static final Map<String, Drop> drops = new LinkedHashMap<>();

  /** A random drop for a lucky block of the given tier, using the weights from config.yml. */
  public static Drop randomDrop(GeneratorKind tier) {
    String id = LuckyBlocks.getInstance().getCachedConfig().getLoot().pick(tier, drops.keySet(), random);
    return (id == null) ? null : drops.get(id);
  }

  public static void registerDrop(String id, Drop drop) {
    drops.put(id.toLowerCase(Locale.ROOT), drop);
  }

  public static Set<String> ids() {
    return Collections.unmodifiableSet(drops.keySet());
  }

  public abstract void drop(Location paramLocation, Player paramPlayer);

  public Random getRandom() {
    return random;
  }
}
