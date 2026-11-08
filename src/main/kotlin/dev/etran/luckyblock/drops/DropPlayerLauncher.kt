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
        // Read and changed from the async chat thread as well as the main thread: it must be a concurrent set, and
        // "is it a launcher? then use it" must be one atomic step (claim), or two chat messages could both launch.
        private val launchers: MutableSet<java.util.UUID> = java.util.concurrent.ConcurrentHashMap.newKeySet()

        @JvmStatic
        fun addLauncher(launcher: Player) {
            launchers.add(launcher.uniqueId)
        }

        @JvmStatic
        fun removeLauncher(launcher: Player) {
            launchers.remove(launcher.uniqueId)
        }

        @JvmStatic
        fun isLauncher(player: Player): Boolean = launchers.contains(player.uniqueId)

        /** Takes the player's launcher if they have one; true only for the one caller that got it. */
        @JvmStatic
        fun claim(player: Player): Boolean = launchers.remove(player.uniqueId)
    }
}
