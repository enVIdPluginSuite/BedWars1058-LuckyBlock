package dev.etran.luckyblock.drops

import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DropLightningScroll(private val language: Language) : Drop() {
    private val lightningScroll: ItemStack

    init {
        val stack = ItemStack(Material.PAPER)
        val meta = stack.itemMeta
        meta.displayName = language.lightningScrollName
        meta.lore = language.lightningScrollLore
        stack.itemMeta = meta
        lightningScroll = NBTUtils.applyNBTTags(stack, "lightningScroll")
    }

    override fun drop(location: Location, player: Player) {
        location.world.dropItem(location, lightningScroll)
        language.sendReceivedItemMessage(player, language.lightningScrollName, 1)
    }
}
