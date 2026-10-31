package dev.etran.luckyblock.data

import dev.etran.luckyblock.utils.ColorUtils
import org.bukkit.entity.Player

class Language(
    receivedItemLang: String,
    usedInventoryCopierPlayerLang: String,
    usedInventoryCopierTargetLang: String,
    usedLightningScrollPlayerLang: String,
    usedLightningScrollTargetLang: String,
    typeNameToLaunchLang: String,
    usedPlayerLauncherPlayerLang: String,
    usedPlayerLauncherTargetLang: String,
    rainbowWoolName: String,
    rainbowWoolLore: List<String>,
    inventoryCopierName: String,
    inventoryCopierLore: List<String>,
    playerLauncherName: String,
    playerLauncherLore: List<String>,
    lightningScrollName: String,
    lightningScrollLore: List<String>
) {
    val receivedItemLang: String = ColorUtils.colorize(receivedItemLang)
    val usedInventoryCopierPlayerLang: String = ColorUtils.colorize(usedInventoryCopierPlayerLang)
    val usedInventoryCopierTargetLang: String = ColorUtils.colorize(usedInventoryCopierTargetLang)
    val usedLightningScrollPlayerLang: String = ColorUtils.colorize(usedLightningScrollPlayerLang)
    val usedLightningScrollTargetLang: String = ColorUtils.colorize(usedLightningScrollTargetLang)
    val typeNameToLaunchLang: String = ColorUtils.colorize(typeNameToLaunchLang)
    val usedPlayerLauncherPlayerLang: String = ColorUtils.colorize(usedPlayerLauncherPlayerLang)
    val usedPlayerLauncherTargetLang: String = ColorUtils.colorize(usedPlayerLauncherTargetLang)
    val rainbowWoolName: String = ColorUtils.colorize(rainbowWoolName)
    val rainbowWoolLore: List<String> = ColorUtils.colorizeList(rainbowWoolLore)
    val inventoryCopierName: String = ColorUtils.colorize(inventoryCopierName)
    val inventoryCopierLore: List<String> = ColorUtils.colorizeList(inventoryCopierLore)
    val playerLauncherName: String = ColorUtils.colorize(playerLauncherName)
    val playerLauncherLore: List<String> = ColorUtils.colorizeList(playerLauncherLore)
    val lightningScrollName: String = ColorUtils.colorize(lightningScrollName)
    val lightningScrollLore: List<String> = ColorUtils.colorizeList(lightningScrollLore)

    fun sendReceivedItemMessage(player: Player, name: String, count: Int) {
        player.sendMessage(receivedItemLang.replace("{item}", name).replace("{count}", count.toString()))
    }

    fun sendInventoryCopierPlayerMessage(player: Player, target: Player) {
        player.sendMessage(usedInventoryCopierPlayerLang.replace("{target}", target.name))
    }

    fun sendInventoryCopierTargetMessage(target: Player, copier: Player) {
        target.sendMessage(usedInventoryCopierTargetLang.replace("{copier}", copier.name))
    }

    fun sendLightningScrollPlayerMessage(player: Player, target: Player) {
        player.sendMessage(usedLightningScrollPlayerLang.replace("{target}", target.name))
    }

    fun sendLightningScrollTargetMessage(target: Player, striker: Player) {
        target.sendMessage(usedLightningScrollTargetLang.replace("{striker}", striker.name))
    }

    fun sendPlayerLauncherPlayerMessage(player: Player, target: Player) {
        player.sendMessage(usedPlayerLauncherPlayerLang.replace("{target}", target.name))
    }

    fun sendPlayerLauncherTargetMessage(player: Player, launcher: Player) {
        player.sendMessage(usedPlayerLauncherTargetLang.replace("{launcher}", launcher.name))
    }
}
