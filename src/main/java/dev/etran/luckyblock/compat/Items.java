package dev.etran.luckyblock.compat;

import dev.etran.luckyblock.luckyblock.texture.GlassColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * Colored wool / carpet / glass and skulls on every version: WHITE_WOOL on 1.13+, WOOL with a data
 * value before that. Materials are looked up by name so no constant that is missing on some version
 * is ever referenced directly.
 */
public final class Items {
  private Items() {}

  /** A block/item type plus the legacy data value (always 0 on 1.13+). */
  public static final class Type {
    private final Material material;
    private final byte data;

    Type(Material material, int data) {
      this.material = material;
      this.data = (byte) data;
    }

    public Material getMaterial() {
      return material;
    }

    public ItemStack stack(int amount) {
      return (data == 0) ? new ItemStack(material, amount) : new ItemStack(material, amount, (short) data);
    }

    /** Turns the block into this type. */
    @SuppressWarnings("deprecation")
    public void place(Block block) {
      block.setType(material);
      if (!Compat.FLATTENED && data != 0)
        block.setData(data);
    }
  }

  private static Material named(String... names) {
    for (String name : names) {
      Material material = Material.getMaterial(name);
      if (material != null)
        return material;
    }
    throw new IllegalStateException("None of these materials exist on this server: " + String.join(", ", names));
  }

  private static Type colored(GlassColor color, String modernSuffix, String legacyName) {
    if (Compat.FLATTENED)
      return new Type(named(color.name() + "_" + modernSuffix), 0);
    return new Type(named(legacyName), color.getData());
  }

  public static Type wool(GlassColor color) {
    return colored(color, "WOOL", "WOOL");
  }

  public static Type carpet(GlassColor color) {
    return colored(color, "CARPET", "CARPET");
  }

  public static Type stainedGlass(GlassColor color) {
    return colored(color, "STAINED_GLASS", "STAINED_GLASS");
  }

  /** The fire charge item (FIRE_CHARGE, called FIREBALL before 1.13). */
  public static Material fireCharge() {
    return named("FIRE_CHARGE", "FIREBALL");
  }

  /** A fresh player head item. */
  public static ItemStack playerHead() {
    return Compat.FLATTENED ? new ItemStack(named("PLAYER_HEAD"), 1) : new ItemStack(named("SKULL_ITEM"), 1, (short) 3);
  }

  public static boolean isPlayerHeadItem(ItemStack stack) {
    if (stack == null)
      return false;
    if (Compat.FLATTENED)
      return stack.getType().name().equals("PLAYER_HEAD");
    return stack.getType().name().equals("SKULL_ITEM") && stack.getDurability() == 3;
  }

  /** True for a placed skull block (PLAYER_HEAD / PLAYER_WALL_HEAD, or SKULL before 1.13). */
  public static boolean isHeadBlock(Material material) {
    String name = material.name();
    return name.equals("PLAYER_HEAD") || name.equals("PLAYER_WALL_HEAD") || name.equals("SKULL");
  }
}
