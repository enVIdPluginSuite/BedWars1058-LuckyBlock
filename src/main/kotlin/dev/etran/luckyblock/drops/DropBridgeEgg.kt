package dev.etran.luckyblock.drops

import dev.etran.luckyblock.data.Language
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DropBridgeEgg(private val language: Language) : Drop() {
    private val drop: ItemStack = ItemStack(Material.EGG).also { stack ->
        stack.itemMeta?.let { meta ->
            meta.displayName = ChatColor.AQUA.toString() + "Bridge Egg"
            stack.itemMeta = meta
        }
    }

    override fun drop(location: Location, player: Player) {
        val clone = drop.clone()
        val amount = getRandom().nextInt(3) + 1
        clone.amount = amount
        location.world.dropItem(location, clone)
        language.sendReceivedItemMessage(player, ChatColor.AQUA.toString() + "Bridge Egg", amount)
    }
}
