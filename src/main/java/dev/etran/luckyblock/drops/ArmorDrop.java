package dev.etran.luckyblock.drops;

import com.andrei1058.bedwars.api.arena.IArena;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.compat.Items;
import dev.etran.luckyblock.data.Language;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Leggings and boots that are put on the player who opened the lucky block (players can't put armor on
 * themselves). They are only worn while that player is alive: BedWars resets the armor on death.
 * Armor the player already wears is only replaced by something better.
 */
public class ArmorDrop extends Drop {
  /** Tag on lucky armor, so it can be taken off again when its wearer dies (see LuckyItemListener). */
  public static final String TAG = "luckyArmor";

  private final Language language;
  private final String label;
  private final String material;   // "CHAINMAIL", "IRON", "DIAMOND"
  private final int protection;

  public ArmorDrop(Language language, String label, String material, int protection) {
    this.language = language;
    this.label = label;
    this.material = material;
    this.protection = protection;
  }

  public void drop(Location location, Player player) {
    if (player == null || player.isDead() || !player.isOnline())
      return;
    IArena arena = null;
    try {
      arena = LuckyBlocks.getBedWars().getArenaUtil().getArenaByPlayer(player);
    } catch (RuntimeException e) {
      // no BedWars: still equip
    }
    if (arena != null && arena.isSpectator(player))
      return;
    PlayerInventory inventory = player.getInventory();
    boolean legs = equip(inventory.getLeggings(), make(this.material + "_LEGGINGS"), true, inventory);
    boolean boots = equip(inventory.getBoots(), make(this.material + "_BOOTS"), false, inventory);
    if (legs || boots) {
      this.language.sendReceivedItemMessage(player, this.label + " (worn)", 1);
    } else {
      player.sendMessage(ChatColor.YELLOW + "You already wear better leggings and boots than the " + this.label + ".");
    }
  }

  private boolean equip(ItemStack current, ItemStack wanted, boolean leggings, PlayerInventory inventory) {
    if (rank(current) >= rank(wanted))
      return false;
    if (leggings) {
      inventory.setLeggings(wanted);
    } else {
      inventory.setBoots(wanted);
    }
    return true;
  }

  private ItemStack make(String name) {
    ItemStack item = new ItemStack(Items.material(name), 1);
    if (this.protection > 0) {
      Enchantment protection = Enchantment.getByName("PROTECTION_ENVIRONMENTAL");
      if (protection == null)
        protection = Enchantment.getByName("PROTECTION");
      if (protection != null)
        item.addUnsafeEnchantment(protection, this.protection);
    }
    return dev.etran.luckyblock.utils.NBTUtils.applyNBTTags(item, TAG);
  }

  /** Material tier * 10 + protection level; empty slots rank -1. */
  private static int rank(ItemStack item) {
    if (item == null || item.getType() == Material.AIR)
      return -1;
    String name = item.getType().name();
    int tier = name.startsWith("DIAMOND") ? 5 : name.startsWith("NETHERITE") ? 6 : name.startsWith("IRON") ? 3
        : name.startsWith("CHAINMAIL") ? 2 : name.startsWith("GOLD") ? 2 : 1;
    int prot = 0;
    for (Enchantment enchantment : item.getEnchantments().keySet()) {
      String n = enchantment.getName();
      if (n.equals("PROTECTION_ENVIRONMENTAL") || n.equals("PROTECTION"))
        prot = item.getEnchantmentLevel(enchantment);
    }
    return tier * 10 + prot;
  }
}
