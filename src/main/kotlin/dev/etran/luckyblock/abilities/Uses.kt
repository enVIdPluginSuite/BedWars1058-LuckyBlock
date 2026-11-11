package dev.etran.luckyblock.abilities

import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.luckyblock.LuckyBlock
import dev.etran.luckyblock.luckyblock.texture.GlassColor
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.Arrow
import org.bukkit.entity.Chicken
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.WitherSkull
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.math.abs

/**
 * What right clicking a lucky item does. The item's type tag (see Kit.mark) picks the function; it returns true
 * when the item was used up.
 */
internal object Uses {
    fun interface Use {
        fun use(player: Player, arena: IArena?, stack: ItemStack, event: PlayerInteractEvent): Boolean
    }

    private val uses: MutableMap<String, Use> = HashMap()
    private val lastUse: MutableMap<java.util.UUID, Long> = HashMap()

    /** Called for every right click with an item. Returns true if the item was a lucky item. */
    fun dispatch(player: Player, arena: IArena?, stack: ItemStack, event: PlayerInteractEvent): Boolean {
        val type = Kit.typeOf(stack) ?: return false
        val use = uses[type] ?: return false
        event.isCancelled = true
        // some clients/versions send the same right click twice in one tick: one use per player per tick
        val tick = Live.now() / 50L
        if (lastUse[player.uniqueId] == tick) return true
        lastUse[player.uniqueId] = tick
        if (use.use(player, arena, stack, event)) Kit.consume(player, stack)
        return true
    }

    fun forgetUse(id: java.util.UUID) {
        lastUse.remove(id)
    }

    private fun say(player: Player, text: String) {
        player.sendMessage(ChatColor.GOLD.toString() + text)
    }

    private fun fail(player: Player, text: String): Boolean {
        player.sendMessage(ChatColor.RED.toString() + text)
        return false
    }

    init {
        // ---- traps: placed on the floor, they spring when an enemy walks over them ----
        trap("trap_arrow", GlassColor.RED)
        trap("trap_damage", GlassColor.ORANGE)
        trap("trap_slow", GlassColor.GRAY)
        trap("trap_freeze", GlassColor.LIGHT_BLUE)
        trap("trap_poison", GlassColor.GREEN)

        uses["sleepinator"] = Use { p, _, _, _ ->
            val target = Kit.aimedEnemy(p, 14.0, 0.85) ?: return@Use fail(p, "Aim at an enemy within 14 blocks!")
            Kit.effect(target, 6, 0, "BLINDNESS")
            Kit.effect(target, 4, 1, "SLOW", "SLOWNESS")
            target.sendMessage(ChatColor.RED.toString() + p.name + " put you to sleep!")
            say(p, "${target.name} is blinded - keep hitting!")
            true
        }

        uses["bed_pointer"] = Use { p, arena, _, _ ->
            val own = arena?.getTeam(p)
            var best: Location? = null
            for (team in arena?.teams ?: emptyList()) {
                if (team === own) continue
                val spots = ArrayList<Location>(Live.standingBackupBeds(arena!!.worldName + "/" + team.name))
                if (!team.isBedDestroyed) team.bed?.let { spots.add(it) }
                for (spot in spots) {
                    if (spot.world == p.world && (best == null || spot.distanceSquared(p.location) < best.distanceSquared(p.location))) best = spot
                }
            }
            if (best == null) return@Use fail(p, "No enemy bed found.")
            p.compassTarget = best
            say(p, "Your compass now points to the closest enemy bed (${best.distance(p.location).toInt()} blocks away).")
            false
        }

        uses["hot_potato"] = Use { p, _, _, _ ->
            if (Live.potatoes.any { it.holder == p.uniqueId }) return@Use fail(p, "You are already holding a lit Hot Potato!")
            Live.potatoes.add(Live.Potato(p.uniqueId, Live.now() + 6000L))
            p.sendMessage(ChatColor.RED.toString() + "HOT POTATO! It explodes in 6 seconds - hit an enemy to pass it on!")
            true
        }

        uses["lava_rune"] = Use { p, arena, _, e ->
            val face = e.clickedBlock?.getRelative(e.blockFace) ?: return@Use fail(p, "Click on a block to place lava.")
            if (face.type != Material.AIR) return@Use fail(p, "There is no room for lava there.")
            if (!Kit.place(face, Kit.lava(), arena)) return@Use fail(p, "You can't place lava there.")
            Live.tempBlock(face.location, Material.AIR, 30000L, Kit.lava(), Items.material("LAVA", "STATIONARY_LAVA"))
            true
        }

        uses["note_bomb"] = Use { p, arena, _, _ ->
            val item = p.world.dropItem(p.eyeLocation, ItemStack(Kit.mat("NOTE_BLOCK")))
            item.pickupDelay = Int.MAX_VALUE
            item.velocity = Kit.direction(p).multiply(0.9)
            Kit.runInGame(arena, 50L) {
                if (item.isValid) {
                    Kit.blast(item.location, 4.0, 6.0, 1.1, p)
                    item.remove()
                }
            }
            true
        }

        uses["chicken_bomb"] = Use { p, arena, _, _ ->
            val chicken = p.world.spawnEntity(p.location, EntityType.CHICKEN) as Chicken
            Kit.runLater(40L) {
                if (chicken.isValid && Kit.gameRunning(arena)) {
                    Kit.blast(chicken.location, 4.0, 0.0, 1.5, p)
                    for (i in 0 until 3) chicken.world.dropItemNaturally(chicken.location, ItemStack(Kit.mat("EGG")))
                }
                if (chicken.isValid) chicken.remove()
            }
            true
        }

        uses["explosive_chicken"] = Use { p, _, _, e ->
            val above = e.clickedBlock?.getRelative(BlockFace.UP) ?: return@Use fail(p, "Click on a block to place it.")
            if (above.type.isSolid || above.getRelative(BlockFace.UP).type.isSolid) return@Use fail(p, "There is no room for the chicken there.")
            val base = above.location.add(0.5, 0.0, 0.5)
            val chicken = base.world.spawnEntity(base, EntityType.CHICKEN)
            chicken.customName = ChatColor.RED.toString() + "Explosive Chicken"
            chicken.isCustomNameVisible = true
            Live.chickens.add(Live.Chicken(chicken, p.uniqueId, Live.now() + 180000L))
            true
        }

        // ---- scrolls ----
        uses["scroll_creeper"] = Use { p, _, _, _ ->
            for (i in 0 until 3) {
                val at = p.location.add(Kit.random.nextInt(5) - 2.0, 0.0, Kit.random.nextInt(5) - 2.0)
                Allies.spawn(Kit.entityType("CREEPER"), at, p, 30, ChatColor.GREEN.toString() + "Creeper Army", 40.0)
            }
            say(p, "A creeper army fights for you!")
            true
        }
        uses["scroll_heal"] = Use { p, _, _, _ ->
            p.health = minOf(p.maxHealth, p.health + 10.0)
            say(p, "You healed yourself!")
            true
        }
        uses["scroll_launch"] = Use { p, _, _, _ ->
            val t = Kit.aimedEnemy(p, 25.0, 0.9) ?: return@Use fail(p, "Aim at an enemy within 25 blocks!")
            t.velocity = Kit.direction(p).setY(0.0).normalize().multiply(1.6).setY(1.3)
            t.sendMessage(ChatColor.RED.toString() + p.name + " launched you!")
            true
        }
        uses["scroll_poison"] = Use { p, _, _, _ ->
            val t = Kit.aimedEnemy(p, 25.0, 0.9) ?: return@Use fail(p, "Aim at an enemy within 25 blocks!")
            Kit.effect(t, 8, 1, "POISON")
            Kit.effect(t, 8, 0, "SLOW", "SLOWNESS")
            t.sendMessage(ChatColor.DARK_GREEN.toString() + p.name + " poisoned you!")
            true
        }
        uses["scroll_smite"] = Use { p, _, _, _ ->
            val t = Kit.aimedEnemy(p, 35.0, 0.9) ?: Kit.nearestEnemy(p, 30.0) ?: return@Use fail(p, "There is no enemy to smite.")
            t.world.strikeLightningEffect(t.location)
            t.damage(10.0, p)
            t.sendMessage(ChatColor.YELLOW.toString() + p.name + " smote you!")
            true
        }
        uses["scroll_tnt"] = Use { p, _, _, _ ->
            val block = Kit.targetBlock(p, 40) ?: return@Use fail(p, "Look at a block to fountain TNT from.")
            val at = block.location.add(0.5, 1.0, 0.5)
            for (i in 0 until 6) {
                val tnt = at.world.spawn(at, TNTPrimed::class.java)
                tnt.fuseTicks = 40 + Kit.random.nextInt(15)
                tnt.velocity = Vector(Kit.random.nextDouble() - 0.5, 0.6 + Kit.random.nextDouble() * 0.3, Kit.random.nextDouble() - 0.5)
            }
            true
        }

        uses["stasis"] = Use { p, _, _, _ ->
            val targets = Kit.enemies(p).filter { it.location.distanceSquared(p.location) <= 100.0 }
            if (targets.isEmpty()) return@Use fail(p, "No enemy within 10 blocks.")
            for (t in targets) {
                Live.frozen[t.uniqueId] = Live.now() + 5000L
                t.sendMessage(ChatColor.AQUA.toString() + "You are frozen in time for 5 seconds!")
            }
            say(p, "Froze ${targets.size} enemy(s) for 5 seconds!")
            true
        }

        uses["instant_barrier"] = Use { p, arena, _, _ ->
            val look = Kit.direction(p)
            val alongX = abs(look.x) >= abs(look.z)
            val sx = if (alongX) (if (look.x >= 0) 1 else -1) else 0
            val sz = if (alongX) 0 else (if (look.z >= 0) 1 else -1)
            val center = p.location.block
            var placed = 0
            for (w in -2..2) for (h in 0..2) {
                val b = center.getRelative(sx * 2 + (if (alongX) 0 else w), h, sz * 2 + (if (alongX) w else 0))
                if (b.type == Material.AIR && Kit.place(b, Kit.mat("OBSIDIAN"), arena)) placed++
            }
            if (placed == 0) return@Use fail(p, "There is no room for a wall here.")
            say(p, "An obsidian wall rises in front of you!")
            true
        }

        uses["teleport_beam"] = Use { p, arena, _, _ ->
            if (arena?.getTeam(p)?.spawn == null) return@Use fail(p, "You have no base to teleport to.")
            if (Live.beams.any { it.player == p.uniqueId }) return@Use fail(p, "You are already teleporting.")
            Live.beams.add(Live.Beam(p.uniqueId, p.location, Live.now() + 3000L))
            say(p, "Teleporting to your base in 3 seconds - don't move!")
            true
        }

        uses["gravity_gun"] = Use { p, _, _, _ ->
            val t = Kit.aimedEnemy(p, 20.0, 0.9) ?: return@Use fail(p, "Aim at an enemy within 20 blocks!")
            t.velocity = Kit.direction(p).multiply(2.4).setY(0.9)
            t.sendMessage(ChatColor.RED.toString() + p.name + " flung you with the Gravity Gun!")
            true
        }

        uses["dreadlord_skull"] = Use { p, _, _, _ ->
            val skull = p.launchProjectile(WitherSkull::class.java)
            skull.velocity = Kit.direction(p).multiply(1.6)
            true
        }

        uses["jedi_force"] = Use { p, _, _, _ ->
            val look = Kit.direction(p)
            var hit = 0
            for (t in Kit.enemies(p)) {
                val to = t.location.toVector().subtract(p.location.toVector())
                if (to.length() > 12.0 || to.lengthSquared() < 0.01) continue
                if (to.clone().normalize().dot(look) < 0.7) continue
                t.velocity = look.clone().setY(0.0).normalize().multiply(2.3).setY(0.7)
                hit++
            }
            if (hit == 0) return@Use fail(p, "No enemy in front of you.")
            say(p, "The Force pushes $hit enemy(s) away!")
            true
        }

        uses["magic_toy_stick"] = Use { p, arena, _, _ ->
            val block = Kit.targetBlock(p, 30) ?: return@Use fail(p, "Look at something to zap.")
            val at = block.location.add(0.5, 0.5, 0.5)
            for (x in -2..2) for (y in -2..2) for (z in -2..2) {
                val b = at.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
                if (b.type != Material.AIR && arena != null && arena.isBlockPlaced(b) && !LuckyBlock.isLuckyBlock(b)) {
                    b.type = Material.AIR
                    arena.removePlacedBlock(b)
                }
            }
            Kit.blast(at, 3.5, 5.0, 1.0, p)
            true
        }

        uses["nuke"] = Use { p, arena, _, _ ->
            val block = Kit.targetBlock(p, 150) ?: return@Use fail(p, "Aim at a spot (a bed, perhaps) to call the nuke.")
            val target = block.location.add(0.5, 1.0, 0.5)
            val where = "${target.blockX}, ${target.blockY}, ${target.blockZ}"
            for (other in p.world.players) {
                if (other.location.distanceSquared(target) <= 40.0 * 40.0) {
                    other.sendMessage(ChatColor.DARK_RED.toString() + "NUKE INCOMING at $where in 5 seconds! RUN!")
                }
            }
            say(p, "Nuke launched at $where!")
            Kit.runInGame(arena, 100L) {
                val sky = target.clone().add(0.0, 28.0, 0.0)
                for (i in 0 until 28) {
                    val a = Math.PI * 2 * i / 28
                    val tnt = sky.world.spawn(sky.clone().add(Math.cos(a) * 5.0, 0.0, Math.sin(a) * 5.0), TNTPrimed::class.java)
                    tnt.fuseTicks = 45
                }
                val centre = sky.world.spawn(sky, TNTPrimed::class.java)
                centre.fuseTicks = 45
                Kit.runInGame(arena, 55L) { Kit.blast(target, 8.0, 14.0, 2.0, p) }
            }
            true
        }

        uses["void_maker"] = Use { p, arena, _, e ->
            val block = e.clickedBlock ?: return@Use fail(p, "Click on obsidian or bedrock.")
            val t = block.type.name
            if (t != "OBSIDIAN" && t != "BEDROCK") return@Use fail(p, "The Void Maker only works on obsidian and bedrock.")
            // only what players placed: the map's own obsidian and bedrock (and the floor of the world) stay
            val placed = try {
                arena != null && arena.isBlockPlaced(block)
            } catch (ex: RuntimeException) {
                false
            }
            if (!placed) return@Use fail(p, "That block is part of the map.")
            block.type = Material.AIR
            arena?.removePlacedBlock(block)
            true
        }

        uses["vampire_blood"] = Use { p, _, _, _ ->
            Live.vampire[p.uniqueId] = Live.now() + 20000L
            say(p, "Vampire Blood: your hits heal you for 20 seconds!")
            true
        }
    }

    private fun trap(type: String, color: GlassColor) {
        uses[type] = Use { p, _, _, e ->
            if (e.action != Action.RIGHT_CLICK_BLOCK || e.blockFace != BlockFace.UP) return@Use fail(p, "Click on top of the floor to set the trap.")
            val spot = e.clickedBlock!!.getRelative(BlockFace.UP)
            if (spot.type != Material.AIR) return@Use fail(p, "There is no room for a trap there.")
            if (!Kit.canBuild(spot, Kit.arenaOf(p))) return@Use fail(p, "You can't set a trap there.")
            if (Live.traps.containsKey(spot.location)) return@Use fail(p, "There is a trap there already.")
            Items.carpet(color).place(spot)
            Live.traps[spot.location] = Live.Trap(type, p.uniqueId)
            say(p, "Trap set. It springs when an enemy steps on it.")
            true
        }
    }

    /** What happens to the victim when they step on a trap. */
    fun springTrap(trap: Live.Trap, victim: Player) {
        val owner = org.bukkit.Bukkit.getPlayer(trap.owner)
        victim.sendMessage(ChatColor.RED.toString() + "You stepped on a trap!")
        when (trap.type) {
            "trap_arrow" -> for (i in 0 until 8) {
                val at = victim.location.add(Kit.random.nextDouble() * 3 - 1.5, 9.0, Kit.random.nextDouble() * 3 - 1.5)
                val arrow: Arrow = victim.world.spawnArrow(at, Vector(0.0, -1.0, 0.0), 1.5f, 1.0f)
                arrow.setBounce(false)
            }
            "trap_damage" -> if (owner != null) victim.damage(6.0, owner) else victim.damage(6.0)
            "trap_slow" -> Kit.effect(victim, 10, 2, "SLOW", "SLOWNESS")
            "trap_freeze" -> Live.frozen[victim.uniqueId] = Live.now() + 4000L
            "trap_poison" -> Kit.effect(victim, 8, 1, "POISON")
        }
    }
}
