package dev.etran.luckyblock.compat

import dev.etran.luckyblock.luckyblock.texture.GlassColor
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.inventory.ItemStack

/**
 * Colored wool / carpet / glass and skulls on every version: WHITE_WOOL on 1.13+, WOOL with a data
 * value before that. Materials are looked up by name so no constant that is missing on some version
 * is ever referenced directly.
 */
object Items {

    /** A block/item type plus the legacy data value (always 0 on 1.13+). */
    class Type internal constructor(val material: Material, data: Int) {
        private val data: Byte = data.toByte()

        fun stack(amount: Int): ItemStack =
            if (data.toInt() == 0) ItemStack(material, amount) else ItemStack(material, amount, data.toShort())

        /** Turns the block into this type. */
        @Suppress("DEPRECATION")
        fun place(block: Block) {
            block.type = material
            if (!Compat.FLATTENED && data.toInt() != 0) block.data = data
        }
    }

    /** The first of these materials that exists on this server (names differ between versions). */
    @JvmStatic
    fun material(vararg names: String): Material = named(*names)

    @JvmStatic
    fun terracotta(color: GlassColor): Type = colored(color, "TERRACOTTA", "STAINED_CLAY")

    private fun named(vararg names: String): Material {
        for (name in names) {
            val material = Material.getMaterial(name)
            if (material != null) return material
        }
        throw IllegalStateException("None of these materials exist on this server: " + names.joinToString(", "))
    }

    private fun colored(color: GlassColor, modernSuffix: String, legacyName: String): Type =
        if (Compat.FLATTENED) Type(named(color.name + "_" + modernSuffix), 0)
        else Type(named(legacyName), color.data)

    @JvmStatic
    fun wool(color: GlassColor): Type = colored(color, "WOOL", "WOOL")

    @JvmStatic
    fun carpet(color: GlassColor): Type = colored(color, "CARPET", "CARPET")

    @JvmStatic
    fun stainedGlass(color: GlassColor): Type = colored(color, "STAINED_GLASS", "STAINED_GLASS")

    /** The fire charge item (FIRE_CHARGE, called FIREBALL before 1.13). */
    @JvmStatic
    fun fireCharge(): Material = named("FIRE_CHARGE", "FIREBALL")

    /** A fresh player head item. */
    @JvmStatic
    fun playerHead(): ItemStack =
        if (Compat.FLATTENED) ItemStack(named("PLAYER_HEAD"), 1) else ItemStack(named("SKULL_ITEM"), 1, 3.toShort())

    @JvmStatic
    fun isPlayerHeadItem(stack: ItemStack?): Boolean {
        if (stack == null) return false
        if (Compat.FLATTENED) return stack.type.name == "PLAYER_HEAD"
        return stack.type.name == "SKULL_ITEM" && stack.durability.toInt() == 3
    }

    /** True for a placed skull block (PLAYER_HEAD / PLAYER_WALL_HEAD, or SKULL before 1.13). */
    @JvmStatic
    fun isHeadBlock(material: Material): Boolean {
        val name = material.name
        return name == "PLAYER_HEAD" || name == "PLAYER_WALL_HEAD" || name == "SKULL"
    }
}
