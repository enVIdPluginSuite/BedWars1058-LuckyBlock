package dev.etran.luckyblock.listeners

import com.andrei1058.bedwars.api.arena.GameState
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent
import dev.etran.luckyblock.arena.LuckyBlockArena
import dev.etran.luckyblock.luckyblock.LuckyBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class GameStateChangeListener : Listener {
    @EventHandler
    fun onGameStateChange(event: GameStateChangeEvent) {
        val arena = event.arena
        val state = event.newState
        if (state == GameState.playing) {
            LuckyBlockArena.start(arena)
        } else {
            LuckyBlockArena.stop(arena.arenaName)
            if (state == GameState.restarting) LuckyBlock.destroyAllIn(arena.worldName)
        }
    }
}
