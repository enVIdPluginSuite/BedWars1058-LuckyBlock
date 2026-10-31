package dev.etran.luckyblock.listeners;

import dev.etran.luckyblock.arena.LuckyBlockArena;
import dev.etran.luckyblock.generator.LuckyBlockGenerator;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemMergeEvent;

/**
 * Lucky blocks dropped at the same spot merge into one stack. When that happens the generator has
 * to follow the surviving item, otherwise it would think the blocks were collected.
 */
public class ItemMergeListener implements Listener {
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemMerge(ItemMergeEvent event) {
    UUID merged = event.getEntity().getUniqueId();
    UUID survivor = event.getTarget().getUniqueId();
    for (LuckyBlockArena arena : LuckyBlockArena.all()) {
      for (LuckyBlockGenerator generator : arena.getGenerators()) {
        if (generator.isTracking(merged))
          generator.track(survivor);
      }
    }
  }
}
