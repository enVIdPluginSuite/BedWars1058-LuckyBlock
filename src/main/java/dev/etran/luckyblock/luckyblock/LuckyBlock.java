package dev.etran.luckyblock.luckyblock;

import com.andrei1058.bedwars.api.arena.IArena;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.drops.Drop;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;
import dev.etran.luckyblock.tasks.CreateLuckyBlockTask;
import java.util.HashMap;
import java.util.Map;
import lombok.Generated;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LuckyBlock {
  @Generated
  public Block getBlock() {
    return this.block;
  }
  
  @Generated
  public LuckyBlockHead getHead() {
    return this.head;
  }
  
  @Generated
  public LuckyBlockTexture getTexture() {
    return this.texture;
  }
  
  private static final Map<Block, LuckyBlock> blocks = new HashMap<>();
  
  private final Block block;
  
  private final LuckyBlockHead head;
  
  private final LuckyBlockTexture texture;
  
  public LuckyBlock(Block block, LuckyBlockTexture texture, IArena arena) {
    this.block = block;
    this.texture = texture;
    this.head = new LuckyBlockHead(block.getLocation().add(0.5D, -1.25D, 0.5D), texture);
  }
  
  public static void createLuckyBlock(Block block, ItemStack itemInHand, org.bukkit.block.BlockState replaced, Player placer, int before) {
    IArena arena = LuckyBlocks.getBedWars().getArenaUtil().getArenaByIdentifier(block.getWorld().getName());
    LuckyBlockTexture texture = LuckyBlockTexture.byBukkitItem(itemInHand);
    // lucky blocks only exist inside games, and only on free space (never on map blocks like grass)
    if (arena == null || texture == null || !CreateLuckyBlockTask.isFreeSpace(replaced))
      return;
    LuckyBlock luckyBlock = new LuckyBlock(block, texture, arena);
    blocks.put(block, luckyBlock);
    CreateLuckyBlockTask.create(luckyBlock, arena, replaced, placer, itemInHand, before);
  }

  /** Drops a lucky block that never got created (its head block disappeared). */
  public static void forget(Block block) {
    blocks.remove(block);
  }

  public static void destroyLuckyBlock(Block block, Player player) {
    LuckyBlock luckyBlock = blocks.remove(block);
    if (luckyBlock == null)
      return;
    luckyBlock.getHead().destroy();
    // every rarity has its own loot table (see "loot:" in config.yml)
    Drop drop = Drop.randomDrop(luckyBlock.getTexture().getRarity());
    if (drop != null)
      drop.drop(block.getLocation(), player);
  }

  /** Forgets the lucky blocks placed in one world (used when a game ends and the map is reset). */
  public static void destroyAllIn(String worldName) {
    java.util.Iterator<Map.Entry<Block, LuckyBlock>> iterator = blocks.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Block, LuckyBlock> entry = iterator.next();
      if (entry.getKey().getWorld().getName().equals(worldName)) {
        entry.getValue().getHead().destroy();
        iterator.remove();
      }
    }
  }

  /** Removes every lucky block head (used when the plugin is disabled). */
  public static void destroyAll() {
    for (LuckyBlock luckyBlock : blocks.values())
      luckyBlock.getHead().destroy();
    blocks.clear();
  }
  
  public static boolean isLuckyBlock(Block block) {
    return blocks.containsKey(block);
  }
}
