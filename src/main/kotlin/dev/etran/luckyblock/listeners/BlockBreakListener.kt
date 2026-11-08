package dev.etran.luckyblock.listeners

import dev.etran.luckyblock.luckyblock.LuckyBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.entity.EntityExplodeEvent

class BlockBreakListener : Listener {
    // MONITOR + ignoreCancelled: only a break that really happens opens the lucky block. If BedWars1058 (or another
    // plugin) cancels it, the block stays and must not give its loot as well.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        if (LuckyBlock.isLuckyBlock(block)) LuckyBlock.destroyLuckyBlock(block, event.player)
    }

    // An explosion breaks blocks without a break event: a lucky block caught in one would vanish and leave its
    // floating head behind. Lucky blocks are blast proof; they have to be broken by hand.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) {
        event.blockList().removeAll { LuckyBlock.isLuckyBlock(it) }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) {
        event.blockList().removeAll { LuckyBlock.isLuckyBlock(it) }
    }
}
