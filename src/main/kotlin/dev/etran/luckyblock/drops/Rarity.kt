package dev.etran.luckyblock.drops

import java.util.Locale

/** How good a lucky block is. Each rarity has its own look (colors), loot table and name. */
enum class Rarity(
    /** The key used in config.yml. */
    val key: String
) {
    COMMON("common"),
    UNCOMMON("uncommon"),
    RARE("rare"),
    LEGENDARY("legendary");

    /** The next better rarity (Legendary stays Legendary). */
    fun better(): Rarity {
        val all = values()
        return all[minOf(ordinal + 1, all.size - 1)]
    }

    companion object {
        /** Null if the text is not a rarity. */
        @JvmStatic
        fun fromKey(text: String?): Rarity? {
            if (text == null) return null
            val lower = text.trim().lowercase(Locale.ROOT)
            return values().firstOrNull { it.key == lower }
        }
    }
}
