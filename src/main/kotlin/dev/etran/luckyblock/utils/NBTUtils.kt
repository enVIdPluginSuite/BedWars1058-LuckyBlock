package dev.etran.luckyblock.utils

import dev.etran.luckyblock.LuckyBlocks
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.Plugin
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.Locale

/**
 * Custom item tags that work on every version from 1.8 up.
 *
 * On 1.14+ the tag is stored in the item's PersistentDataContainer (reached by reflection, because the
 * plugin is compiled against the 1.8 API). Servers before 1.14 have no such thing, so there the tag is
 * stored at the end of the item's display name as a run of color codes. Those render as nothing, and on
 * 1.8 - 1.12 names are plain strings so the codes survive. (From 1.13 on names are JSON text and trailing
 * color codes would be dropped, which is why the data container is used wherever it exists.)
 */
object NBTUtils {
    private const val SECTION = '§'
    private const val MARK = "§r§0§r§0"
    private const val HEX = "0123456789abcdef"

    // PersistentDataContainer through reflection; pdcOk is false before 1.14
    private var pdcOk = false
    private var metaGetContainer: Method? = null
    private var containerSet: Method? = null
    private var containerGet: Method? = null
    private var keyConstructor: Constructor<*>? = null
    private var stringType: Any? = null

    init {
        try {
            val keyClass = Class.forName("org.bukkit.NamespacedKey")
            val containerClass = Class.forName("org.bukkit.persistence.PersistentDataContainer")
            val typeClass = Class.forName("org.bukkit.persistence.PersistentDataType")
            // all looked up on public interfaces, never on the (non-public) CraftBukkit implementations
            metaGetContainer = ItemMeta::class.java.getMethod("getPersistentDataContainer")
            containerSet = containerClass.getMethod("set", keyClass, typeClass, Any::class.java)
            containerGet = containerClass.getMethod("get", keyClass, typeClass)
            keyConstructor = keyClass.getConstructor(Plugin::class.java, String::class.java)
            stringType = typeClass.getField("STRING").get(null)
            pdcOk = true
        } catch (e: ReflectiveOperationException) {
            pdcOk = false
        } catch (e: LinkageError) {
            pdcOk = false
        } catch (e: RuntimeException) {
            pdcOk = false
        }
    }

    /** True if tags are stored in the item data container (1.14+), false if they are stored in the name. */
    @JvmStatic
    fun usesDataContainer(): Boolean = pdcOk

    private fun key(key: String): Any =
        keyConstructor!!.newInstance(LuckyBlocks.getInstance(), key.lowercase(Locale.ROOT))

    /** Marks the item with a flag tag. */
    @JvmStatic
    fun applyNBTTags(item: ItemStack, tag: String): ItemStack = setValue(item, tag, "1")

    /** True if the item carries the flag tag. */
    @JvmStatic
    fun hasNBTTag(item: ItemStack?, tag: String): Boolean = "1" == getValue(item, tag)

    /** Returns a copy of the item with key=value stored in it (replacing any earlier tag). */
    @JvmStatic
    fun setValue(item: ItemStack, key: String, value: String): ItemStack {
        val copy = item.clone()
        val meta = copy.itemMeta ?: return copy
        if (pdcOk) {
            try {
                containerSet!!.invoke(metaGetContainer!!.invoke(meta), key(key), stringType, value)
                copy.itemMeta = meta
            } catch (e: ReflectiveOperationException) {
                warn(key, e)
            } catch (e: RuntimeException) {
                warn(key, e)
            }
            return copy
        }
        val name = stripTag(if (meta.hasDisplayName()) meta.displayName else defaultName(copy))
        meta.displayName = name + MARK + encode(key.lowercase(Locale.ROOT) + "=" + value)
        copy.itemMeta = meta
        return copy
    }

    private fun warn(key: String, e: Exception) {
        LuckyBlocks.getInstance().logger.warning("Could not tag an item ($key): ${e.cause ?: e}")
    }

    /** The value stored under the key, or null. */
    @JvmStatic
    fun getValue(item: ItemStack?, key: String): String? {
        if (item == null || !item.hasItemMeta()) return null
        val meta = item.itemMeta ?: return null
        if (pdcOk) {
            return try {
                containerGet!!.invoke(metaGetContainer!!.invoke(meta), key(key), stringType) as String?
            } catch (e: ReflectiveOperationException) {
                null
            } catch (e: RuntimeException) {
                null
            }
        }
        if (!meta.hasDisplayName()) return null
        val name = meta.displayName
        val at = name.lastIndexOf(MARK)
        if (at < 0) return null
        val payload = decode(name.substring(at + MARK.length)) ?: return null
        val equals = payload.indexOf('=')
        if (equals < 0 || payload.substring(0, equals) != key.lowercase(Locale.ROOT)) return null
        return payload.substring(equals + 1)
    }

    private fun defaultName(item: ItemStack): String = item.type.name.replace('_', ' ').lowercase(Locale.ROOT)

    private fun stripTag(name: String): String {
        val at = name.lastIndexOf(MARK)
        return if (at >= 0 && decode(name.substring(at + MARK.length)) != null) name.substring(0, at) else name
    }

    private fun encode(text: String): String {
        val out = StringBuilder()
        for (c in text) {
            val value = if (c.code < 256) c.code else '?'.code
            out.append(SECTION).append(HEX[value shr 4]).append(SECTION).append(HEX[value and 15])
        }
        return out.toString()
    }

    /** Returns null if the text is not a valid run of encoded characters. */
    private fun decode(codes: String): String? {
        if (codes.isEmpty() || codes.length % 4 != 0) return null
        val out = StringBuilder()
        var i = 0
        while (i < codes.length) {
            val high = HEX.indexOf(codes[i + 1])
            val low = HEX.indexOf(codes[i + 3])
            if (codes[i] != SECTION || codes[i + 2] != SECTION || high < 0 || low < 0) return null
            out.append((high * 16 + low).toChar())
            i += 4
        }
        return out.toString()
    }
}
