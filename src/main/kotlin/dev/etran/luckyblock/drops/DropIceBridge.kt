package dev.etran.luckyblock.drops

import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DropIceBridge(private val language: Language) : Drop() {
    private val drop: ItemStack

    init {
        val stack = ItemStack(Material.ICE)
        stack.itemMeta?.let { meta ->
            meta.displayName = ChatColor.GREEN.toString() + "Ice Bridge"
            stack.itemMeta = meta
        }
        drop = NBTUtils.applyNBTTags(stack, "iceBridge")
    }

    override fun drop(location: Location, player: Player) {
        val clone = drop.clone()
        clone.amount = 1
        location.world.dropItem(location, clone)
        language.sendReceivedItemMessage(player, ChatColor.GREEN.toString() + "Ice Bridge", 1)
    }
}
