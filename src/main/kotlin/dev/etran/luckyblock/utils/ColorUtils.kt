package dev.etran.luckyblock.utils

import org.bukkit.ChatColor

object ColorUtils {
    @JvmStatic
    fun colorize(text: String): String = ChatColor.translateAlternateColorCodes('&', text)

    @JvmStatic
    fun colorizeList(list: List<String>): List<String> = list.map { colorize(it) }
}
