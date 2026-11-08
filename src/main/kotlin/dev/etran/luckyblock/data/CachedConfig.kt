package dev.etran.luckyblock.data

import dev.etran.luckyblock.drops.LootTable
import dev.etran.luckyblock.drops.Rarity
import dev.etran.luckyblock.generator.GeneratorKind
import java.util.EnumMap
import java.util.Locale
import java.util.Random

class CachedConfig(
    /** Max lucky blocks lying around uncollected per generator. */
    val generatorLimit: Int,
    val isDoNotSpawnIfTeamDead: Boolean,
    luckyGroups: Set<String>,
    kinds: Map<GeneratorKind, KindSettings>,
    /** How likely each drop is for each rarity of lucky block ("loot:" in config.yml). */
    val loot: LootTable,
    rarities: Map<Rarity, RaritySettings>,
    /** Weights for the rarity roll of each kind of generator. */
    rarityChances: Map<GeneratorKind, Map<Rarity, Int>>,
    /** Percent chance (at the exact middle of the map) that a lucky block is upgraded one rarity. */
    val middleUpgrade: Int,
    /** True if lucky blocks speed up with the upgrades of the real generator next to them. */
    val isSyncUpgrades: Boolean,
    val hologram: HologramSettings,
    /** Name of a lucky block item; "{rarity}" is replaced by the rarity name. */
    val luckyBlockName: String
) {
    /** config.yml "generators.<kind>" section. */
    class KindSettings(
        val isEnabled: Boolean,
        /** Seconds between lucky blocks while the real generator runs at its starting speed. */
        val cooldown: Int
    )

    /** config.yml "rarities.<rarity>" section. */
    class RaritySettings(
        /** The name with color codes as written in config.yml (e.g. "&9Rare"). */
        val display: String,
        textures: List<String>
    ) {
        /** Names (keys of textures.yml, lower case) of the lucky block colors of this rarity. */
        val textures: List<String> = textures.toList()
    }

    /** config.yml "hologram" section. */
    class HologramSettings(
        val isEnabled: Boolean,
        /** Height above a team generator. */
        val teamHeight: Double,
        /** Height above a diamond / emerald generator (above BedWars1058's own hologram). */
        val oreHeight: Double
    )

    private val luckyGroups: Set<String> = LinkedHashSet(luckyGroups)

    /** Groups added in-game with /bwlb group add (groups.yml), lower case. */
    @Volatile
    private var extraGroups: Set<String> = emptySet()

    private val kinds: Map<GeneratorKind, KindSettings> = EnumMap(kinds)
    private val rarities: Map<Rarity, RaritySettings> = EnumMap(rarities)
    private val rarityChances: Map<GeneratorKind, Map<Rarity, Int>> = EnumMap(rarityChances)

    fun getRarity(rarity: Rarity): RaritySettings = rarities.getValue(rarity)

    /**
     * The rarity of a lucky block color. A color listed under several rarities counts as the best of
     * them, and a color listed under none counts as Normal.
     */
    fun rarityOfTexture(textureName: String?): Rarity {
        if (textureName != null) {
            val name = textureName.lowercase(Locale.ROOT)
            for (rarity in Rarity.values().reversed()) {
                if (getRarity(rarity).textures.contains(name)) return rarity
            }
        }
        return Rarity.NORMAL
    }

    /** Weight of a rarity in the roll of a generator kind. */
    fun getRarityWeight(kind: GeneratorKind, rarity: Rarity): Int =
        maxOf(0, rarityChances[kind]?.get(rarity) ?: 0)

    /** The chance in percent (0-100) that a generator of this kind rolls this rarity, before the middle bonus. */
    fun getRarityChance(kind: GeneratorKind, rarity: Rarity): Double {
        val total = Rarity.values().sumOf { getRarityWeight(kind, it) }
        if (total <= 0) return if (rarity == Rarity.NORMAL) 100.0 else 0.0
        return 100.0 * getRarityWeight(kind, rarity) / total
    }

    /**
     * Rolls the rarity of the next lucky block of a generator.
     *
     * @param centrality 0 at the edge of the map, 1 in the very middle: the closer to the middle, the more likely
     *                   the rarity is upgraded one step (up to "middle-upgrade" percent)
     */
    fun rollRarity(kind: GeneratorKind, centrality: Double, random: Random): Rarity {
        val total = Rarity.values().sumOf { getRarityWeight(kind, it) }
        var rolled = Rarity.NORMAL
        if (total > 0) {
            var roll = random.nextInt(total)
            for (r in Rarity.values()) {
                roll -= getRarityWeight(kind, r)
                if (roll < 0) {
                    rolled = r
                    break
                }
            }
        }
        val upgrade = middleUpgrade * maxOf(0.0, minOf(1.0, centrality))
        if (upgrade > 0.0 && random.nextDouble() * 100.0 < upgrade) {
            // never past the best rarity this kind of generator can make (miracle is for emerald generators only)
            var best = Rarity.NORMAL
            for (r in Rarity.values()) {
                if (getRarityWeight(kind, r) > 0) best = r
            }
            if (rolled.ordinal < best.ordinal) rolled = rolled.better()
        }
        return rolled
    }

    /** Lower case names of the BedWars1058 arena groups that get automatic lucky block generators (config.yml and groups.yml). */
    fun getLuckyGroups(): Set<String> {
        val all = LinkedHashSet(luckyGroups)
        all.addAll(extraGroups)
        return all
    }

    /** The groups from config.yml only. */
    fun getConfigGroups(): Set<String> = luckyGroups

    /** Replaces the groups added in-game. */
    fun setExtraGroups(groups: Collection<String>) {
        extraGroups = groups.map { it.trim().lowercase(Locale.ROOT) }.toCollection(LinkedHashSet())
    }

    fun isLuckyGroup(group: String?): Boolean {
        if (group == null) return false
        val key = group.trim().lowercase(Locale.ROOT)
        return luckyGroups.contains(key) || extraGroups.contains(key)
    }

    fun getKind(kind: GeneratorKind): KindSettings = kinds.getValue(kind)
}
