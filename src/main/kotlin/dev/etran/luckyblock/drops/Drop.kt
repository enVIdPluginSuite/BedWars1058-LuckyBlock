package dev.etran.luckyblock.drops

import dev.etran.luckyblock.LuckyBlocks
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.Locale
import java.util.Random

abstract class Drop {

    abstract fun drop(location: Location, player: Player)

    fun getRandom(): Random = random

    companion object {
        private val random = Random()

        /** Every drop by id (the names used under "loot:" in config.yml), in registration order. */
        private val drops: MutableMap<String, Drop> = LinkedHashMap()

        /** A random drop for a lucky block of the given rarity, using the weights from config.yml. */
        @JvmStatic
        fun randomDrop(rarity: Rarity): Drop? {
            val id = LuckyBlocks.getInstance().cachedConfig.loot.pick(rarity, drops.keys, random)
            return if (id == null) null else drops[id]
        }

        @JvmStatic
        fun get(id: String): Drop? = drops[id.lowercase(Locale.ROOT)]

        @JvmStatic
        fun registerDrop(id: String, drop: Drop) {
            drops[id.lowercase(Locale.ROOT)] = drop
        }

        /** Forgets every drop (used when the plugin is reloaded and the drops are registered again). */
        @JvmStatic
        fun clear() {
            drops.clear()
        }

        @JvmStatic
        fun ids(): Set<String> = java.util.Collections.unmodifiableSet(drops.keys)
    }
}
