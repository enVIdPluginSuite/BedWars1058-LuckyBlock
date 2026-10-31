package dev.etran.luckyblock.listeners

import dev.etran.luckyblock.drops.ArmorDrop
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/** Lucky armor lasts only while its wearer lives; the overpowered cookie gives its effects when eaten. */
class LuckyItemListener : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onDeath(event: PlayerDeathEvent) {
        val drops = event.drops.iterator()
        while (drops.hasNext()) {
            if (NBTUtils.hasNBTTag(drops.next(), ArmorDrop.TAG)) drops.remove()
        }
        removeLuckyArmor(event.entity)
    }

    @EventHandler(ignoreCancelled = true)
    fun onDrop(event: PlayerDropItemEvent) {
        // it is not a loot item: it can't be thrown away and picked up by someone else
        if (NBTUtils.hasNBTTag(event.itemDrop.itemStack, ArmorDrop.TAG)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onEat(event: PlayerItemConsumeEvent) {
        if (!NBTUtils.hasNBTTag(event.item, "opCookie")) return
        val player = event.player
        effect(player, "INCREASE_DAMAGE", 200, 1)
        effect(player, "SPEED", 200, 2)
        effect(player, "REGENERATION", 120, 1)
        effect(player, "DAMAGE_RESISTANCE", 100, 0)
        effect(player, "ABSORPTION", 300, 1)
    }

    private fun effect(player: Player, name: String, ticks: Int, amplifier: Int) {
        var type = PotionEffectType.getByName(name)
        if (type == null && name == "INCREASE_DAMAGE") type = PotionEffectType.getByName("STRENGTH")
        if (type == null && name == "DAMAGE_RESISTANCE") type = PotionEffectType.getByName("RESISTANCE")
        if (type != null) player.addPotionEffect(PotionEffect(type, ticks, amplifier), true)
    }

    private fun removeLuckyArmor(player: Player) {
        val inventory = player.inventory
        if (NBTUtils.hasNBTTag(inventory.leggings, ArmorDrop.TAG)) inventory.leggings = null
        if (NBTUtils.hasNBTTag(inventory.boots, ArmorDrop.TAG)) inventory.boots = null
    }
}
