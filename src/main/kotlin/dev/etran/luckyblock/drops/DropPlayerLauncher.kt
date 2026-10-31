package dev.etran.luckyblock.drops

import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DropPlayerLauncher(private val language: Language) : Drop() {
    private val playerLauncher: ItemStack

    init {
        val stack = ItemStack(Material.PAPER)
        val meta = stack.itemMeta
        meta.displayName = language.playerLauncherName
        meta.lore = language.playerLauncherLore
        stack.itemMeta = meta
        playerLauncher = NBTUtils.applyNBTTags(stack, "playerLauncher")
    }

    override fun drop(location: Location, player: Player) {
        location.world.dropItem(location, playerLauncher)
        language.sendReceivedItemMessage(player, language.playerLauncherName, 1)
    }

    companion object {
        private val launchers: MutableSet<String> = HashSet()

        @JvmStatic
        fun addLauncher(launcher: Player) {
            launchers.add(launcher.name)
        }

        @JvmStatic
        fun removeLauncher(launcher: Player) {
            launchers.remove(launcher.name)
        }

        @JvmStatic
        fun isLauncher(player: Player): Boolean = launchers.contains(player.name)
    }
}
