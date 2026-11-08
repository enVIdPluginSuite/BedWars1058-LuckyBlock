package dev.etran.luckyblock.listeners

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.abilities.Kit
import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.drops.DropPlayerLauncher
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class AsyncPlayerChatListener(@Suppress("UNUSED_PARAMETER") language: Language?) : Listener {

    /**
     * The name typed after using a Player Launcher. This event runs off the main thread, so here the launcher is only
     * claimed (atomically) and the message hidden; looking up the target and launching happen on the main thread.
     */
    @EventHandler
    fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        if (!DropPlayerLauncher.claim(player)) return
        event.isCancelled = true
        val name = event.message.trim()
        Bukkit.getScheduler().runTask(LuckyBlocks.getInstance(), Runnable {
            if (!player.isOnline) return@Runnable
            val arena = Kit.arenaOf(player)
            if (arena == null || !Kit.gameRunning(arena)) return@Runnable // the game ended meanwhile: the launcher is gone
            val target = Bukkit.getPlayerExact(name)
            // only someone playing in the same game (not a player in the lobby or in another game)
            if (target == null || Kit.arenaOf(target) !== arena || !Kit.isPlaying(target)) {
                DropPlayerLauncher.addLauncher(player)
                player.sendMessage(language().typeNameToLaunchLang)
                return@Runnable
            }
            target.velocity = target.location.direction.multiply(2.0).setY(1.3)
            language().sendPlayerLauncherPlayerMessage(player, target)
            language().sendPlayerLauncherTargetMessage(target, player)
        })
    }

    private fun language(): Language = LuckyBlocks.getInstance().language
}
