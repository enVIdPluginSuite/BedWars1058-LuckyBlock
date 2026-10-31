package dev.etran.luckyblock.listeners

import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.compat.Compat
import dev.etran.luckyblock.drops.ArmorDrop
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Creature
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
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

    // A cookie can only be eaten while hungry, and BedWars players are always full, so right click eats it directly.
    @EventHandler
    fun onUse(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (!Compat.isMainHand(event)) return
        val stack = event.item ?: return
        val player = event.player
        if (NBTUtils.hasNBTTag(stack, "opCookie")) {
            event.isCancelled = true
            applyCookie(player)
            consume(player, stack)
        } else if (NBTUtils.hasNBTTag(stack, "dreamDefender")) {
            // the item is an iron block, so don't let it be placed
            event.isCancelled = true
            val arena = LuckyBlocks.getBedWars().arenaUtil.getArenaByPlayer(player) ?: return
            summonDefender(player, arena)
            consume(player, stack)
        }
    }

    /** An iron golem that goes after the closest enemy for 45 seconds. */
    private fun summonDefender(owner: Player, arena: IArena) {
        val ownTeam = arena.getTeam(owner)
        val golem = owner.world.spawnEntity(owner.location, EntityType.IRON_GOLEM) as? Creature ?: return
        golem.customName = ChatColor.WHITE.toString() + owner.name + "'s Dream Defender"
        golem.isCustomNameVisible = true
        owner.sendMessage(ChatColor.WHITE.toString() + "Your Dream Defender has arrived!")
        object : BukkitRunnable() {
            var ticks = 0

            override fun run() {
                ticks += 20
                if (golem.isDead || !golem.isValid || ticks > 900) {
                    golem.remove()
                    cancel()
                    return
                }
                var nearest: Player? = null
                var best = 400.0
                for (other in arena.players) {
                    if (other == owner || arena.getTeam(other) === ownTeam || other.world != golem.world) continue
                    val distance = other.location.distanceSquared(golem.location)
                    if (distance < best) {
                        best = distance
                        nearest = other
                    }
                }
                golem.target = nearest
            }
        }.runTaskTimer(LuckyBlocks.getInstance(), 20L, 20L)
    }

    private fun consume(player: Player, stack: ItemStack) {
        if (stack.amount > 1) stack.amount = stack.amount - 1 else player.inventory.removeItem(stack)
    }

    // BED BUG: remember which snowballs were thrown from a Bed Bug stack
    @EventHandler(ignoreCancelled = true)
    fun onLaunch(event: ProjectileLaunchEvent) {
        val shooter = event.entity.shooter as? Player ?: return
        @Suppress("DEPRECATION")
        val held = shooter.itemInHand
        if (NBTUtils.hasNBTTag(held, "bedBug")) bugs[event.entity.uniqueId] = shooter.uniqueId
    }

    @EventHandler
    fun onHit(event: ProjectileHitEvent) {
        val owner = bugs.remove(event.entity.uniqueId) ?: return
        val at = event.entity.location
        val bug = at.world?.spawnEntity(at, EntityType.SILVERFISH) as? Creature ?: return
        bug.customName = ChatColor.LIGHT_PURPLE.toString() + "Bed Bug"
        val arena = LuckyBlocks.getBedWars().arenaUtil.getArenaByPlayer(Bukkit.getPlayer(owner) ?: return)
        val ownTeam = arena?.getTeam(Bukkit.getPlayer(owner))
        object : BukkitRunnable() {
            var ticks = 0

            override fun run() {
                ticks += 20
                if (bug.isDead || !bug.isValid || ticks > 300) {
                    bug.remove()
                    cancel()
                    return
                }
                var nearest: Player? = null
                var best = 256.0
                for (other in bug.world.players) {
                    if (other.uniqueId == owner || arena == null || !arena.isPlayer(other) || arena.getTeam(other) === ownTeam) continue
                    val distance = other.location.distanceSquared(bug.location)
                    if (distance < best) {
                        best = distance
                        nearest = other
                    }
                }
                bug.target = nearest
            }
        }.runTaskTimer(LuckyBlocks.getInstance(), 0L, 20L)
    }

    private val bugs: MutableMap<UUID, UUID> = HashMap()

    // normal eating still works if something else lets it through
    @EventHandler(ignoreCancelled = true)
    fun onEat(event: PlayerItemConsumeEvent) {
        if (!NBTUtils.hasNBTTag(event.item, "opCookie")) return
        applyCookie(event.player)
    }

    private fun applyCookie(player: Player) {
        effect(player, "INCREASE_DAMAGE", 160, 0)
        effect(player, "SPEED", 200, 1)
        effect(player, "REGENERATION", 100, 1)
        effect(player, "DAMAGE_RESISTANCE", 100, 0)
        effect(player, "ABSORPTION", 200, 0)
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
