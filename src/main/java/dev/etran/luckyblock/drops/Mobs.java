package dev.etran.luckyblock.drops;

import com.andrei1058.bedwars.api.arena.IArena;
import dev.etran.luckyblock.LuckyBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.Plugin;

/** The mob and explosive drops: wolves and raining TNT. */
final class Mobs {
  private Mobs() {}

  /** A random living player of another team, or null. */
  static Player randomEnemy(Player player, IArena arena, Random random) {
    if (arena == null)
      return null;
    List<Player> enemies = new ArrayList<>();
    Object own = arena.getTeam(player);
    for (Player other : arena.getPlayers()) {
      if (!other.equals(player) && !other.isDead() && !arena.isSpectator(other) && arena.getTeam(other) != own)
        enemies.add(other);
    }
    return enemies.isEmpty() ? null : enemies.get(random.nextInt(enemies.size()));
  }

  /** Removes the mobs again after a while so they never pile up. */
  private static void expire(final List<Entity> mobs, long ticks) {
    Plugin plugin = LuckyBlocks.getInstance();
    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
      public void run() {
        for (Entity mob : mobs) {
          if (mob.isValid())
            mob.remove();
        }
      }
    }, ticks);
  }

  static String tntRain(Location at, Player player, IArena arena, Random random) {
    Player enemy = randomEnemy(player, arena, random);
    if (enemy == null)
      return "There is no enemy to rain TNT on!";
    for (int i = 0; i < 6; i++) {
      Location spot = enemy.getLocation().add(random.nextInt(9) - 4, 7.0D, random.nextInt(9) - 4);
      TNTPrimed tnt = enemy.getWorld().spawn(spot, TNTPrimed.class);
      tnt.setFuseTicks(50 + random.nextInt(20));
    }
    enemy.sendMessage(org.bukkit.ChatColor.RED + "TNT is raining down on you!");
    return "TNT rains on " + enemy.getName() + "!";
  }

  static String wolves(Location at, Player player, IArena arena, Random random) {
    List<Entity> list = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      Wolf wolf = (Wolf) player.getWorld().spawnEntity(player.getLocation().add(random.nextInt(3) - 1, 0.0D, random.nextInt(3) - 1), EntityType.WOLF);
      wolf.setTamed(true);
      wolf.setOwner(player);
      wolf.setAdult();
      list.add(wolf);
    }
    expire(list, 20L * 90L);
    return "Three wolves came to fight for you!";
  }
}
