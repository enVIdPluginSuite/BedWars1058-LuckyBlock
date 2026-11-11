package dev.etran.luckyblock.drops

import dev.etran.luckyblock.data.Language
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.Random

/** A drop that just hands out some items, built by a small function so the catalog stays short. */
class SimpleDrop(private val language: Language, private val label: String, private val maker: Maker) : Drop() {

    /** Makes the items of one drop. The first stack decides the amount shown in the chat message. */
    fun interface Maker {
        fun make(player: Player, random: Random): Array<ItemStack>
    }

    override fun drop(location: Location, player: Player) {
        val items = maker.make(player, getRandom())
        if (items.isEmpty()) return
        for (item in items) {
            // BedWars1058 deletes every bed (and seed) item that spawns in a game world, so those go into the inventory
            if (dev.etran.luckyblock.abilities.Kit.isBed(item.type)) dev.etran.luckyblock.abilities.Kit.give(player, item)
            else location.world.dropItemNaturally(location, item)
        }
        language.sendReceivedItemMessage(player, label, items[0].amount)
    }
}
