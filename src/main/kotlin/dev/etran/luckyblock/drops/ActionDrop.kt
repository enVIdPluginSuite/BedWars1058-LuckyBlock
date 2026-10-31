package dev.etran.luckyblock.drops

import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.luckyblock.LuckyBlocks
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.Random

/** A drop that does something in the world (spawns mobs, TNT...) instead of giving an item. */
class ActionDrop(private val action: Action) : Drop() {

    fun interface Action {
        /** Returns what happened for the player's chat message, or null if there is nothing to say. */
        fun run(at: Location, player: Player, arena: IArena?, random: Random): String?
    }

    override fun drop(location: Location, player: Player) {
        if (player.isDead) return
        var arena: IArena? = null
        try {
            arena = LuckyBlocks.getBedWars().arenaUtil.getArenaByPlayer(player)
        } catch (e: RuntimeException) {
            // handled by the action
        }
        val message = action.run(location, player, arena, getRandom())
        if (message != null) player.sendMessage(ChatColor.GOLD.toString() + message)
    }
}
