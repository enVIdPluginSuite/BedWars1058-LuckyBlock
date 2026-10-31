package dev.etran.luckyblock.tasks;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.luckyblock.LuckyBlock;
import dev.etran.luckyblock.luckyblock.LuckyBlockHead;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;
import com.andrei1058.bedwars.api.arena.IArena;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CreateLuckyBlockTask extends BukkitRunnable {
  private final Block block;

  private final LuckyBlockTexture texture;

  private final LuckyBlockHead head;

  private final IArena arena;

  private final BlockState replaced;

  private final Player placer;

  private final ItemStack item;

  /** How many of this lucky block the placer held right before placing it (-1 = unknown). */
  private final int countBefore;

  public CreateLuckyBlockTask(Block block, LuckyBlockTexture texture, LuckyBlockHead head, IArena arena, BlockState replaced, Player placer, ItemStack item, int countBefore) {
    this.countBefore = countBefore;
    this.block = block;
    this.texture = texture;
    this.head = head;
    this.arena = arena;
    this.replaced = replaced;
    this.placer = placer;
    this.item = item.clone();
    this.item.setAmount(1);
  }

  public static void create(LuckyBlock luckyBlock, IArena arena, BlockState replaced, Player placer, ItemStack item, int countBefore) {
    (new CreateLuckyBlockTask(luckyBlock.getBlock(), luckyBlock.getTexture(), luckyBlock.getHead(), arena, replaced, placer, item, countBefore))
        .runTaskLater((Plugin)LuckyBlocks.getInstance(), 1L);
  }

  /** True if a lucky block may stand where this state was: air or liquid, never a map block like grass. */
  public static boolean isFreeSpace(BlockState state) {
    String name = state.getType().name();
    return name.equals("AIR") || name.endsWith("_AIR") || name.contains("WATER") || name.contains("LAVA");
  }

  /**
   * Only blocks that BedWars1058 accepted as a player-placed block become lucky blocks. If it refused (or
   * never registered it), or the spot was a map block, the head is undone, so no glass is left behind.
   */
  private boolean accepted() {
    if (this.arena == null || !isFreeSpace(this.replaced))
      return false;
    try {
      return this.arena.isBlockPlaced(this.block);
    } catch (RuntimeException | LinkageError e) {
      return false;
    }
  }

  /** How many of this lucky block the player carries now. */
  public static int countOf(Player player, LuckyBlockTexture texture) {
    int total = 0;
    for (ItemStack stack : player.getInventory().getContents()) {
      if (stack != null && LuckyBlockTexture.byBukkitItem(stack) == texture)
        total += stack.getAmount();
    }
    return total;
  }

  private boolean placerIsSurvival() {
    return this.placer != null && this.placer.isOnline() && this.placer.getGameMode() != org.bukkit.GameMode.CREATIVE;
  }

  public void run() {
    boolean known = this.countBefore >= 0 && placerIsSurvival();
    int now = known ? countOf(this.placer, this.texture) : 0;
    if (!dev.etran.luckyblock.compat.Items.isHeadBlock(this.block.getType())) {
      // the head was removed again within the tick (or the placement was undone): nothing to turn into a lucky block
      LuckyBlock.forget(this.block);
      return;
    }
    if (!accepted()) {
      LuckyBlock.forget(this.block);
      this.replaced.update(true, false);
      // the item is only given back if the game really took it
      if (placerIsSurvival() && (!known || now < this.countBefore))
        this.placer.getInventory().addItem(new ItemStack[] { this.item });
      return;
    }
    // a lucky block is now in the world, so exactly one must be gone from the inventory
    if (known && now >= this.countBefore)
      takeOne();
    this.texture.placeGlass(this.block);
    this.head.spawn();
  }

  private void takeOne() {
    ItemStack[] contents = this.placer.getInventory().getContents();
    for (int i = 0; i < contents.length; i++) {
      ItemStack stack = contents[i];
      if (stack != null && LuckyBlockTexture.byBukkitItem(stack) == this.texture) {
        if (stack.getAmount() > 1) {
          stack.setAmount(stack.getAmount() - 1);
          this.placer.getInventory().setItem(i, stack);
        } else {
          this.placer.getInventory().setItem(i, null);
        }
        return;
      }
    }
  }
}
