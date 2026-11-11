package dev.etran.luckyblock.data

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.arena.LoadedArena
import dev.etran.luckyblock.drops.Drop
import dev.etran.luckyblock.drops.LootTable
import dev.etran.luckyblock.drops.Rarity
import dev.etran.luckyblock.generator.GeneratorKind
import dev.etran.luckyblock.generator.LoadedGenerator
import dev.etran.luckyblock.luckyblock.texture.GlassColor
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import org.bukkit.configuration.file.FileConfiguration
import java.util.EnumMap
import java.util.Locale
import java.util.logging.Logger

object LoadData {

    /** Reads data.yml (the manual generators). Broken lines are skipped with a warning. */
    @JvmStatic
    fun loadData(data: FileConfiguration, logger: Logger) {
        for (key in data.getKeys(false)) {
            val section = data.getConfigurationSection(key) ?: continue
            val arena = LoadedArena.getOrCreate(key)
            for (id in section.getKeys(false)) {
                try {
                    arena.addGenerator(LoadedGenerator.parse(key, id, section.getString(id)))
                } catch (e: IllegalArgumentException) {
                    logger.warning("Skipping data.yml entry $key.$id: ${e.message}")
                }
            }
        }
    }

    @JvmStatic
    fun loadTextures(data: FileConfiguration) {
        for (key in data.getKeys(false)) {
            val section = data.getConfigurationSection(key) ?: continue
            try {
                LuckyBlockTexture.addTexture(
                    key,
                    section.getString("texture"),
                    GlassColor.valueOf(section.getString("glass").uppercase(Locale.ROOT)),
                    section.getString("item-name"),
                    section.getStringList("item-lore")
                )
            } catch (e: RuntimeException) {
                LuckyBlocks.getInstance().logger.warning("Skipping texture \"$key\" in textures.yml: ${e.message}")
            }
        }
        val (heads, glass) = LuckyBlockTexture.headCounts()
        LuckyBlocks.getInstance().logger.info("Lucky block textures: $heads with a skull skin, $glass as stained glass.")
    }

    @JvmStatic
    fun loadLang(data: FileConfiguration): Language = Language(
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
        data.getStringList("lightning-scroll.lore")
    )

    /**
     * Reads config.yml. Missing keys fall back to the defaults bundled in the jar when the
     * configuration was loaded through JavaPlugin#getConfig(), so older config files keep working.
     */
    @JvmStatic
    fun loadConfig(config: FileConfiguration): CachedConfig {
        val groups = LinkedHashSet<String>()
        for (group in config.getStringList("groups")) {
            if (group != null && group.trim().isNotEmpty()) groups.add(group.trim().lowercase(Locale.ROOT))
        }
        val kinds = EnumMap<GeneratorKind, CachedConfig.KindSettings>(GeneratorKind::class.java)
        val chances = EnumMap<GeneratorKind, Map<Rarity, Int>>(GeneratorKind::class.java)
        for (kind in GeneratorKind.values()) {
            val base = "generators." + kind.configKey
            kinds[kind] = CachedConfig.KindSettings(
                boolOf(config, "$base.enabled", true),
                maxOf(1, intOf(config, "$base.cooldown", 30))
            )
            val byRarity = EnumMap<Rarity, Int>(Rarity::class.java)
            for (rarity in Rarity.values())
                byRarity[rarity] = maxOf(0, intOf(config, "rarity-chances.${kind.configKey}.${rarity.key}", 0))
            chances[kind] = byRarity
        }
        val rarities = EnumMap<Rarity, CachedConfig.RaritySettings>(Rarity::class.java)
        for (rarity in Rarity.values()) {
            val base = "rarities." + rarity.key
            val textures = config.getStringList("$base.textures")
                .filter { it != null && it.trim().isNotEmpty() }
                .map { it.trim().lowercase(Locale.ROOT) }
            val display = if (config.contains("$base.name")) config.getString("$base.name") else rarity.key
            rarities[rarity] = CachedConfig.RaritySettings(display, textures)
        }
        val hologram = CachedConfig.HologramSettings(
            boolOf(config, "hologram.enabled", true),
            doubleOf(config, "hologram.height-team", 2.4),
            doubleOf(config, "hologram.height-ore", 3.8)
        )
        return CachedConfig(
            maxOf(1, intOf(config, "generator-limit", 10)),
            boolOf(config, "do-not-spawn-if-team-dead", true),
            groups,
            kinds,
            loadLoot(config),
            rarities,
            chances,
            maxOf(0, minOf(100, intOf(config, "middle-upgrade", 30))),
            boolOf(config, "sync-upgrades", true),
            hologram,
            if (config.contains("lucky-block-name")) config.getString("lucky-block-name") else "{rarity} &fLucky Block"
        )
    }

    // Bukkit's getInt(path, fallback) / getBoolean(path, fallback) ignore the defaults bundled in the jar, so
    // an older config.yml would get the fallback instead of the real default. Going through contains() and the
    // one-argument getters does use the bundled defaults, and the fallback only applies if neither has the key.
    private fun intOf(config: FileConfiguration, path: String, fallback: Int): Int =
        if (config.contains(path)) config.getInt(path) else fallback

    private fun doubleOf(config: FileConfiguration, path: String, fallback: Double): Double =
        if (config.contains(path)) config.getDouble(path) else fallback

    private fun boolOf(config: FileConfiguration, path: String, fallback: Boolean): Boolean =
        if (config.contains(path)) config.getBoolean(path) else fallback

    /**
     * Reads "loot:" (rarity -> drop id -> weight). Drops missing from the file itself are filled in from
     * the defaults bundled in the jar, so an older config.yml still gets sensible loot.
     */
    @JvmStatic
    fun loadLoot(config: FileConfiguration): LootTable {
        val loot = LootTable()
        for (rarity in Rarity.values()) {
            val base = "loot." + rarity.key
            val ids = LinkedHashSet<String>()
            config.getConfigurationSection(base)?.let { ids.addAll(it.getKeys(false)) }
            config.defaults?.getConfigurationSection(base)?.let { ids.addAll(it.getKeys(false)) }
            for (id in ids) loot.set(id, rarity, intOf(config, "$base.$id", 0))
        }
        return loot
    }

    /** Logs loot entries that match no drop, and rarities whose weights are all 0 (called once the drops are registered). */
    @JvmStatic
    fun validateLoot(config: CachedConfig) {
        val logger = LuckyBlocks.getInstance().logger
        for (id in config.loot.ids) {
            if (!Drop.ids().contains(id))
                logger.warning("config.yml: loot lists \"$id\", which is not a drop. The drops are: ${Drop.ids().joinToString(", ")}")
        }
        for (rarity in Rarity.values()) {
            if (config.loot.totalWeight(rarity, Drop.ids()) <= 0)
                logger.warning("config.yml: every loot weight for ${rarity.key} is 0, so all drops will be equally likely for it.")
        }
    }

    /** Logs texture names in config.yml that don't exist in textures.yml (they would be ignored). */
    @JvmStatic
    fun validateConfig(config: CachedConfig) {
        val logger = LuckyBlocks.getInstance().logger
        for (rarity in Rarity.values()) {
            val settings = config.getRarity(rarity)
            for (name in LuckyBlockTexture.unknownNames(settings.textures))
                logger.warning("config.yml: rarities.${rarity.key}.textures lists \"$name\", which does not exist in textures.yml")
        }
        for (kind in GeneratorKind.values()) {
            val total = Rarity.values().sumOf { config.getRarityWeight(kind, it) }
            if (total <= 0)
                logger.warning("config.yml: every rarity-chances entry for ${kind.configKey} is 0, so it will only make Normal lucky blocks.")
        }
    }
}
