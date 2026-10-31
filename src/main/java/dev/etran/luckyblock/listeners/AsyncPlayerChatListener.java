package dev.etran.luckyblock.listeners;

import com.andrei1058.bedwars.api.arena.IArena;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.data.Language;
import dev.etran.luckyblock.drops.DropPlayerLauncher;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class AsyncPlayerChatListener implements Listener {
  private final Language language;

  @Generated
  public AsyncPlayerChatListener(Language language) {
    this.language = language;
  }

  @EventHandler
  public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
    final Player player = event.getPlayer();
    String message = event.getMessage();
    IArena arena = LuckyBlocks.getBedWars().getArenaUtil().getArenaByPlayer(player);
    if (arena == null)
      return;
    if (DropPlayerLauncher.isLauncher(player)) {
      final Player target = Bukkit.getPlayerExact(message);
      if (target == null) {
        player.sendMessage(language().getTypeNameToLaunchLang());
      } else {
        event.setCancelled(true);
        DropPlayerLauncher.removeLauncher(player);
        // This event runs off the main thread on modern servers, but changing a player's
        // velocity must happen on the main thread, so hop over to it.
        Bukkit.getScheduler().runTask(LuckyBlocks.getInstance(), () -> {
          if (!target.isOnline())
            return;
          target.setVelocity(target.getLocation().getDirection().multiply(2.0D).setY(1.3D));
          language().sendPlayerLauncherPlayerMessage(player, target);
          language().sendPlayerLauncherTargetMessage(target, player);
        });
      }
    }
  }

  private static Language language() {
    return dev.etran.luckyblock.LuckyBlocks.getInstance().getLanguage();
  }
}
