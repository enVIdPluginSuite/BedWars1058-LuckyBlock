package dev.etran.luckyblock.listeners;

import dev.etran.luckyblock.drops.DropPlayerLauncher;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    DropPlayerLauncher.removeLauncher(event.getPlayer());
  }
}
