package dev.etran.luckyblock.listeners

import com.andrei1058.bedwars.api.events.player.PlayerBedBreakEvent
import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.abilities.Allies
import dev.etran.luckyblock.abilities.BwBridge
import dev.etran.luckyblock.abilities.Companions
import dev.etran.luckyblock.abilities.Disguises
import dev.etran.luckyblock.abilities.Kit
import dev.etran.luckyblock.abilities.Live
import dev.etran.luckyblock.abilities.Traders
import dev.etran.luckyblock.abilities.Uses
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Arrow
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.block.BlockFace
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.EntityBlockFormEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityCombustByBlockEvent
import org.bukkit.event.entity.EntityCombustByEntityEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

/** The events behind the lucky items of the Hypixel catalog: hits, shots, falls, steps, trades. */
class AbilityListener : Listener {

    private fun held(p: Player): ItemStack? = p.itemInHand

    private fun shooterOf(damager: Entity): Player? {
        if (damager is Player) return damager
        if (damager is Projectile) return damager.shooter as? Player
        return null
    }

    // ---------------------------------------------------------------- damage

    /** The mob (or the shooter of the projectile) behind a hit, if it fights for a player. */
    private fun allyOf(damager: Entity): Entity? {
        if (Allies.isAlly(damager.uniqueId)) return damager
        val shooter = (damager as? Projectile)?.shooter as? Entity ?: return null
        return if (Allies.isAlly(shooter.uniqueId)) shooter else null
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onHit(event: EntityDamageByEntityEvent) {
        val victim = event.entity
        if (victim is Player && Live.isInvincible(victim)) {
            event.isCancelled = true
            return
        }
        if (Live.traders.containsKey(victim.uniqueId)) {
            event.isCancelled = true
            return
        }
        // companions, skeletons, creepers and withers never hurt their own team (BedWars1058 only stops players)
        val ally = allyOf(event.damager)
        if (ally != null && victim is Player && Allies.isFriend(ally.uniqueId, victim)) {
            event.isCancelled = true
            return
        }
        val attacker = shooterOf(event.damager) ?: return
        // nor can the team hurt its own companions
        if (Allies.isFriend(victim.uniqueId, attacker)) {
            event.isCancelled = true
            return
        }
        if (victim is Player && Kit.sameTeam(attacker, victim)) return
        if (Live.disguised.containsKey(attacker.uniqueId)) Disguises.end(attacker)

        // thrown / shot things
        if (event.damager is Projectile) {
            when (Live.projectiles.remove(event.damager.uniqueId)) {
                "fling" -> Kit.throwLater(victim, event.damager.velocity.clone().normalize().multiply(2.6).setY(1.0))
                "chicken" -> {
                    Kit.blast(victim.location, 4.0, 6.0, 1.6, attacker)
                    victim.world.spawnEntity(victim.location, EntityType.CHICKEN).let { c -> Kit.runLater(30L) { c.remove() } }
                }
            }
            return
        }

        // Hot Potato: hitting an enemy hands it over
        if (victim is Player && Kit.isPlaying(victim)) {
            val potato = Live.potatoes.firstOrNull { it.holder == attacker.uniqueId }
            if (potato != null && Live.potatoes.none { it.holder == victim.uniqueId }) {
                potato.holder = victim.uniqueId
                potato.lastCount = -1L
                attacker.sendMessage(ChatColor.GOLD.toString() + "You passed the Hot Potato to ${victim.name}!")
                victim.sendMessage(ChatColor.RED.toString() + "${attacker.name} passed you the HOT POTATO! Hit an enemy before it explodes!")
            }
        }

        // melee: what the attacker holds
        val item = held(attacker)
        when (Kit.typeOf(item)) {
            "angel_sword" -> {
                event.damage = event.damage + 14.0
                Kit.throwLater(victim, attacker.location.direction.normalize().multiply(2.8).setY(0.9))
                attacker.inventory.removeItem(item)
                attacker.sendMessage(ChatColor.GOLD.toString() + "The Angel of Death's Sword crumbles after its strike.")
            }
            "kb_slimeball" -> {
                Kit.throwLater(victim, attacker.location.direction.normalize().multiply(3.2).setY(0.7))
                Kit.consume(attacker, item!!)
            }
            "justice_iron", "justice_gold" -> {
                val now = Live.now()
                if ((Live.justiceReady[attacker.uniqueId] ?: 0L) <= now) {
                    Live.justiceReady[attacker.uniqueId] = now + 7000L
                    attacker.health = minOf(attacker.maxHealth, attacker.health + (if (Kit.typeOf(item) == "justice_gold") 4.0 else 3.0))
                }
            }
        }
        val gauntlet = Live.gauntlet[attacker.uniqueId]
        if (gauntlet != null) event.damage = event.damage + gauntlet
        if (Live.isVampire(attacker)) {
            attacker.health = minOf(attacker.maxHealth, attacker.health + maxOf(1.0, event.damage * 0.6))
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        val p = event.entity as? Player ?: return
        val void = event.cause == EntityDamageEvent.DamageCause.VOID
        // a Super Star does not save from the void: BedWars1058 kills players who fall below the map with void
        // damage, and cancelling it would leave them falling forever
        if (Live.isInvincible(p) && !void) {
            event.isCancelled = true
            return
        }
        if (event.cause == EntityDamageEvent.DamageCause.FALL) {
            if (Live.hasNoFall(p)) {
                event.isCancelled = true
                return
            }
            val boots = Kit.wornType(p.inventory.boots)
            val hat = Kit.wornType(p.inventory.helmet)
            if (boots == "slime_boots") {
                event.isCancelled = true
                val bounce = p.velocity
                bounce.y = 0.9
                p.velocity = bounce
                return
            }
            if (hat == "frog_helmet") {
                event.isCancelled = true
                return
            }
        }
        // Wildcard: the death that would have happened sends you back to your base instead
        if (event.finalDamage >= p.health) {
            if (!Kit.isPlaying(p)) return
            val card = p.inventory.contents.firstOrNull { Kit.typeOf(it) == "wildcard" } ?: return
            val spawn = Kit.arenaOf(p)?.getTeam(p)?.spawn ?: return
            event.isCancelled = true
            Kit.consume(p, card)
            p.health = p.maxHealth
            p.fallDistance = 0f
            p.fireTicks = 0
            p.velocity = org.bukkit.util.Vector(0, 0, 0)
            p.teleport(spawn)
            p.sendMessage(ChatColor.GOLD.toString() + "Wildcard! You were saved and sent back to your base.")
        }
    }

    @EventHandler
    fun onTarget(event: EntityTargetLivingEntityEvent) {
        val target = event.target ?: return
        if (target is Player) {
            if (Allies.isFriend(event.entity.uniqueId, target)) event.isCancelled = true
            return
        }
        // two mobs of the same team don't fight each other
        if (Allies.sameSide(event.entity.uniqueId, target.uniqueId)) event.isCancelled = true
    }

    /** Skeletons and zombies that fight for a player don't burn in daylight. */
    @EventHandler(ignoreCancelled = true)
    fun onCombust(event: EntityCombustEvent) {
        if (event is EntityCombustByEntityEvent || event is EntityCombustByBlockEvent) return
        if (Allies.isAlly(event.entity.uniqueId)) event.isCancelled = true
    }

    /** Lucky mobs don't change the map: no snow trails from the snowman, no blocks broken by the wither or giant. */
    @EventHandler(ignoreCancelled = true)
    fun onMobBlock(event: EntityChangeBlockEvent) {
        val id = event.entity.uniqueId
        if (Allies.isAlly(id) || Live.giants.any { it.entity.uniqueId == id }) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onSnowTrail(event: EntityBlockFormEvent) {
        if (Allies.isAlly(event.entity.uniqueId)) event.isCancelled = true
    }

    // ------------------------------------------------------------- projectiles

    @EventHandler(ignoreCancelled = true)
    fun onShoot(event: EntityShootBowEvent) {
        if (event.entity !is Player) return
        when (Kit.typeOf(event.bow)) {
            "fling_bow" -> Live.projectiles[event.projectile.uniqueId] = "fling"
            "chicken_bow" -> Live.projectiles[event.projectile.uniqueId] = "chicken"
        }
    }

    private val thrownTypes = setOf("water_balloon", "time_warp", "rush_pearl")

    /**
     * Remembers which lucky projectile is being thrown. On some versions the last snowball / pearl leaves the hand
     * before ProjectileLaunchEvent, so the hand alone can't tell what was thrown then.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onThrowClick(event: PlayerInteractEvent) {
        val a = event.action.name
        if (!a.startsWith("RIGHT_CLICK")) return
        val type = Kit.typeOf(event.item) ?: return
        if (type in thrownTypes) Live.throwing[event.player.uniqueId] = Live.Throw(type, Live.now())
    }

    @EventHandler(ignoreCancelled = true)
    fun onLaunch(event: ProjectileLaunchEvent) {
        val p = event.entity.shooter as? Player ?: return
        val recent = Live.throwing.remove(p.uniqueId)
        val type = Kit.typeOf(held(p))?.takeIf { it in thrownTypes }
            ?: recent?.takeIf { Live.now() - it.at < 250L }?.type ?: return
        val id = event.entity.uniqueId
        when (type) {
            "water_balloon" -> Live.projectiles[id] = "water"
            "time_warp" -> {
                Live.warpBack[p.uniqueId] = Live.Warp(p.location, Kit.arenaOf(p)?.arenaName)
                Live.projectiles[id] = "time_warp"
            }
            "rush_pearl" -> {
                val projectile = event.entity
                val velocity = projectile.velocity.clone().normalize().multiply(1.7)
                object : BukkitRunnable() {
                    var ticks = 0
                    override fun run() {
                        if (!projectile.isValid || ticks++ > 60) {
                            cancel()
                            return
                        }
                        projectile.velocity = velocity
                    }
                }.runTaskTimer(LuckyBlocks.getInstance(), 1L, 1L)
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onProjectileHit(event: ProjectileHitEvent) {
        val id = event.entity.uniqueId
        val kind = Live.projectiles[id] ?: return
        when (kind) {
            "water" -> {
                Live.projectiles.remove(id)
                // where it hit, the snowball may already be inside the block: step back along its path to free space
                val back = event.entity.velocity.clone()
                if (back.lengthSquared() > 1.0E-6) back.normalize().multiply(-0.5)
                var spot = event.entity.location.block
                if (spot.type != Material.AIR) spot = event.entity.location.add(back).block
                val shooter = event.entity.shooter as? Player
                val arena = shooter?.let { Kit.arenaOf(it) }
                if (spot.type == Material.AIR && Kit.place(spot, Kit.water(), arena)) {
                    Live.tempBlock(spot.location, Material.AIR, 5000L, Kit.water(), dev.etran.luckyblock.compat.Items.material("STATIONARY_WATER", "WATER"))
                }
            }
            "time_warp" -> {
                Live.projectiles.remove(id)
                val shooter = event.entity.shooter as? Player ?: return
                Live.warpBack[shooter.uniqueId]?.landedAt = Live.now()
            }
            // the arrow hit something: the damage event (if it was an entity) comes right after, so wait a tick
            "fling" -> Kit.runLater(1L) { Live.projectiles.remove(id) }
            "chicken" -> {
                Live.projectiles.remove(event.entity.uniqueId)
                val shooter = (event.entity as? Arrow)?.shooter as? Player
                Kit.blast(event.entity.location, 3.5, 5.0, 1.4, shooter)
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) {
        if (event.cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return
        val p = event.player
        val warp = Live.warpBack[p.uniqueId] ?: return
        // only the Time Warp Pearl that just landed (not a later, normal pearl after one fell into the void)
        if (warp.landedAt == 0L || Live.now() - warp.landedAt > 1000L) return
        Live.warpBack.remove(p.uniqueId)
        p.sendMessage(ChatColor.LIGHT_PURPLE.toString() + "Time Warp: you snap back in 5 seconds!")
        Kit.runLater(100L) {
            // not after dying and respawning, and never into a game that has ended
            if (Live.warpBack.containsKey(p.uniqueId)) return@runLater
            if (Kit.isPlaying(p) && Kit.arenaOf(p)?.arenaName == warp.arena && p.world == warp.origin.world) {
                p.fallDistance = 0f
                p.teleport(warp.origin)
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        val p = event.player
        val rod = held(p) ?: return
        if (Kit.typeOf(rod) != "grapple") return
        val state = event.state.name
        if (state != "IN_GROUND" && state != "CAUGHT_ENTITY" && state != "FAILED_ATTEMPT" && state != "REEL_IN") return
        val to = event.hook.location
        // a hook that is still flying or floating in water has nothing to pull towards
        if (state != "IN_GROUND" && state != "CAUGHT_ENTITY") {
            val under = to.clone().add(0.0, -0.2, 0.0).block
            if (!under.type.isSolid && !to.block.type.isSolid) return
        }
        val pull = to.toVector().subtract(p.location.toVector())
        if (pull.length() < 2.0) return
        p.velocity = pull.multiply(0.32).setY(maxOf(0.55, pull.y * 0.28 + 0.45))
        p.fallDistance = 0f
        rod.durability = (rod.durability + 21).toShort()
        if (rod.durability >= rod.type.maxDurability) p.inventory.removeItem(rod)
    }

    // ----------------------------------------------------------------- movement

    @EventHandler(ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        val p = event.player
        val to = event.to ?: return
        if (Live.isFrozen(p)) {
            to.x = event.from.x
            to.z = event.from.z
            return
        }
        if (Live.traps.isEmpty()) return
        val feet = to.block.location
        val trap = Live.traps[feet] ?: return
        // the carpet was blown away: the trap went with it
        if (!feet.block.type.name.contains("CARPET")) {
            Live.traps.remove(feet)
            return
        }
        // spectators and players waiting to respawn fly over traps without springing them
        if (!Kit.isPlaying(p)) return
        val owner = Bukkit.getPlayer(trap.owner)
        if (p.uniqueId == trap.owner || (owner != null && Kit.sameTeam(owner, p))) return
        Live.traps.remove(feet)
        feet.block.type = Material.AIR
        Uses.springTrap(trap, p)
    }

    // -------------------------------------------------------------------- items

    @EventHandler
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        if (!Kit.isMainHand(event)) return
        val p = event.player
        val clicked = event.rightClicked
        val trader = Live.traders[clicked.uniqueId]
        if (trader != null) {
            event.isCancelled = true
            Traders.trade(p, trader)
            return
        }
        if (clicked is Player) {
            val potato = Live.potatoes.firstOrNull { it.holder == p.uniqueId }
            if (potato != null && Kit.isPlaying(clicked) && !Kit.sameTeam(p, clicked) && Live.potatoes.none { it.holder == clicked.uniqueId }) {
                event.isCancelled = true
                potato.holder = clicked.uniqueId
                potato.lastCount = -1L
                p.sendMessage(ChatColor.GOLD.toString() + "You passed the Hot Potato to ${clicked.name}!")
                clicked.sendMessage(ChatColor.RED.toString() + "${p.name} passed you the HOT POTATO! Hit an enemy before it explodes!")
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onConsume(event: PlayerItemConsumeEvent) {
        val p = event.player
        when (Kit.typeOf(event.item)) {
            "sugar_cookie" -> {
                Kit.effect(p, 20, 2, "SPEED")
                p.sendMessage(ChatColor.GOLD.toString() + "Sugar rush! Run!")
            }
            "mystery_meat" -> {
                val good = Kit.random.nextBoolean()
                if (good) {
                    Kit.effect(p, 12, 1, "REGENERATION")
                    Kit.effect(p, 20, 0, "INCREASE_DAMAGE", "STRENGTH")
                    p.sendMessage(ChatColor.GREEN.toString() + "The Mystery Meat was delicious!")
                } else {
                    Kit.effect(p, 10, 0, "CONFUSION", "NAUSEA")
                    Kit.effect(p, 6, 0, "POISON")
                    Kit.effect(p, 8, 0, "BLINDNESS")
                    p.sendMessage(ChatColor.DARK_GREEN.toString() + "The Mystery Meat was... off.")
                }
            }
        }
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val p = event.whoClicked as? Player ?: return
        if (!Live.companionMenu.contains(p.uniqueId)) return
        event.isCancelled = true
        // a click in the menu itself (raw slots below the menu's size), not in the player's own inventory
        if (event.rawSlot in 0 until event.view.topInventory.size) Companions.pick(p, event.rawSlot)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val p = event.player as? Player ?: return
        if (Live.companionMenu.contains(p.uniqueId)) Companions.closed(p)
    }

    // --------------------------------------------------------------------- beds

    private fun bedKey(p: Player): String? {
        val arena = Kit.arenaOf(p) ?: return null
        val team = arena.getTeam(p) ?: return null
        return arena.worldName + "/" + team.name
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlaceBed(event: BlockPlaceEvent) {
        if (Kit.typeOf(event.itemInHand) != "placeable_bed") return
        val p = event.player
        val arena = Kit.arenaOf(p) ?: return
        val team = arena.getTeam(p) ?: return
        val key = bedKey(p) ?: return
        val foot = event.block
        // a backup bed keeps a team in the game only while it has a bed; it can't bring a team back
        if (team.isBedDestroyed && !Live.mainBedGone.contains(key)) {
            event.isCancelled = true
            p.sendMessage(ChatColor.RED.toString() + "Your team's bed is already gone - a backup bed can't bring it back.")
            return
        }
        // a backup bed within 2 blocks of a team bed would count as that team bed for BedWars1058
        if (arena.teams.any { t -> t.bed != null && t.bed.world == foot.world && t.bed.distanceSquared(foot.location) <= 9.0 }) {
            event.isCancelled = true
            p.sendMessage(ChatColor.RED.toString() + "Place your backup bed further away from the team beds.")
            return
        }
        Live.backupBeds.getOrPut(key) { HashSet() }.add(foot.location)
        Live.backupTeams[key] = team
        // BedWars1058 only marks the clicked half as player-placed, so the other half could not be broken: mark it too
        Kit.runInGame(arena, 1L) {
            for (face in listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)) {
                val other = foot.getRelative(face)
                if (Kit.isBed(other.type) && !arena.isBlockPlaced(other)) {
                    arena.addPlacedBlock(other)
                    break
                }
            }
        }
        p.sendMessage(ChatColor.GOLD.toString() + "Backup bed placed! While it stands, your team is not out when the main bed is destroyed.")
    }

    @EventHandler
    fun onBedBreak(event: PlayerBedBreakEvent) {
        val team = event.victimTeam
        val key = event.arena.worldName + "/" + team.name
        if (Live.standingBackupBeds(key).isEmpty()) return
        Live.backupTeams[key] = team
        Live.mainBedGone.add(key)
        // BedWars1058 sets the flag before this event and never again after it, so it is undone right here. Doing
        // it a few ticks later (as before) left a window in which a team mate who died got a final kill.
        BwBridge.setBedFlag(team, false)
        Kit.runLater(1L) {
            for (m in team.members) m.sendMessage(ChatColor.GOLD.toString() + "Your main bed is gone, but your backup bed keeps you in the game!")
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreakBlock(event: BlockBreakEvent) {
        val arena = Kit.arenaOf(event.player) ?: return
        val at = event.block.location
        for ((key, spots) in Live.backupBeds) {
            if (!key.startsWith(arena.worldName + "/")) continue
            if (!Kit.isBed(event.block.type)) continue
            val hit = spots.firstOrNull { it.world == at.world && it.distanceSquared(at) <= 2.0 } ?: continue
            spots.remove(hit)
            val team = arena.getTeam(key.substringAfter('/')) ?: continue
            if (spots.isEmpty()) {
                if (Live.mainBedGone.remove(key)) BwBridge.setBedFlag(team, true)
                for (m in team.members) m.sendMessage(ChatColor.RED.toString() + "Your backup bed was destroyed!")
            }
        }
    }

    // -------------------------------------------------------------------- cleanup

    @EventHandler(priority = EventPriority.LOW)
    fun onDeath(event: PlayerDeathEvent) {
        // the disguise comes off first, so the armor from before it is back for the lucky armor clean-up after
        Disguises.end(event.entity)
        Live.forgetPlayer(event.entity.uniqueId, false)
        Companions.forget(event.entity.uniqueId)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val p = event.player
        // a rejoining player must not come back in disguise or with lucky armor over their real armor
        Disguises.end(p)
        Kit.takeOffLucky(p)
        Live.forgetPlayer(p.uniqueId, true)
        Companions.forget(p.uniqueId)
    }
}
