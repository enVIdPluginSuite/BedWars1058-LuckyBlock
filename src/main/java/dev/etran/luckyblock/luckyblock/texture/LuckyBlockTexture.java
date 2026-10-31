package dev.etran.luckyblock.luckyblock.texture;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.compat.Items;
import dev.etran.luckyblock.compat.Skulls;
import dev.etran.luckyblock.utils.ColorUtils;
import dev.etran.luckyblock.utils.NBTUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class LuckyBlockTexture {

  /** Every texture by name (the key in textures.yml, lower case), in file order. */
  private static final Map<String, LuckyBlockTexture> textures = new LinkedHashMap<>();
  private static final Random random = new Random();

  private final String name;
  private final ItemStack bukkitItem;
  private final Items.Type glass;

  private static final String TAG_KEY = "luckyblock_texture";

  public LuckyBlockTexture(String name, String value, GlassColor color, String itemName, List<String> itemLore) {

    this.name = name;

    ItemStack item = Items.playerHead();

    SkullMeta meta = (SkullMeta) item.getItemMeta();

    // Apply the custom texture. textures.yml stores the base64 "texture" value (a JSON blob containing the skin URL).
    if (!Skulls.applyTexture(meta, value)) {
      LuckyBlocks.getInstance().getLogger().warning(
          "Could not apply the skin of texture \"" + name + "\" in textures.yml, using a plain head instead.");
    }

    meta.setDisplayName(ColorUtils.colorize(itemName));

    List<String> coloredLore = new ArrayList<>();
    for (String lore : itemLore) {
      coloredLore.add(ColorUtils.colorize(lore));
    }
    meta.setLore(coloredLore);

    item.setItemMeta(meta);

    // Tag the item so we can recognise it reliably later (instead of comparing item meta).
    this.bukkitItem = NBTUtils.setValue(item, TAG_KEY, name);
    this.glass = Items.stainedGlass(color);
  }

  public String getName() {
    return name;
  }

  public ItemStack getBukkitItem() {
    return bukkitItem;
  }

  /** Turns the block into this lucky block's stained glass. */
  public void placeGlass(Block block) {
    glass.place(block);
  }

  public static void addTexture(String name, String value, GlassColor color, String itemName, List<String> itemLore) {
    String key = name.toLowerCase(Locale.ROOT);
    textures.put(key, new LuckyBlockTexture(key, value, color, itemName, itemLore));
  }

  public static LuckyBlockTexture byBukkitItem(ItemStack stack) {
    if (!Items.isPlayerHeadItem(stack)) return null;

    String textureName = NBTUtils.getValue(stack, TAG_KEY);

    return (textureName == null) ? null : textures.get(textureName);
  }

  public static boolean isLuckyBlockItem(ItemStack stack) {
    return byBukkitItem(stack) != null;
  }

  /** A random lucky block of any color. */
  public static LuckyBlockTexture randomTexture() {
    return randomTexture(null);
  }

  /**
   * A random lucky block out of the given texture names. Unknown names are ignored; if none of the
   * names exist (or the list is empty) any color can be picked, so a typo never stops a generator.
   */
  public static LuckyBlockTexture randomTexture(Collection<String> names) {
    List<LuckyBlockTexture> pool = new ArrayList<>();

    if (names != null) {
      for (String name : names) {
        LuckyBlockTexture texture = textures.get(name.toLowerCase(Locale.ROOT));
        if (texture != null && !pool.contains(texture)) pool.add(texture);
      }
    }

    if (pool.isEmpty()) pool.addAll(textures.values());

    return pool.get(random.nextInt(pool.size()));
  }

  /** Names from the list that don't exist in textures.yml. */
  public static List<String> unknownNames(Collection<String> names) {
    List<String> unknown = new ArrayList<>();
    for (String name : names) {
      if (!textures.containsKey(name.toLowerCase(Locale.ROOT))) unknown.add(name);
    }
    return unknown;
  }

  /** How many of the names exist in textures.yml. */
  public static int knownCount(Collection<String> names) {
    return names.size() - unknownNames(names).size();
  }

  public static Collection<String> allNames() {
    return textures.keySet();
  }
}
