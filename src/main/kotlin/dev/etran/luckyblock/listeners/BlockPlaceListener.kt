package dev.etran.luckyblock.listeners

import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.drops.DropRainbowWool
import dev.etran.luckyblock.luckyblock.LuckyBlock
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import dev.etran.luckyblock.tasks.CreateLuckyBlockTask
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

class BlockPlaceListener : Listener {
    /** How many of a lucky block each player held when they right-clicked with it: measured before the game takes the item. */
    private val heldBefore: MutableMap<UUID, Int> = HashMap()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onClickWithLuckyBlock(event: PlayerInteractEvent) {
        val item = event.item
        if (event.action != Action.RIGHT_CLICK_BLOCK || item == null) return
        val texture = LuckyBlockTexture.byBukkitItem(item)
        if (texture != null) heldBefore[event.player.uniqueId] = CreateLuckyBlockTask.countOf(event.player, texture)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        heldBefore.remove(event.player.uniqueId)
    }

    // A lucky block may only replace air or liquid. Replacing map blocks (grass, snow...) left glass behind.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPlaceCheck(event: BlockPlaceEvent) {
        if (!LuckyBlockTexture.isLuckyBlockItem(event.itemInHand)) return
        val replaced = event.blockReplacedState.type.name
        if (!(replaced == "AIR" || replaced.endsWith("_AIR") || replaced.contains("WATER") || replaced.contains("LAVA"))) {
            event.isCancelled = true
            event.player.sendMessage(ChatColor.RED.toString() + "You can't place a lucky block there.")
        }
    }

    // MONITOR + ignoreCancelled: BedWars1058 cancels placing onto map blocks (grass, snow...). Acting before it
    // left the glass in the world and the item in the inventory, so the lucky block must only be created once the
    // placement is final.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        // getItemInHand() on the event is the item that was actually used (main hand or off hand)
        val itemInHand = event.itemInHand
        val block = event.block
        val isHeadBlock = Items.isHeadBlock(block.type) || Items.isStainedGlassBlock(block.type)
        if (isHeadBlock && LuckyBlockTexture.isLuckyBlockItem(itemInHand)) {
            val before = heldBefore.remove(event.player.uniqueId)
            LuckyBlock.createLuckyBlock(block, itemInHand, event.blockReplacedState, event.player, before ?: -1)
        } else if (NBTUtils.hasNBTTag(itemInHand, "rainbowWool")) {
            DropRainbowWool.randomWool().place(block)
        }
    }
}
