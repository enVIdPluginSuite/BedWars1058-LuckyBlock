package dev.etran.luckyblock.drops

import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.luckyblock.texture.GlassColor
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.Random

class DropRainbowWool(private val language: Language) : Drop() {
    private val rainbowWool: ItemStack

    init {
        val stack = Items.wool(GlassColor.WHITE).stack(64)
        val meta = stack.itemMeta
        meta.displayName = language.rainbowWoolName
        meta.lore = language.rainbowWoolLore
        stack.itemMeta = meta
        rainbowWool = NBTUtils.applyNBTTags(stack, "rainbowWool")
    }

    override fun drop(location: Location, player: Player) {
        location.world.dropItemNaturally(location, rainbowWool)
        language.sendReceivedItemMessage(player, language.rainbowWoolName, 64)
    }

    companion object {
        private val colors = listOf(
            GlassColor.LIME, GlassColor.CYAN, GlassColor.GREEN, GlassColor.GRAY, GlassColor.PINK,
            GlassColor.RED, GlassColor.YELLOW, GlassColor.BLUE, GlassColor.LIGHT_GRAY
        )
        private val random = Random()

        @JvmStatic
        fun randomWool(): Items.Type = Items.wool(colors[random.nextInt(colors.size)])
    }
}
