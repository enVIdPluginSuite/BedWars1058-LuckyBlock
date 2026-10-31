package dev.etran.luckyblock.drops

import java.util.EnumMap
import java.util.LinkedHashMap
import java.util.Locale
import java.util.Random

/**
 * How likely each drop is, per rarity of lucky block. A drop with weight 20 comes up twice as often as one
 * with weight 10; weight 0 (or not listed) means never.
 */
class LootTable {
    private val weights: MutableMap<String, MutableMap<Rarity, Int>> = LinkedHashMap()

    fun set(id: String, rarity: Rarity, weight: Int) {
        weights.getOrPut(id.lowercase(Locale.ROOT)) { EnumMap(Rarity::class.java) }[rarity] = weight
    }

    /** Ids that have a weight in config.yml for any rarity. */
    val ids: Set<String> get() = weights.keys

    fun getWeight(rarity: Rarity, id: String): Int =
        maxOf(0, weights[id.lowercase(Locale.ROOT)]?.get(rarity) ?: 0)

    /**
     * Picks one of the drops at random, weighted. If every weight for this rarity is 0 all drops are
     * equally likely, so a config mistake never leaves a lucky block empty.
     *
     * @return null only if there are no drops at all
     */
    fun pick(rarity: Rarity, ids: Collection<String>, random: Random): String? {
        if (ids.isEmpty()) return null
        val total = totalWeight(rarity, ids)
        if (total <= 0) return ids.elementAt(random.nextInt(ids.size))
        var roll = random.nextInt(total)
        for (id in ids) {
            roll -= getWeight(rarity, id)
            if (roll < 0) return id
        }
        return ids.first()
    }

    /** The chance (0-100) that a lucky block of this rarity gives this drop. */
    fun chance(rarity: Rarity, id: String, ids: Collection<String>): Double {
        val total = totalWeight(rarity, ids)
        if (total <= 0) return if (ids.isEmpty()) 0.0 else 100.0 / ids.size
        return 100.0 * getWeight(rarity, id) / total
    }

    fun totalWeight(rarity: Rarity, ids: Collection<String>): Int = ids.sumOf { getWeight(rarity, it) }
}
