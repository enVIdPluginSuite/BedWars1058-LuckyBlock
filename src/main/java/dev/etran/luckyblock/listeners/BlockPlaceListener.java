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
  @EventHandler(priority = EventPriority.LOWEST)
  public void onBlockPlace(BlockPlaceEvent event) {
    // getItemInHand() on the event is the item that was actually used (main hand or off hand)
    ItemStack itemInHand = event.getItemInHand();
    Block block = event.getBlock();
    boolean isHeadBlock = Items.isHeadBlock(block.getType());
    if (isHeadBlock && LuckyBlockTexture.isLuckyBlockItem(itemInHand)) {
      LuckyBlock.createLuckyBlock(block, itemInHand);
    } else if (NBTUtils.hasNBTTag(itemInHand, "rainbowWool")) {
      DropRainbowWool.randomWool().place(block);
    }
  }
}
