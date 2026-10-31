package dev.etran.luckyblock.utils;

import dev.etran.luckyblock.LuckyBlocks;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * Custom item tags that work on every version from 1.8 up.
 *
 * On 1.14+ the tag is stored in the item's PersistentDataContainer (reached by reflection, because the
 * plugin is compiled against the 1.8 API). Servers before 1.14 have no such thing, so there the tag is
 * stored at the end of the item's display name as a run of color codes. Those render as nothing, and on
 * 1.8 - 1.12 names are plain strings so the codes survive. (From 1.13 on names are JSON text and trailing
 * color codes would be dropped, which is why the data container is used wherever it exists.)
 */
public class NBTUtils {
  private static final char SECTION = '§';
  private static final String MARK = SECTION + "r" + SECTION + "0" + SECTION + "r" + SECTION + "0";
  private static final String HEX = "0123456789abcdef";

  // PersistentDataContainer through reflection; PDC_OK is false before 1.14
  private static final boolean PDC_OK;
  private static Method metaGetContainer;
  private static Method containerSet;
  private static Method containerGet;
  private static Constructor<?> keyConstructor;
  private static Object stringType;

  static {
    boolean ok;
    try {
      Class<?> keyClass = Class.forName("org.bukkit.NamespacedKey");
      Class<?> containerClass = Class.forName("org.bukkit.persistence.PersistentDataContainer");
      Class<?> typeClass = Class.forName("org.bukkit.persistence.PersistentDataType");
      // all looked up on public interfaces, never on the (non-public) CraftBukkit implementations
      metaGetContainer = ItemMeta.class.getMethod("getPersistentDataContainer");
      containerSet = containerClass.getMethod("set", keyClass, typeClass, Object.class);
      containerGet = containerClass.getMethod("get", keyClass, typeClass);
      keyConstructor = keyClass.getConstructor(Plugin.class, String.class);
      stringType = typeClass.getField("STRING").get(null);
      ok = true;
    } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
      ok = false;
    }
    PDC_OK = ok;
  }

  /** True if tags are stored in the item data container (1.14+), false if they are stored in the name. */
  public static boolean usesDataContainer() {
    return PDC_OK;
  }

  private static Object key(String key) throws ReflectiveOperationException {
    return keyConstructor.newInstance(LuckyBlocks.getInstance(), key.toLowerCase(Locale.ROOT));
  }

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
    if (PDC_OK) {
      try {
        containerSet.invoke(metaGetContainer.invoke(meta), key(key), stringType, value);
        copy.setItemMeta(meta);
      } catch (ReflectiveOperationException | RuntimeException e) {
        LuckyBlocks.getInstance().getLogger().warning("Could not tag an item (" + key + "): " + ((e.getCause() != null) ? e.getCause() : e));
      }
      return copy;
    }
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
    if (meta == null)
      return null;
    if (PDC_OK) {
      try {
        return (String) containerGet.invoke(metaGetContainer.invoke(meta), key(key), stringType);
      } catch (ReflectiveOperationException | RuntimeException e) {
        return null;
      }
    }
    if (!meta.hasDisplayName())
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
