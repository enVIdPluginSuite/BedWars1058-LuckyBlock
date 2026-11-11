package dev.etran.luckyblock.drops

import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.luckyblock.texture.GlassColor
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DropSleepingDust(private val language: Language) : Drop() {
    private val drop: ItemStack = stack()

    companion object {
        /** One Sleeping Dust item. */
        @JvmStatic
        fun stack(): ItemStack {
            val stack = Items.carpet(GlassColor.PURPLE).stack(1)
            val meta = stack.itemMeta
            meta.displayName = ChatColor.LIGHT_PURPLE.toString() + "Sleeping Dust"
            stack.itemMeta = meta
            return NBTUtils.applyNBTTags(stack, "sleepDust")
        }
    }

    override fun drop(location: Location, player: Player) {
        val clone = drop.clone()
        val amount = getRandom().nextInt(2) + 1
        clone.amount = amount
        location.world.dropItem(location, clone)
        language.sendReceivedItemMessage(player, ChatColor.LIGHT_PURPLE.toString() + "Sleeping Dust", amount)
    }
}
