package dev.etran.luckyblock.listeners;

import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import dev.etran.luckyblock.arena.LuckyBlockArena;
import dev.etran.luckyblock.luckyblock.LuckyBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GameStateChangeListener implements Listener {
  @EventHandler
  public void onGameStateChange(GameStateChangeEvent event) {
    IArena arena = event.getArena();
    GameState state = event.getNewState();
    if (state == GameState.playing) {
      LuckyBlockArena.start(arena);
    } else {
      LuckyBlockArena.stop(arena.getArenaName());
      if (state == GameState.restarting)
        LuckyBlock.destroyAllIn(arena.getWorldName());
    }
  }
}
