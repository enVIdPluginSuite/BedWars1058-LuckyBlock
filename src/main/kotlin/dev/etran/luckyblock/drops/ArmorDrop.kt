package dev.etran.luckyblock.drops

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Leggings and boots that are put on the player who opened the lucky block (players can't put armor on
 * themselves). They are only worn while that player is alive: they are removed when the wearer dies.
 * Armor the player already wears is only replaced by something better.
 */
class ArmorDrop(
    private val language: Language,
    private val label: String,
    private val material: String, // "CHAINMAIL", "IRON", "DIAMOND"
    private val protection: Int
) : Drop() {

    override fun drop(location: Location, player: Player) {
        if (player.isDead || !player.isOnline) return
        try {
            val arena = LuckyBlocks.getBedWars().arenaUtil.getArenaByPlayer(player)
            if (arena != null && arena.isSpectator(player)) return
        } catch (e: RuntimeException) {
            // no BedWars: still equip
        }
        val inventory = player.inventory
        val wantedLegs = make(material + "_LEGGINGS")
        val wantedBoots = make(material + "_BOOTS")
        var legs = false
        var boots = false
        if (rank(inventory.leggings) < rank(wantedLegs)) {
            inventory.leggings = wantedLegs
            legs = true
        }
        if (rank(inventory.boots) < rank(wantedBoots)) {
            inventory.boots = wantedBoots
            boots = true
        }
        if (legs || boots) {
            language.sendReceivedItemMessage(player, "$label (worn)", 1)
        } else {
            player.sendMessage(ChatColor.YELLOW.toString() + "You already wear better leggings and boots than the " + label + ".")
        }
    }

    private fun make(name: String): ItemStack {
        val item = ItemStack(Items.material(name), 1)
        if (protection > 0) {
            val enchantment = Enchantment.getByName("PROTECTION_ENVIRONMENTAL") ?: Enchantment.getByName("PROTECTION")
            if (enchantment != null) item.addUnsafeEnchantment(enchantment, protection)
        }
        return NBTUtils.applyNBTTags(item, TAG)
    }

    companion object {
        /** Tag on lucky armor, so it can be taken off again when its wearer dies (see LuckyItemListener). */
        const val TAG = "luckyArmor"

        /** Material tier * 10 + protection level; empty slots rank -1. */
        private fun rank(item: ItemStack?): Int {
            if (item == null || item.type == Material.AIR) return -1
            val name = item.type.name
            val tier = when {
                name.startsWith("DIAMOND") -> 5
                name.startsWith("NETHERITE") -> 6
                name.startsWith("IRON") -> 3
                name.startsWith("CHAINMAIL") || name.startsWith("GOLD") -> 2
                else -> 1
            }
            var prot = 0
            for (enchantment in item.enchantments.keys) {
                val n = enchantment.name
                if (n == "PROTECTION_ENVIRONMENTAL" || n == "PROTECTION") prot = item.getEnchantmentLevel(enchantment)
            }
            return tier * 10 + prot
        }
    }
}
