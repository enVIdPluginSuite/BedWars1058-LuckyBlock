package dev.etran.luckyblock.listeners

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.drops.DropPlayerLauncher
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class AsyncPlayerChatListener(@Suppress("UNUSED_PARAMETER") language: Language?) : Listener {

    @EventHandler
    fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val message = event.message
        LuckyBlocks.getBedWars().arenaUtil.getArenaByPlayer(player) ?: return
        if (DropPlayerLauncher.isLauncher(player)) {
            val target = Bukkit.getPlayerExact(message)
            if (target == null) {
                player.sendMessage(language().typeNameToLaunchLang)
            } else {
                event.isCancelled = true
                DropPlayerLauncher.removeLauncher(player)
                // This event runs off the main thread on modern servers, but changing a player's
                // velocity must happen on the main thread, so hop over to it.
                Bukkit.getScheduler().runTask(LuckyBlocks.getInstance(), Runnable {
                    if (!target.isOnline) return@Runnable
                    target.velocity = target.location.direction.multiply(2.0).setY(1.3)
                    language().sendPlayerLauncherPlayerMessage(player, target)
                    language().sendPlayerLauncherTargetMessage(target, player)
                })
            }
        }
    }

    private fun language(): Language = LuckyBlocks.getInstance().language
}
