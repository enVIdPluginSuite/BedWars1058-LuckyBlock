package dev.etran.luckyblock.luckyblock.texture

import org.bukkit.DyeColor

enum class GlassColor(
    /** Legacy (pre-1.13) data value. */
    val data: Int
) {
    WHITE(0),
    ORANGE(1),
    MAGENTA(2),
    LIGHT_BLUE(3),
    YELLOW(4),
    LIME(5),
    PINK(6),
    GRAY(7),
    LIGHT_GRAY(8),
    CYAN(9),
    PURPLE(10),
    BLUE(11),
    BROWN(12),
    GREEN(13),
    RED(14),
    BLACK(15);

    companion object {
        /** The matching color for a dye color (the old name SILVER is light gray). Unknown colors become white. */
        @JvmStatic
        fun fromDye(dye: DyeColor): GlassColor {
            val name = if (dye.name == "SILVER") "LIGHT_GRAY" else dye.name
            return values().firstOrNull { it.name == name } ?: WHITE
        }
    }
}
