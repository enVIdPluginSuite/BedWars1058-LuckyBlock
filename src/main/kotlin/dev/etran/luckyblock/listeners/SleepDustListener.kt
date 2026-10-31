package dev.etran.luckyblock.listeners

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.compat.Compat
import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.luckyblock.texture.GlassColor
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

class SleepDustListener : Listener {

    private companion object {
        /** How long the carpet slows its victim: 6 seconds. */
        const val SLEEP_TICKS = 120
    }

    private val traps: MutableSet<Location> = HashSet()
    private val sleeping: MutableSet<UUID> = HashSet()
    private val placedBy: MutableMap<Location, UUID> = HashMap()

    // a 2-line hologram above each sleeping player
    private val holograms: MutableMap<UUID, List<ArmorStand>> = HashMap()

    // PLACE
    @EventHandler
    fun onPlace(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        // Since 1.9 this event fires once per hand; only handle the main hand.
        if (!Compat.isMainHand(event)) return

        val stack = event.item ?: return
        if (!NBTUtils.hasNBTTag(stack, "sleepDust")) return

        val player = event.player
        val clicked = event.clickedBlock ?: return

        // The item is a carpet, so stop vanilla from also placing it as a normal block
        event.isCancelled = true

        val place = clicked.location.add(0.0, 1.0, 0.0)
        if (place.block.type != Material.AIR) return

        Items.carpet(GlassColor.PURPLE).place(place.block)

        traps.add(place.block.location)
        placedBy[place.block.location] = player.uniqueId

        stack.amount = stack.amount - 1
    }

    // STEP ON TRAP
    @EventHandler
    fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val id = player.uniqueId

        // a sleeper is only slowed down, they can still move
        if (sleeping.contains(id)) return

        val feet = player.location.block.location
        if (!traps.contains(feet)) return

        val owner = placedBy[feet]
        if (owner != null && owner == id) return

        traps.remove(feet)
        placedBy.remove(feet)
        feet.block.type = Material.AIR

        startSleep(player)
    }

    // WAKE UP
    @EventHandler
    fun onRightClick(event: PlayerInteractEntityEvent) {
        val target = event.rightClicked as? Player ?: return
        val clicker = event.player
        val id = target.uniqueId
        if (!sleeping.contains(id)) return

        sleeping.remove(id)
        removeHologram(id)
        removeSlowness(target)

        target.sendMessage(ChatColor.GREEN.toString() + "You were woken up by " + clicker.name)
    }

    // SLEEP SYSTEM: a short slowdown with a hologram that follows the player
    private fun startSleep(player: Player) {
        val id = player.uniqueId
        sleeping.add(id)

        val slowness = slowness()
        if (slowness != null) player.addPotionEffect(PotionEffect(slowness, SLEEP_TICKS, 2), true)

        createHologram(player)

        object : BukkitRunnable() {
            var ticks = SLEEP_TICKS

            override fun run() {
                if (!player.isOnline || !sleeping.contains(id)) {
                    sleeping.remove(id)
                    removeHologram(id)
                    cancel()
                    return
                }

                if (ticks <= 0) {
                    sleeping.remove(id)
                    removeHologram(id)
                    player.sendMessage(ChatColor.GREEN.toString() + "☀ You woke up!")
                    cancel()
                    return
                }

                moveHologram(player)
                updateHologram(player, (ticks + 19) / 20)

                ticks -= 2
            }
        }.runTaskTimer(LuckyBlocks.getInstance(), 0L, 2L)
    }

    private fun slowness(): PotionEffectType? = PotionEffectType.getByName("SLOW") ?: PotionEffectType.getByName("SLOWNESS")

    private fun removeSlowness(player: Player) {
        val type = slowness() ?: return
        player.removePotionEffect(type)
    }

    private fun moveHologram(player: Player) {
        val list = holograms[player.uniqueId] ?: return
        val base = player.location.add(0.0, 2.4, 0.0)
        if (!list[0].isDead) list[0].teleport(base.clone().add(0.0, 0.25, 0.0))
        if (!list[1].isDead) list[1].teleport(base.clone().add(0.0, 0.05, 0.0))
    }

    private fun spawnLine(player: Player, at: Location): ArmorStand {
        val line = player.world.spawnEntity(at, EntityType.ARMOR_STAND) as ArmorStand
        line.isVisible = false
        line.setGravity(false)
        line.isCustomNameVisible = true
        line.isSmall = true
        line.setMarker(true)
        return line
    }

    // HOLOGRAM CREATE (2 LINES + FIXED HEIGHT)
    private fun createHologram(player: Player) {
        val base = player.location.add(0.0, 2.4, 0.0)
        val line1 = spawnLine(player, base.clone().add(0.0, 0.25, 0.0))
        val line2 = spawnLine(player, base.clone().add(0.0, 0.05, 0.0))
        holograms[player.uniqueId] = listOf(line1, line2)
        updateHologram(player, SLEEP_TICKS / 20)
    }

    // HOLOGRAM UPDATE (LIVE COUNTDOWN)
    private fun updateHologram(player: Player, time: Int) {
        val list = holograms[player.uniqueId] ?: return
        list[0].customName = ChatColor.LIGHT_PURPLE.toString() + player.name + " is drowsy for " + time + "s"
        list[1].customName = ChatColor.YELLOW.toString() + "Right click to wake up"
    }

    // REMOVE HOLOGRAM
    private fun removeHologram(id: UUID) {
        val list = holograms.remove(id) ?: return
        for (stand in list) {
            if (!stand.isDead) stand.remove()
        }
    }
}
