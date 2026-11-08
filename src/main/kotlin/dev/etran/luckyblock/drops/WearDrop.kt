package dev.etran.luckyblock.drops

import dev.etran.luckyblock.abilities.Kit
import dev.etran.luckyblock.data.Language
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.Random

/**
 * A hat, chestplate, pants or boots that are put straight on the player who opened the lucky block (players can't
 * put armor on themselves from the ground). They are tagged, so they are taken off again when the wearer dies.
 */
class WearDrop(
    private val language: Language,
    private val label: String,
    private val slot: String, // HELMET, CHESTPLATE, LEGGINGS or BOOTS
    private val maker: (Random) -> ItemStack
) : Drop() {
    override fun drop(location: Location, player: Player) {
        if (player.isDead || !player.isOnline) return
        Kit.equip(player, slot, maker(getRandom()))
        language.sendReceivedItemMessage(player, "$label (worn)", 1)
    }
}
