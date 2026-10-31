package dev.etran.luckyblock.luckyblock.texture

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.compat.Skulls
import dev.etran.luckyblock.drops.Rarity
import dev.etran.luckyblock.utils.ColorUtils
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import java.util.Locale
import java.util.Random

class LuckyBlockTexture(
    val name: String,
    value: String,
    color: GlassColor,
    @Suppress("UNUSED_PARAMETER") itemName: String?,
    itemLore: List<String>
) {
    val rarity: Rarity
    val bukkitItem: ItemStack
    private val glass: Items.Type

    init {
        val item = Items.playerHead()
        val meta = item.itemMeta as SkullMeta

        // Apply the custom texture. textures.yml stores the base64 "texture" value (a JSON blob containing the skin URL).
        if (!Skulls.applyTexture(meta, value)) {
            LuckyBlocks.getInstance().logger.warning(
                "Could not apply the skin of texture \"$name\" in textures.yml, using a plain head instead."
            )
        }

        // the rarity comes from the color (config.yml: rarities.<rarity>.textures) and is part of the item's name
        val config = LuckyBlocks.getInstance().cachedConfig
        rarity = config.rarityOfTexture(name)
        val rarityName = config.getRarity(rarity).display
        meta.displayName = ColorUtils.colorize(config.luckyBlockName.replace("{rarity}", rarityName))

        val coloredLore = ArrayList<String>()
        coloredLore.add(ColorUtils.colorize("&7Rarity: $rarityName"))
        for (lore in itemLore) coloredLore.add(ColorUtils.colorize(lore))
        meta.lore = coloredLore

        item.itemMeta = meta

        // Tag the item so we can recognise it reliably later (instead of comparing item meta).
        bukkitItem = NBTUtils.setValue(item, TAG_KEY, name)
        glass = Items.stainedGlass(color)
    }

    /** Turns the block into this lucky block's stained glass. */
    fun placeGlass(block: Block) {
        glass.place(block)
    }

    companion object {
        /** Every texture by name (the key in textures.yml, lower case), in file order. */
        private val textures: MutableMap<String, LuckyBlockTexture> = LinkedHashMap()
        private val random = Random()
        private const val TAG_KEY = "luckyblock_texture"

        @JvmStatic
        fun addTexture(name: String, value: String, color: GlassColor, itemName: String?, itemLore: List<String>) {
            val key = name.lowercase(Locale.ROOT)
            textures[key] = LuckyBlockTexture(key, value, color, itemName, itemLore)
        }

        @JvmStatic
        fun byBukkitItem(stack: ItemStack?): LuckyBlockTexture? {
            if (stack == null || !Items.isPlayerHeadItem(stack)) return null
            val textureName = NBTUtils.getValue(stack, TAG_KEY) ?: return null
            return textures[textureName]
        }

        @JvmStatic
        fun isLuckyBlockItem(stack: ItemStack?): Boolean = byBukkitItem(stack) != null

        /** A random lucky block of the given rarity. If no color has that rarity, any color is used. */
        @JvmStatic
        fun randomTexture(rarity: Rarity): LuckyBlockTexture {
            val pool = textures.values.filter { it.rarity == rarity }.ifEmpty { textures.values.toList() }
            return pool[random.nextInt(pool.size)]
        }

        /**
         * A random lucky block out of the given texture names. Unknown names are ignored; if none of the
         * names exist (or the list is empty) any color can be picked, so a typo never stops a generator.
         */
        @JvmStatic
        fun randomTexture(names: Collection<String>?): LuckyBlockTexture {
            val pool = ArrayList<LuckyBlockTexture>()
            if (names != null) {
                for (name in names) {
                    val texture = textures[name.lowercase(Locale.ROOT)]
                    if (texture != null && !pool.contains(texture)) pool.add(texture)
                }
            }
            if (pool.isEmpty()) pool.addAll(textures.values)
            return pool[random.nextInt(pool.size)]
        }

        /** Names from the list that don't exist in textures.yml. */
        @JvmStatic
        fun unknownNames(names: Collection<String>): List<String> =
            names.filter { !textures.containsKey(it.lowercase(Locale.ROOT)) }

        /** How many of the names exist in textures.yml. */
        @JvmStatic
        fun knownCount(names: Collection<String>): Int = names.size - unknownNames(names).size

        /** Forgets every texture (used when textures.yml is reloaded). */
        @JvmStatic
        fun clear() {
            textures.clear()
        }

        @JvmStatic
        fun allNames(): Collection<String> = textures.keys
    }
}
