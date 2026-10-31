package dev.etran.luckyblock.drops;

import dev.etran.luckyblock.data.Language;
import dev.etran.luckyblock.utils.NBTUtils;
import lombok.Generated;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DropLightningScroll extends Drop {
  private final ItemStack inventoryCopier;
  
  private final Language language;
  
  @Generated
  public DropLightningScroll(ItemStack inventoryCopier, Language language) {
    this.inventoryCopier = inventoryCopier;
    this.language = language;
  }
  
  public DropLightningScroll(Language language) {
    ItemStack stack = new ItemStack(Material.PAPER);
    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(language.getLightningScrollName());
    meta.setLore(language.getLightningScrollLore());
    stack.setItemMeta(meta);
    this.inventoryCopier = NBTUtils.applyNBTTags(stack, "lightningScroll");
    this.language = language;
  }
  
  public void drop(Location location, Player player) {
    location.getWorld().dropItem(location, this.inventoryCopier);
    this.language.sendReceivedItemMessage(player, this.language.getLightningScrollName(), 1);
  }
}
