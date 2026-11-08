package dev.etran.luckyblock.drops

import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.data.Language
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DropFireball(private val language: Language) : Drop() {
    private val drop = ItemStack(Items.fireCharge())

    override fun drop(location: Location, player: Player) {
        val clone = drop.clone()
        val amount = getRandom().nextInt(9) + 1
        clone.amount = amount
        location.world.dropItemNaturally(location, clone)
        language.sendReceivedItemMessage(player, "Fireball", amount)
    }
}
