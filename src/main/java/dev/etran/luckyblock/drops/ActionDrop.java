package dev.etran.luckyblock.drops;

import com.andrei1058.bedwars.api.arena.IArena;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** A drop that does something in the world (spawns mobs, TNT...) instead of giving an item. */
public class ActionDrop extends Drop {

  public interface Action {
    /** Returns what happened for the player's chat message, or null if it could not be done (then they are told why in the message). */
    String run(Location at, Player player, IArena arena, Random random);
  }

  private final Action action;

  public ActionDrop(Action action) {
    this.action = action;
  }

  public void drop(Location location, Player player) {
    if (player == null || player.isDead())
      return;
    IArena arena = null;
    try {
      arena = dev.etran.luckyblock.LuckyBlocks.getBedWars().getArenaUtil().getArenaByPlayer(player);
    } catch (RuntimeException e) {
      // handled by the action
    }
    String message = this.action.run(location, player, arena, getRandom());
    if (message != null)
      player.sendMessage(ChatColor.GOLD + message);
  }
}
