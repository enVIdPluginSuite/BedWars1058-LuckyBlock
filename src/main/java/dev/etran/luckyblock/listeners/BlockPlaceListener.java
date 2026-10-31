package dev.etran.luckyblock.listeners;

import dev.etran.luckyblock.compat.Items;
import dev.etran.luckyblock.drops.DropRainbowWool;
import dev.etran.luckyblock.luckyblock.LuckyBlock;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;
import dev.etran.luckyblock.utils.NBTUtils;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockPlaceListener implements Listener {
  /** How many of a lucky block each player held when they right-clicked with it: measured before the game takes the item. */
  private final java.util.Map<java.util.UUID, Integer> heldBefore = new java.util.HashMap<>();

  @EventHandler(priority = EventPriority.LOWEST)
  public void onClickWithLuckyBlock(org.bukkit.event.player.PlayerInteractEvent event) {
    if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || event.getItem() == null)
      return;
    LuckyBlockTexture texture = LuckyBlockTexture.byBukkitItem(event.getItem());
    if (texture != null)
      heldBefore.put(event.getPlayer().getUniqueId(), Integer.valueOf(dev.etran.luckyblock.tasks.CreateLuckyBlockTask.countOf(event.getPlayer(), texture)));
  }

  @EventHandler
  public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
    heldBefore.remove(event.getPlayer().getUniqueId());
  }

  // A lucky block may only replace air or liquid. Replacing map blocks (grass, snow...) left glass behind.
  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onBlockPlaceCheck(BlockPlaceEvent event) {
    if (!LuckyBlockTexture.isLuckyBlockItem(event.getItemInHand()))
      return;
    String replaced = event.getBlockReplacedState().getType().name();
    if (!(replaced.equals("AIR") || replaced.endsWith("_AIR") || replaced.contains("WATER") || replaced.contains("LAVA"))) {
      event.setCancelled(true);
      event.getPlayer().sendMessage(org.bukkit.ChatColor.RED + "You can't place a lucky block there.");
    }
  }

  // MONITOR + ignoreCancelled: BedWars1058 cancels placing onto map blocks (grass, snow...). Acting before it
  // left the glass in the world and the item in the inventory, so the lucky block must only be created once the
  // placement is final.
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    // getItemInHand() on the event is the item that was actually used (main hand or off hand)
    ItemStack itemInHand = event.getItemInHand();
    Block block = event.getBlock();
    boolean isHeadBlock = Items.isHeadBlock(block.getType());
    if (isHeadBlock && LuckyBlockTexture.isLuckyBlockItem(itemInHand)) {
      Integer before = heldBefore.remove(event.getPlayer().getUniqueId());
      LuckyBlock.createLuckyBlock(block, itemInHand, event.getBlockReplacedState(), event.getPlayer(), before == null ? -1 : before.intValue());
    } else if (NBTUtils.hasNBTTag(itemInHand, "rainbowWool")) {
      DropRainbowWool.randomWool().place(block);
    }
  }
}
