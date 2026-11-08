package dev.etran.luckyblock.abilities

import dev.etran.luckyblock.drops.Rarity
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.entity.Wolf
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.UUID

/** Someone dresses up as the enemy for a while. */
internal object Disguises {
    fun start(p: Player, seconds: Int): Boolean {
        val arena = Kit.arenaOf(p) ?: return false
        val own = arena.getTeam(p)
        val other = arena.teams.filter { it !== own && it.members.isNotEmpty() }.randomOrNull() ?: return false
        val color = other.color.dye().color
        val until = Live.now() + seconds * 1000L
        val before = Live.disguised[p.uniqueId]
        if (before == null) {
            // what is worn now comes back afterwards: real armor and lucky pieces alike
            Live.disguised[p.uniqueId] = Live.Disguise(Kit.SLOTS.map { Kit.armorIn(p, it) }.toTypedArray(), until)
        } else {
            // a second disguise: keep the armor from before the first one, only the time and colors change
            Live.disguised[p.uniqueId] = Live.Disguise(before.armor, until)
        }
        for (slot in Kit.SLOTS) {
            Kit.setArmor(p, slot, Kit.wearable(Kit.leather(slot, color), "disguise", other.color.chat().toString() + "Disguise"))
        }
        p.sendMessage(ChatColor.GOLD.toString() + "You are disguised as team ${other.name} for $seconds seconds. Hitting someone breaks the disguise!")
        return true
    }

    fun end(p: Player) {
        val d = Live.disguised.remove(p.uniqueId) ?: return
        Kit.SLOTS.forEachIndexed { i, slot ->
            // a lucky piece put on while disguised stays on; disguise pieces give way to what was worn before
            if (Kit.wornType(Kit.armorIn(p, slot)) == "disguise") Kit.setArmor(p, slot, d.armor[i])
        }
        if (!p.isDead) p.sendMessage(ChatColor.GOLD.toString() + "Your disguise is gone.")
    }
}

/** Villagers you can trade with by right clicking: the Resource Trader and Jerry. */
internal object Traders {
    fun spawn(owner: Player, kind: String, seconds: Int): Boolean {
        // stand next to the player, not inside them (they would push each other around)
        val ahead = owner.location.direction.setY(0.0)
        if (ahead.lengthSquared() < 1.0E-4) ahead.x = 1.0
        var at = owner.location.clone().add(ahead.normalize().multiply(1.5))
        if (at.block.type.isSolid || at.block.getRelative(0, 1, 0).type.isSolid) at = owner.location.clone()
        val villager = owner.world.spawnEntity(at, EntityType.VILLAGER) as Villager
        villager.customName = if (kind == "jerry") ChatColor.GREEN.toString() + "Jerry" else ChatColor.GOLD.toString() + "Resource Trader"
        villager.isCustomNameVisible = true
        Kit.freeze(villager)
        Live.traders[villager.uniqueId] = Live.Trader(villager, kind, Live.now() + seconds * 1000L, owner.uniqueId)
        owner.sendMessage(
            ChatColor.GOLD.toString() +
                if (kind == "jerry") "Jerry sells Miracle lucky blocks for 4 emeralds each. Right click him! He leaves in $seconds seconds."
                else "The Resource Trader gives 1 diamond for 4 gold or 24 iron. Right click him! He leaves in $seconds seconds."
        )
        return true
    }

    fun trade(player: Player, trader: Live.Trader) {
        val gold = Kit.mat("GOLD_INGOT")
        val iron = Kit.mat("IRON_INGOT")
        val emerald = Kit.mat("EMERALD")
        if (trader.kind == "jerry") {
            if (Kit.count(player, emerald) < 4) {
                player.sendMessage(ChatColor.RED.toString() + "Jerry wants 4 emeralds for a Miracle lucky block.")
                return
            }
            val block = try {
                LuckyBlockTexture.randomTexture(Rarity.MIRACLE).bukkitItem.clone()
            } catch (e: RuntimeException) {
                null // no lucky block colors loaded (broken textures.yml)
            }
            if (block == null) {
                player.sendMessage(ChatColor.RED.toString() + "Jerry has no lucky blocks to sell right now.")
                return
            }
            Kit.take(player, emerald, 4)
            Kit.give(player, block)
            player.sendMessage(ChatColor.GREEN.toString() + "Jerry gave you a Miracle lucky block!")
            return
        }
        if (Kit.count(player, gold) >= 4) {
            Kit.take(player, gold, 4)
        } else if (Kit.count(player, iron) >= 24) {
            Kit.take(player, iron, 24)
        } else {
            player.sendMessage(ChatColor.RED.toString() + "You need 4 gold or 24 iron for a diamond.")
            return
        }
        Kit.give(player, ItemStack(Kit.mat("DIAMOND"), 1))
        player.sendMessage(ChatColor.AQUA.toString() + "Traded for a diamond!")
    }
}

/** The Companion menu: three helpers to pick from, each with armor of a different quality. */
internal object Companions {
    private const val TITLE = "Choose your Companion"

    private class Option(val label: String, val type: String, val tier: String)

    private val choices: MutableMap<UUID, List<Option>> = HashMap()

    fun open(p: Player) {
        val tiers = listOf("CHAINMAIL", "IRON", "DIAMOND").shuffled()
        val options = listOf(
            Option("Skeleton Companion", "SKELETON", tiers[0]),
            Option("Zombie Companion", "ZOMBIE", tiers[1]),
            Option("Wolf Companion", "WOLF", tiers[2])
        )
        val inventory: Inventory = Bukkit.createInventory(null, 9, TITLE)
        val icons = listOf("BONE", "ROTTEN_FLESH", "BONE")
        options.forEachIndexed { i, o ->
            val icon = Kit.named(
                ItemStack(Kit.mat(icons[i])), ChatColor.GREEN.toString() + o.label,
                "Armor: ${o.tier.lowercase().replaceFirstChar { it.uppercase() }}", "Click to pick"
            )
            inventory.setItem(2 + i * 2, icon)
        }
        choices[p.uniqueId] = options
        Live.companionMenu.add(p.uniqueId)
        p.openInventory(inventory)
    }

    fun isMenu(title: String?): Boolean = title == TITLE

    /** A click in the menu: slots 2, 4 and 6 are the three companions. */
    fun pick(p: Player, slot: Int) {
        val index = when (slot) {
            2 -> 0
            4 -> 1
            6 -> 2
            else -> return
        }
        val options = choices.remove(p.uniqueId) ?: return
        Live.companionMenu.remove(p.uniqueId)
        // closing an inventory inside its own click event is not allowed by Bukkit: do it a tick later
        Kit.runLater(1L, Runnable { if (p.isOnline) p.closeInventory() })
        summon(p, options[index])
    }

    /** The menu was closed without a pick: the lucky block was not for nothing, a random companion comes. */
    fun closed(p: Player) {
        val options = choices.remove(p.uniqueId) ?: return
        Live.companionMenu.remove(p.uniqueId)
        if (!Kit.isPlaying(p)) return
        val o = options.random()
        p.sendMessage(ChatColor.GOLD.toString() + "You didn't choose, so a ${o.label} comes.")
        summon(p, o)
    }

    private fun summon(p: Player, o: Option) {
        if (!Kit.isPlaying(p)) return
        val mob = Allies.spawn(Kit.entityType(o.type), p.location, p, 120, ChatColor.GREEN.toString() + o.label) ?: return
        val eq = mob.equipment
        if (mob !is Wolf) {
            eq.helmet = ItemStack(Kit.mat(o.tier + "_HELMET"))
            eq.chestplate = ItemStack(Kit.mat(o.tier + "_CHESTPLATE"))
            eq.leggings = ItemStack(Kit.mat(o.tier + "_LEGGINGS"))
            eq.boots = ItemStack(Kit.mat(o.tier + "_BOOTS"))
            eq.helmetDropChance = 0f; eq.chestplateDropChance = 0f; eq.leggingsDropChance = 0f; eq.bootsDropChance = 0f
        } else {
            mob.isTamed = true
            mob.owner = p
            Kit.effect(mob, 120, if (o.tier == "DIAMOND") 2 else 1, "DAMAGE_RESISTANCE", "RESISTANCE")
        }
        if (mob is org.bukkit.entity.Zombie) mob.isBaby = false
        p.sendMessage(ChatColor.GOLD.toString() + "Your ${o.label} fights for you for 2 minutes!")
    }

    fun forget(id: UUID) {
        choices.remove(id)
        Live.companionMenu.remove(id)
    }
}
