package dev.etran.luckyblock.drops;

import dev.etran.luckyblock.data.Language;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** A drop that just hands out some items, built by a small function so the catalog stays short. */
public class SimpleDrop extends Drop {

  /** Makes the items of one drop. The first stack decides the amount shown in the chat message. */
  public interface Maker {
    ItemStack[] make(Player player, java.util.Random random);
  }

  private final String label;
  private final Maker maker;
  private final Language language;

  public SimpleDrop(Language language, String label, Maker maker) {
    this.language = language;
    this.label = label;
    this.maker = maker;
  }

  public void drop(Location location, Player player) {
    ItemStack[] items = this.maker.make(player, getRandom());
    if (items == null || items.length == 0)
      return;
    for (ItemStack item : items) {
      if (item != null)
        location.getWorld().dropItemNaturally(location, item);
    }
    this.language.sendReceivedItemMessage(player, this.label, items[0].getAmount());
  }
}
