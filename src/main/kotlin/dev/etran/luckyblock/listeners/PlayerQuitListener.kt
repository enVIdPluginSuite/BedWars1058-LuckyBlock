package dev.etran.luckyblock.listeners

import dev.etran.luckyblock.drops.DropPlayerLauncher
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class PlayerQuitListener : Listener {
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        DropPlayerLauncher.removeLauncher(event.player)
    }
}
