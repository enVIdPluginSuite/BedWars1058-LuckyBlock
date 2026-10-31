package dev.etran.luckyblock.drops

import dev.etran.luckyblock.data.Language
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DropTnt(private val language: Language) : Drop() {
    private val drop = ItemStack(Material.TNT)

    override fun drop(location: Location, player: Player) {
        val clone = drop.clone()
        val amount = getRandom().nextInt(9) + 1
        clone.amount = amount
        location.world.dropItem(location, clone)
        language.sendReceivedItemMessage(player, ChatColor.RED.toString() + "TNT", amount)
    }
}
