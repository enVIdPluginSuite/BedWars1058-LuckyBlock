package dev.etran.luckyblock.listeners

import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.compat.Compat
import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.drops.DropPlayerLauncher
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.Random

class PlayerInteractListener(@Suppress("UNUSED_PARAMETER") language: Language?) : Listener {
    private val random = Random()

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val action = event.action
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return
        // Since 1.9 this event fires once per hand; only handle the main hand so items aren't used twice.
        if (!Compat.isMainHand(event)) return
        val stack = event.item ?: return
        val player = event.player
        val arena = LuckyBlocks.getBedWars().arenaUtil.getArenaByPlayer(player) ?: return

        if (NBTUtils.hasNBTTag(stack, "inventoryCopier")) {
            val players = ArrayList(arena.players)
            players.remove(player)
            if (players.isEmpty()) {
                player.sendMessage(ChatColor.RED.toString() + "Cannot use this item if there is no other player!")
                return
            }
            val target = players[random.nextInt(players.size)]
            val own = player.inventory
            val theirs = target.inventory
            own.clear()
            val items = theirs.contents
            for (i in items.indices) own.setItem(i, items[i])
            language().sendInventoryCopierPlayerMessage(player, target)
            language().sendInventoryCopierTargetMessage(target, player)
            consumeItem(player, stack)
        } else if (NBTUtils.hasNBTTag(stack, "lightningScroll")) {
            val players = ArrayList(arena.players)
            players.remove(player)
            if (players.isEmpty()) {
                player.sendMessage(ChatColor.RED.toString() + "Cannot use this item if there is no other player!")
                return
            }
            val target = players[random.nextInt(players.size)]
            val location = target.location
            location.world?.strikeLightning(location)
            language().sendLightningScrollPlayerMessage(player, target)
            language().sendLightningScrollTargetMessage(target, player)
            consumeItem(player, stack)
        } else if (NBTUtils.hasNBTTag(stack, "playerLauncher")) {
            DropPlayerLauncher.addLauncher(player)
            consumeItem(player, stack)
            player.sendMessage(language().typeNameToLaunchLang)
        } else if (NBTUtils.hasNBTTag(stack, "iceBridge")) {
            startIceBridge(player)
            consumeItem(player, stack)
        } else if (NBTUtils.hasNBTTag(stack, "swapScroll")) {
            swap(player, arena, stack)
        } else if (NBTUtils.hasNBTTag(stack, "jumpFeather")) {
            val leap = player.location.direction.setY(0.0)
            if (leap.lengthSquared() > 0.0) leap.normalize().multiply(0.5)
            leap.setY(1.0)
            player.velocity = leap
            player.fallDistance = 0.0f
            consumeItem(player, stack)
        } else if (NBTUtils.hasNBTTag(stack, "ghastSummon")) {
            summon(player, arena, stack, EntityType.GHAST, 9.0, 30L)
        } else if (NBTUtils.hasNBTTag(stack, "creeperSummon")) {
            summon(player, arena, stack, EntityType.CREEPER, 0.0, 0L)
        } else if (NBTUtils.hasNBTTag(stack, "healingTotem")) {
            heal(player, arena, stack)
        }
    }

    /** Spawns a mob at the enemy closest to the player. Ghasts go away again after a while. */
    private fun summon(player: Player, arena: IArena, stack: ItemStack, type: EntityType, up: Double, seconds: Long) {
        val own = arena.getTeam(player)
        var nearest: Player? = null
        var best = Double.MAX_VALUE
        for (other in arena.players) {
            if (other == player || arena.getTeam(other) === own || other.world != player.world) continue
            val distance = other.location.distanceSquared(player.location)
            if (distance < best) {
                best = distance
                nearest = other
            }
        }
        if (nearest == null) {
            player.sendMessage(ChatColor.RED.toString() + "There is no enemy to summon this on!")
            return
        }
        val at = nearest.location.add(0.0, up, 0.0)
        val mob = at.world!!.spawnEntity(at, type)
        if (seconds > 0L) {
            Bukkit.getScheduler().runTaskLater(LuckyBlocks.getInstance(), Runnable { mob.remove() }, seconds * 20L)
        }
        player.sendMessage(ChatColor.RED.toString() + "A " + type.name.lowercase() + " appears over " + nearest.name + "!")
        consumeItem(player, stack)
    }

    /** Swaps places with a random player of another team. */
    private fun swap(player: Player, arena: IArena, stack: ItemStack) {
        val own = arena.getTeam(player)
        val enemies = arena.players.filter { it != player && arena.getTeam(it) !== own }
        if (enemies.isEmpty()) {
            player.sendMessage(ChatColor.RED.toString() + "There is no enemy to swap with!")
            return
        }
        val target = enemies[random.nextInt(enemies.size)]
        val mine = player.location
        val theirs = target.location
        player.teleport(theirs)
        target.teleport(mine)
        player.sendMessage(ChatColor.LIGHT_PURPLE.toString() + "You swapped places with " + target.name + "!")
        target.sendMessage(ChatColor.LIGHT_PURPLE.toString() + player.name + " swapped places with you!")
        consumeItem(player, stack)
    }

    /** Heals the player and the teammates within 6 blocks, with a short regeneration. */
    private fun heal(player: Player, arena: IArena, stack: ItemStack) {
        val own = arena.getTeam(player)
        val regeneration = PotionEffectType.getByName("REGENERATION")
        for (other in arena.players) {
            val mate = other == player || (own != null && arena.getTeam(other) === own)
            if (!mate || other.world != player.world || other.location.distanceSquared(player.location) > 36.0) continue
            other.health = other.maxHealth
            if (regeneration != null) other.addPotionEffect(PotionEffect(regeneration, 100, 1), true)
            other.sendMessage(ChatColor.GREEN.toString() + "You were healed by a Healing Totem!")
        }
        consumeItem(player, stack)
    }

    private fun startIceBridge(player: Player) {
        val blocks = ArrayList<Location>()
        val start = player.location.clone()
        start.y = start.y - 1.0
        val dir = start.direction.normalize()
        for (i in 1..10) {
            val place = start.clone().add(dir.clone().multiply(i))
            val block = place.block
            if (block.type == Material.AIR) {
                block.type = Material.ICE
                blocks.add(block.location)
            }
        }
        object : BukkitRunnable() {
            var ticks = 0

            override fun run() {
                ticks += 20
                if (ticks >= 1200) {
                    val r = Random()
                    for (loc in blocks) {
                        if (r.nextBoolean() && loc.block.type == Material.ICE) loc.block.type = Material.AIR
                    }
                    cancel()
                }
            }
        }.runTaskTimer(LuckyBlocks.getInstance(), 20L, 20L)
    }

    private fun consumeItem(player: Player, stack: ItemStack) {
        if (stack.amount > 1) {
            stack.amount = stack.amount - 1
        } else {
            player.inventory.removeItem(stack)
        }
    }

    private fun language(): Language = LuckyBlocks.getInstance().language
}
