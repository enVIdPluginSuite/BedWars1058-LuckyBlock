package dev.etran.luckyblock.listeners

import dev.etran.luckyblock.arena.LuckyBlockArena
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemMergeEvent

/**
 * Lucky blocks dropped at the same spot merge into one stack. When that happens the generator has
 * to follow the surviving item, otherwise it would think the blocks were collected.
 */
class ItemMergeListener : Listener {
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onItemMerge(event: ItemMergeEvent) {
        val merged = event.entity.uniqueId
        val survivor = event.target.uniqueId
        for (arena in LuckyBlockArena.all()) {
            for (generator in arena.getGenerators()) {
                if (generator.isTracking(merged)) generator.track(survivor)
            }
        }
    }
}
