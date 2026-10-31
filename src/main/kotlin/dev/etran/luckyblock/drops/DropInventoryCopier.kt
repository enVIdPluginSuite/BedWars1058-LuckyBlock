package dev.etran.luckyblock.drops

import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DropInventoryCopier(private val language: Language) : Drop() {
    private val inventoryCopier: ItemStack

    init {
        val stack = ItemStack(Material.PAPER)
        val meta = stack.itemMeta
        meta.displayName = language.inventoryCopierName
        meta.lore = language.inventoryCopierLore
        stack.itemMeta = meta
        inventoryCopier = NBTUtils.applyNBTTags(stack, "inventoryCopier")
    }

    override fun drop(location: Location, player: Player) {
        location.world.dropItem(location, inventoryCopier)
        language.sendReceivedItemMessage(player, language.inventoryCopierName, 1)
    }
}
