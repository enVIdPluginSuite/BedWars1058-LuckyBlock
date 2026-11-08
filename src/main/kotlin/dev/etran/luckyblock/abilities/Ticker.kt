package dev.etran.luckyblock.abilities

import dev.etran.luckyblock.LuckyBlocks
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector

/** The repeating work of the lucky items: worn hats and boots, riders, giants, timers. Runs every 2 ticks. */
internal object Ticker {
    private var task: BukkitTask? = null
    private var step = 0L

    private val discoColors = listOf(
        Color.RED, Color.ORANGE, Color.YELLOW, Color.LIME, Color.AQUA, Color.BLUE, Color.PURPLE, Color.FUCHSIA
    )

    fun start() {
        stop()
        task = Bukkit.getScheduler().runTaskTimer(LuckyBlocks.getInstance(), Runnable { run() }, 2L, 2L)
    }

    fun stop() {
        task?.cancel()
        task = null
        Live.clearAll()
    }

    private fun run() {
        step++
        try {
            fast()
            if (step % 3L == 0L) slow()
        } catch (e: RuntimeException) {
            LuckyBlocks.getInstance().logger.warning("Lucky item ticker: $e")
        }
    }

    /** Every 2 ticks: things that move. */
    private fun fast() {
        val now = Live.now()

        // blaze riders: the blaze flies where the rider looks
        for (r in ArrayList(Live.riders)) {
            if (!Live.riders.contains(r)) continue
            val rider = Bukkit.getPlayer(r.rider)
            if (now >= r.until || !r.blaze.isValid || rider == null || rider.isDead || r.blaze.passenger != rider) {
                if (rider != null && r.blaze.passenger == rider) r.blaze.eject()
                // the ride may end high in the air: no fall damage for the way down
                if (rider != null) {
                    rider.fallDistance = 0f
                    Live.noFall[rider.uniqueId] = now + 6000L
                }
                r.blaze.remove()
                Live.riders.remove(r)
                continue
            }
            (r.blaze as? org.bukkit.entity.Creature)?.target = null
            r.blaze.velocity = rider.location.direction.normalize().multiply(0.55)
            r.blaze.fallDistance = 0f
        }

        // giants walk to the nearest player and hit hard
        for (g in ArrayList(Live.giants)) {
            if (!Live.giants.contains(g)) continue
            if (now >= g.until || !g.entity.isValid) {
                g.entity.remove()
                Live.giants.remove(g)
                continue
            }
            val target = g.entity.world.players
                .filter { Kit.isPlaying(it) && !Live.isInvincible(it) && it.location.distanceSquared(g.entity.location) < 40.0 * 40.0 }
                .minByOrNull { it.location.distanceSquared(g.entity.location) } ?: continue
            val to = target.location.toVector().subtract(g.entity.location.toVector())
            val flat = Vector(to.x, 0.0, to.z)
            val distance = flat.length()
            if (distance > 2.5) {
                flat.normalize().multiply(0.26)
                val ahead = g.entity.location.add(flat.clone().multiply(5.0)).block
                val up = if (ahead.type.isSolid) 0.5 else g.entity.velocity.y
                g.entity.velocity = Vector(flat.x, up, flat.z)
            } else if (now - g.lastHit > 1000L) {
                g.lastHit = now
                target.damage(7.0, g.entity)
                Kit.throwLater(target, flat.normalize().multiply(0.9).setY(0.4))
            }
        }

        // explosive chickens go off when an enemy comes close
        for (c in ArrayList(Live.chickens)) {
            if (!Live.chickens.contains(c)) continue
            if (now >= c.until || !c.entity.isValid) {
                c.entity.remove()
                Live.chickens.remove(c)
                continue
            }
            val owner = Bukkit.getPlayer(c.owner)
            val near = c.entity.world.players.firstOrNull {
                Kit.isPlaying(it) && it.uniqueId != c.owner &&
                    (owner == null || !Kit.sameTeam(owner, it)) && it.location.distanceSquared(c.entity.location) <= 9.0
            }
            if (near != null) {
                Live.chickens.remove(c)
                Kit.blast(c.entity.location, 4.0, 8.0, 1.7, owner)
                c.entity.remove()
            }
        }
    }

    /** Roughly every 6 ticks: timers and worn items. */
    private fun slow() {
        val now = Live.now()
        Live.restoreTemps(false)
        if (step % 9L == 0L) Allies.tick()

        // traders and Jerry vanish
        val traders = Live.traders.values.iterator()
        while (traders.hasNext()) {
            val t = traders.next()
            if (now >= t.until || !t.entity.isValid) {
                t.entity.remove()
                traders.remove()
            }
        }

        // hot potatoes
        // (a copy: the explosion can kill the holder, and the death event edits this list while we go through it)
        for (pt in ArrayList(Live.potatoes)) {
            if (!Live.potatoes.contains(pt)) continue
            val holder = Bukkit.getPlayer(pt.holder)
            if (holder == null || !Kit.isPlaying(holder)) {
                Live.potatoes.remove(pt)
                continue
            }
            val secondsLeft = (pt.explodeAt - now + 999L) / 1000L
            if (secondsLeft in 1..5 && secondsLeft != pt.lastCount) {
                pt.lastCount = secondsLeft
                holder.sendMessage(ChatColor.RED.toString() + "Hot Potato: $secondsLeft...")
            }
            if (now >= pt.explodeAt) {
                Kit.blast(holder.location, 3.0, 4.0, 1.0, null)
                holder.damage(8.0)
                holder.fireTicks = 60
                Live.potatoes.remove(pt)
            }
        }

        // teleport beams
        // (a copy: teleporting fires events that may edit this list)
        for (b in ArrayList(Live.beams)) {
            if (!Live.beams.contains(b)) continue
            val p = Bukkit.getPlayer(b.player)
            if (p == null || !Kit.isPlaying(p)) {
                Live.beams.remove(b)
                continue
            }
            if (p.location.world != b.start.world || p.location.distanceSquared(b.start) > 2.0) {
                p.sendMessage(ChatColor.RED.toString() + "You moved - the Teleport Beam was cancelled.")
                Live.beams.remove(b)
            } else if (now >= b.until) {
                Live.beams.remove(b)
                val spawn = Kit.arenaOf(p)?.getTeam(p)?.spawn
                if (spawn != null) p.teleport(spawn) else p.sendMessage(ChatColor.RED.toString() + "You have no base to go to.")
            }
        }

        // disguises end (collected first: Disguises.end changes the map)
        val ending = Live.disguised.entries.filter { now >= it.value.until || Bukkit.getPlayer(it.key)?.isDead != false }.map { it.key }
        for (id in ending) {
            val p = Bukkit.getPlayer(id)
            if (p == null) Live.disguised.remove(id) else Disguises.end(p)
        }

        // backup beds blown up by TNT or removed by a Magic Toy Stick never fire a block break event
        if (step % 15L == 0L) checkBackupBeds()

        for (p in Bukkit.getOnlinePlayers()) wear(p)
    }

    private fun wear(p: Player) {
        if (p.isDead) return
        val inv = p.inventory
        val hat = Kit.wornType(inv.helmet)
        val boots = Kit.wornType(inv.boots)

        when (hat) {
            "chicken_hat" -> if (step % 60L == 0L && p.inventory.contains(Kit.mat("EGG"), 16).not()) {
                p.inventory.addItem(org.bukkit.inventory.ItemStack(Kit.mat("EGG"), 1))
            }
            "disco_helmet" -> {
                val helmet = inv.helmet
                val meta = helmet.itemMeta as? LeatherArmorMeta
                if (meta != null) {
                    meta.color = discoColors[((step / 3) % discoColors.size).toInt()]
                    helmet.itemMeta = meta
                    inv.helmet = helmet
                }
            }
            "hot_head" -> if (step % 30L == 0L) {
                p.fireTicks = 50
                for (e in Kit.enemies(p)) if (e.location.distanceSquared(p.location) <= 9.0) e.fireTicks = 80
            }
            "frog_helmet" -> Kit.effect(p, 2, 3, "JUMP", "JUMP_BOOST")
        }
        when (boots) {
            "heat_boots" -> {
                Kit.effect(p, 2, 0, "FIRE_RESISTANCE")
                if (p.fireTicks > 0) p.fireTicks = 0
            }
            "squid_boots" -> if (p.isSneaking && p.isOnGround) {
                // BedWars1058 locks the armor slots, so the boots can't be taken off to travel: crouch to make water
                val arena = Kit.arenaOf(p)
                var placed = 0
                val feet = p.location.block
                for (dx in -2..2) for (dz in -2..2) for (dy in 0..1) {
                    if (placed >= 6) break
                    val b = feet.getRelative(dx, dy, dz)
                    if (b.type == Material.AIR && (dx != 0 || dz != 0) && Kit.canBuild(b, arena)) {
                        b.type = Kit.water()
                        Live.tempBlock(b.location, Material.AIR, 3000L, Kit.water(), dev.etran.luckyblock.compat.Items.material("STATIONARY_WATER", "WATER"))
                        placed++
                    }
                }
            }
        }
    }

    /** A team whose main bed is gone and whose last backup bed vanished without a break event is now out of beds. */
    private fun checkBackupBeds() {
        for (key in ArrayList(Live.mainBedGone)) {
            if (Live.standingBackupBeds(key).isNotEmpty()) continue
            Live.mainBedGone.remove(key)
            val team = Live.backupTeams[key] ?: continue
            try {
                BwBridge.setBedFlag(team, true)
                for (m in team.members) m.sendMessage(ChatColor.RED.toString() + "Your backup bed was destroyed!")
            } catch (e: RuntimeException) {
                // the game is over
            }
        }
    }
}
