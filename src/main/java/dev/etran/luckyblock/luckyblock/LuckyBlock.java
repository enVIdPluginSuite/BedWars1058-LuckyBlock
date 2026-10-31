package dev.etran.luckyblock.luckyblock;

import com.andrei1058.bedwars.api.arena.IArena;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.drops.Drop;
import dev.etran.luckyblock.generator.GeneratorKind;
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
  
  public static void createLuckyBlock(Block block, ItemStack itemInHand) {
    IArena arena = LuckyBlocks.getBedWars().getArenaUtil().getArenaByIdentifier(block.getWorld().getName());
    LuckyBlock luckyBlock = new LuckyBlock(block, LuckyBlockTexture.byBukkitItem(itemInHand), arena);
    blocks.put(block, luckyBlock);
    CreateLuckyBlockTask.create(luckyBlock);
  }

  public static void destroyLuckyBlock(Block block, Player player) {
    LuckyBlock luckyBlock = blocks.remove(block);
    if (luckyBlock == null)
      return;
    luckyBlock.getHead().destroy();
    // diamond and emerald lucky blocks roll on better loot tables (see "loot:" in config.yml)
    GeneratorKind tier = LuckyBlocks.getInstance().getCachedConfig().tierOf(luckyBlock.getTexture().getName());
    Drop drop = Drop.randomDrop(tier);
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
