package dev.etran.luckyblock.utils;

import java.util.Locale;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Custom item tags that work on every version from 1.8 up.
 *
 * 1.8 has no PersistentDataContainer and raw NBT needs version specific NMS code, so the tag is
 * stored at the end of the item's display name as a run of color codes. Color codes without text
 * render as nothing, so players don't see it, and it survives stacking, dropping and inventory
 * moves like any other item data. (Renaming the item in an anvil removes it.)
 */
public class NBTUtils {
  private static final char SECTION = '§';
  private static final String MARK = SECTION + "r" + SECTION + "0" + SECTION + "r" + SECTION + "0";
  private static final String HEX = "0123456789abcdef";

  /** Marks the item with a flag tag. */
  public static ItemStack applyNBTTags(ItemStack item, String tag) {
    return setValue(item, tag, "1");
  }

  /** True if the item carries the flag tag. */
  public static boolean hasNBTTag(ItemStack item, String tag) {
    return "1".equals(getValue(item, tag));
  }

  /** Returns a copy of the item with key=value stored in it (replacing any earlier tag). */
  public static ItemStack setValue(ItemStack item, String key, String value) {
    ItemStack copy = item.clone();
    ItemMeta meta = copy.getItemMeta();
    if (meta == null)
      return copy;
    String name = stripTag(meta.hasDisplayName() ? meta.getDisplayName() : defaultName(copy));
    meta.setDisplayName(name + MARK + encode(key.toLowerCase(Locale.ROOT) + "=" + value));
    copy.setItemMeta(meta);
    return copy;
  }

  /** The value stored under the key, or null. */
  public static String getValue(ItemStack item, String key) {
    if (item == null || !item.hasItemMeta())
      return null;
    ItemMeta meta = item.getItemMeta();
    if (meta == null || !meta.hasDisplayName())
      return null;
    String name = meta.getDisplayName();
    int at = name.lastIndexOf(MARK);
    if (at < 0)
      return null;
    String payload = decode(name.substring(at + MARK.length()));
    if (payload == null)
      return null;
    int equals = payload.indexOf('=');
    if (equals < 0 || !payload.substring(0, equals).equals(key.toLowerCase(Locale.ROOT)))
      return null;
    return payload.substring(equals + 1);
  }

  private static String defaultName(ItemStack item) {
    return item.getType().name().replace('_', ' ').toLowerCase(Locale.ROOT);
  }

  private static String stripTag(String name) {
    int at = name.lastIndexOf(MARK);
    return (at >= 0 && decode(name.substring(at + MARK.length())) != null) ? name.substring(0, at) : name;
  }

  private static String encode(String text) {
    StringBuilder out = new StringBuilder();
    for (char c : text.toCharArray()) {
      int value = (c < 256) ? c : '?';
      out.append(SECTION).append(HEX.charAt(value >> 4)).append(SECTION).append(HEX.charAt(value & 15));
    }
    return out.toString();
  }

  /** Returns null if the text is not a valid run of encoded characters. */
  private static String decode(String codes) {
    if (codes.isEmpty() || codes.length() % 4 != 0)
      return null;
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < codes.length(); i += 4) {
      int high = HEX.indexOf(codes.charAt(i + 1));
      int low = HEX.indexOf(codes.charAt(i + 3));
      if (codes.charAt(i) != SECTION || codes.charAt(i + 2) != SECTION || high < 0 || low < 0)
        return null;
      out.append((char) (high * 16 + low));
    }
    return out.toString();
  }
}
