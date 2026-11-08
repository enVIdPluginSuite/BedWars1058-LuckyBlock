package dev.etran.luckyblock.drops

import java.util.Locale

/**
 * How good a lucky block is, named after the lucky block types of Hypixel Bed Wars. Each rarity has its own
 * look (colors), loot table and name.
 */
enum class Rarity(
    /** The key used in config.yml. */
    val key: String,
    /** The key older versions used for the same tier (still understood in config.yml and commands). */
    val legacyKey: String?
) {
    NORMAL("normal", "common"),
    PROMISING("promising", "uncommon"),
    FORTUNATE("fortunate", "rare"),
    OFFENSIVE("offensive", null),
    MIRACLE("miracle", "legendary");

    /** The next better rarity (Miracle stays Miracle). */
    fun better(): Rarity {
        val all = values()
        return all[minOf(ordinal + 1, all.size - 1)]
    }

    companion object {
        /** Null if the text is not a rarity. The old names (common, uncommon, rare, legendary) work too. */
        @JvmStatic
        fun fromKey(text: String?): Rarity? {
            if (text == null) return null
            val lower = text.trim().lowercase(Locale.ROOT)
            return values().firstOrNull { it.key == lower || it.legacyKey == lower }
        }

        /** "normal|promising|fortunate|offensive|miracle" for messages. */
        @JvmStatic
        fun keysText(): String = values().joinToString(", ") { it.key }

        /** Regex alternatives matching every key and old key, for commands. */
        @JvmStatic
        fun keysRegex(): String = values().flatMap { listOfNotNull(it.key, it.legacyKey) }.joinToString("|")
    }
}
