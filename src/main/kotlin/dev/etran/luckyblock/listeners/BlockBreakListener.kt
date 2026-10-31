package dev.etran.luckyblock.listeners

import dev.etran.luckyblock.luckyblock.LuckyBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent

class BlockBreakListener : Listener {
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        if (LuckyBlock.isLuckyBlock(block)) LuckyBlock.destroyLuckyBlock(block, event.player)
    }
}
