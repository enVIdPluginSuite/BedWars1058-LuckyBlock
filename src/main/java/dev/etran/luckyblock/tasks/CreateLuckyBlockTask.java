package dev.etran.luckyblock.tasks;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.luckyblock.LuckyBlock;
import dev.etran.luckyblock.luckyblock.LuckyBlockHead;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;
import lombok.Generated;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CreateLuckyBlockTask extends BukkitRunnable {
  private final Block block;

  private final LuckyBlockTexture texture;

  private final LuckyBlockHead head;

  @Generated
  public CreateLuckyBlockTask(Block block, LuckyBlockTexture texture, LuckyBlockHead head) {
    this.block = block;
    this.texture = texture;
    this.head = head;
  }

  public static void create(LuckyBlock luckyBlock) {
    (new CreateLuckyBlockTask(luckyBlock.getBlock(), luckyBlock.getTexture(), luckyBlock.getHead())).runTaskLater((Plugin)LuckyBlocks.getInstance(), 1L);
  }

  public void run() {
    if (this.block.getType() == Material.AIR)
      return;
    this.texture.placeGlass(this.block);
    this.head.spawn();
  }
}
