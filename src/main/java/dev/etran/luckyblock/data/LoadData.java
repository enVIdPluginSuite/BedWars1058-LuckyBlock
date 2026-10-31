package dev.etran.luckyblock.data;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.arena.LoadedArena;
import dev.etran.luckyblock.drops.Drop;
import dev.etran.luckyblock.drops.LootTable;
import dev.etran.luckyblock.drops.Rarity;
import dev.etran.luckyblock.generator.GeneratorKind;
import dev.etran.luckyblock.generator.LoadedGenerator;
import dev.etran.luckyblock.luckyblock.texture.GlassColor;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class LoadData {

  /** Reads data.yml (the manual generators). Broken lines are skipped with a warning. */
  public static void loadData(FileConfiguration data, Logger logger) {
    for (String key : data.getKeys(false)) {
      ConfigurationSection section = data.getConfigurationSection(key);
      if (section == null)
        continue;
      LoadedArena arena = LoadedArena.getOrCreate(key);
      for (String id : section.getKeys(false)) {
        try {
          arena.addGenerator(LoadedGenerator.parse(key, id, section.getString(id)));
        } catch (IllegalArgumentException e) {
          logger.warning("Skipping data.yml entry " + key + "." + id + ": " + e.getMessage());
        }
      }
    }
  }

  public static void loadTextures(FileConfiguration data) {
    for (String key : data.getKeys(false)) {
      ConfigurationSection section = data.getConfigurationSection(key);
      if (section == null)
        continue;
      try {
        LuckyBlockTexture.addTexture(
            key,
            section.getString("texture"),
            GlassColor.valueOf(section.getString("glass").toUpperCase(Locale.ROOT)),
            section.getString("item-name"),
            section.getStringList("item-lore"));
      } catch (RuntimeException e) {
        LuckyBlocks.getInstance().getLogger().warning("Skipping texture \"" + key + "\" in textures.yml: " + e.getMessage());
      }
    }
  }

  public static Language loadLang(FileConfiguration data) {
    return new Language(
        data.getString("received-item"),
        data.getString("used-inventory-copier.player"),
        data.getString("used-inventory-copier.target"),
        data.getString("used-lightning-scroll.player"),
        data.getString("used-lightning-scroll.target"),
        data.getString("used-player-launcher.type-name-to-launch"),
        data.getString("used-player-launcher.player"),
        data.getString("used-player-launcher.target"),
        data.getString("rainbow-wool.name"),
        data.getStringList("rainbow-wool.lore"),
        data.getString("inventory-copier.name"),
        data.getStringList("inventory-copier.lore"),
        data.getString("player-launcher.name"),
        data.getStringList("player-launcher.lore"),
        data.getString("lightning-scroll.name"),
        data.getStringList("lightning-scroll.lore"));
  }

  /**
   * Reads config.yml. Missing keys fall back to the defaults bundled in the jar when the
   * configuration was loaded through JavaPlugin#getConfig(), so older config files keep working.
   */
  public static CachedConfig loadConfig(FileConfiguration config) {
    Set<String> groups = new LinkedHashSet<>();
    for (String group : config.getStringList("groups")) {
      if (group != null && !group.trim().isEmpty())
        groups.add(group.trim().toLowerCase(Locale.ROOT));
    }
    Map<GeneratorKind, CachedConfig.KindSettings> kinds = new EnumMap<>(GeneratorKind.class);
    Map<GeneratorKind, Map<Rarity, Integer>> chances = new EnumMap<>(GeneratorKind.class);
    for (GeneratorKind kind : GeneratorKind.values()) {
      String base = "generators." + kind.getConfigKey();
      kinds.put(kind, new CachedConfig.KindSettings(
            boolOf(config, base + ".enabled", true),
            Math.max(1, intOf(config, base + ".cooldown", 30))));
      Map<Rarity, Integer> byRarity = new EnumMap<>(Rarity.class);
      for (Rarity rarity : Rarity.values())
        byRarity.put(rarity, Integer.valueOf(Math.max(0, intOf(config, "rarity-chances." + kind.getConfigKey() + "." + rarity.getKey(), 0))));
      chances.put(kind, byRarity);
    }
    Map<Rarity, CachedConfig.RaritySettings> rarities = new EnumMap<>(Rarity.class);
    for (Rarity rarity : Rarity.values()) {
      String base = "rarities." + rarity.getKey();
      List<String> textures = new ArrayList<>();
      for (String name : config.getStringList(base + ".textures")) {
        if (name != null && !name.trim().isEmpty())
          textures.add(name.trim().toLowerCase(Locale.ROOT));
      }
      String display = config.contains(base + ".name") ? config.getString(base + ".name") : rarity.getKey();
      rarities.put(rarity, new CachedConfig.RaritySettings(display, textures));
    }
    CachedConfig.HologramSettings hologram = new CachedConfig.HologramSettings(
        boolOf(config, "hologram.enabled", true),
        doubleOf(config, "hologram.height-team", 2.4D),
        doubleOf(config, "hologram.height-ore", 3.8D));
    return new CachedConfig(
        Math.max(1, intOf(config, "generator-limit", 10)),
        boolOf(config, "do-not-spawn-if-team-dead", true),
        groups,
        kinds,
        loadLoot(config),
        rarities,
        chances,
        Math.max(0, Math.min(100, intOf(config, "middle-upgrade", 30))),
        boolOf(config, "sync-upgrades", true),
        hologram,
        config.contains("lucky-block-name") ? config.getString("lucky-block-name") : "{rarity} &fLucky Block");
  }

  // Bukkit's getInt(path, fallback) / getBoolean(path, fallback) ignore the defaults bundled in the jar, so
  // an older config.yml would get the fallback instead of the real default. Going through contains() and the
  // one-argument getters does use the bundled defaults, and the fallback only applies if neither has the key.
  private static int intOf(FileConfiguration config, String path, int fallback) {
    return config.contains(path) ? config.getInt(path) : fallback;
  }

  private static double doubleOf(FileConfiguration config, String path, double fallback) {
    return config.contains(path) ? config.getDouble(path) : fallback;
  }

  private static boolean boolOf(FileConfiguration config, String path, boolean fallback) {
    return config.contains(path) ? config.getBoolean(path) : fallback;
  }

  /**
   * Reads "loot:" (rarity -> drop id -> weight). Drops missing from the file itself are filled in from
   * the defaults bundled in the jar, so an older config.yml still gets sensible loot.
   */
  static LootTable loadLoot(FileConfiguration config) {
    LootTable loot = new LootTable();
    for (Rarity rarity : Rarity.values()) {
      String base = "loot." + rarity.getKey();
      Set<String> ids = new LinkedHashSet<>();
      ConfigurationSection section = config.getConfigurationSection(base);
      if (section != null)
        ids.addAll(section.getKeys(false));
      if (config.getDefaults() != null) {
        ConfigurationSection defaults = config.getDefaults().getConfigurationSection(base);
        if (defaults != null)
          ids.addAll(defaults.getKeys(false));
      }
      for (String id : ids)
        loot.set(id, rarity, intOf(config, base + "." + id, 0));
    }
    return loot;
  }

  /** Logs loot entries that match no drop, and rarities whose weights are all 0 (called once the drops are registered). */
  public static void validateLoot(CachedConfig config) {
    Logger logger = LuckyBlocks.getInstance().getLogger();
    for (String id : config.getLoot().getIds()) {
      if (!Drop.ids().contains(id))
        logger.warning("config.yml: loot lists \"" + id + "\", which is not a drop. The drops are: " + String.join(", ", (Iterable<? extends CharSequence>)Drop.ids()));
    }
    for (Rarity rarity : Rarity.values()) {
      if (config.getLoot().totalWeight(rarity, Drop.ids()) <= 0)
        logger.warning("config.yml: every loot weight for " + rarity.getKey() + " is 0, so all drops will be equally likely for it.");
    }
  }

  /** Logs texture names in config.yml that don't exist in textures.yml (they would be ignored). */
  public static void validateConfig(CachedConfig config) {
    Logger logger = LuckyBlocks.getInstance().getLogger();
    for (Rarity rarity : Rarity.values()) {
      CachedConfig.RaritySettings settings = config.getRarity(rarity);
      for (String name : LuckyBlockTexture.unknownNames(settings.getTextures()))
        logger.warning("config.yml: rarities." + rarity.getKey() + ".textures lists \"" + name + "\", which does not exist in textures.yml");
    }
    for (GeneratorKind kind : GeneratorKind.values()) {
      int total = 0;
      for (Rarity rarity : Rarity.values())
        total += config.getRarityWeight(kind, rarity);
      if (total <= 0)
        logger.warning("config.yml: every rarity-chances entry for " + kind.getConfigKey() + " is 0, so it will only make Common lucky blocks.");
    }
  }
}
