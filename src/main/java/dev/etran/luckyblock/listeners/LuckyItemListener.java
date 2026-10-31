package dev.etran.luckyblock.listeners;

import dev.etran.luckyblock.drops.ArmorDrop;
import dev.etran.luckyblock.utils.NBTUtils;
import java.util.Iterator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Lucky armor lasts only while its wearer lives; the overpowered cookie gives its effects when eaten. */
public class LuckyItemListener implements Listener {

  @EventHandler(priority = EventPriority.HIGH)
  public void onDeath(PlayerDeathEvent event) {
    Iterator<ItemStack> drops = event.getDrops().iterator();
    while (drops.hasNext()) {
      if (NBTUtils.hasNBTTag(drops.next(), ArmorDrop.TAG))
        drops.remove();
    }
    removeLuckyArmor(event.getEntity());
  }

  @EventHandler(ignoreCancelled = true)
  public void onDrop(PlayerDropItemEvent event) {
    // it is not a loot item: it can't be thrown away and picked up by someone else
    if (NBTUtils.hasNBTTag(event.getItemDrop().getItemStack(), ArmorDrop.TAG))
      event.setCancelled(true);
  }

  @EventHandler(ignoreCancelled = true)
  public void onEat(PlayerItemConsumeEvent event) {
    if (!NBTUtils.hasNBTTag(event.getItem(), "opCookie"))
      return;
    Player player = event.getPlayer();
    effect(player, "INCREASE_DAMAGE", 200, 1);
    effect(player, "SPEED", 200, 2);
    effect(player, "REGENERATION", 120, 1);
    effect(player, "DAMAGE_RESISTANCE", 100, 0);
    effect(player, "ABSORPTION", 300, 1);
  }

  private static void effect(Player player, String name, int ticks, int amplifier) {
    PotionEffectType type = PotionEffectType.getByName(name);
    if (type == null && name.equals("INCREASE_DAMAGE"))
      type = PotionEffectType.getByName("STRENGTH");
    if (type == null && name.equals("DAMAGE_RESISTANCE"))
      type = PotionEffectType.getByName("RESISTANCE");
    if (type != null)
      player.addPotionEffect(new PotionEffect(type, ticks, amplifier), true);
  }

  private static void removeLuckyArmor(Player player) {
    PlayerInventory inventory = player.getInventory();
    if (NBTUtils.hasNBTTag(inventory.getLeggings(), ArmorDrop.TAG))
      inventory.setLeggings(null);
    if (NBTUtils.hasNBTTag(inventory.getBoots(), ArmorDrop.TAG))
      inventory.setBoots(null);
  }
}
