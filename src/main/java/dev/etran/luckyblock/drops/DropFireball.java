package dev.etran.luckyblock.drops;

import dev.etran.luckyblock.compat.Items;
import dev.etran.luckyblock.data.Language;
import lombok.Generated;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DropFireball extends Drop {
  private final ItemStack drop;
  
  private final Language language;
  
  @Generated
  public DropFireball(Language language) {
    this.drop = new ItemStack(Items.fireCharge());
    this.language = language;
  }
  
  public void drop(Location location, Player player) {
    ItemStack clone = this.drop.clone();
    int amount = getRandom().nextInt(9) + 1;
    clone.setAmount(amount);
    location.getWorld().dropItemNaturally(location, clone);
    this.language.sendReceivedItemMessage(player, "Fireball", amount);
  }
}
