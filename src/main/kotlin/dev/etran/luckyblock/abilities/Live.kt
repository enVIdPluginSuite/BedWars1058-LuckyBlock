package dev.etran.luckyblock.abilities

import com.andrei1058.bedwars.api.arena.team.ITeam
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.UUID

/** Everything the lucky block items keep track of while a game runs. All of it is forgotten when a game ends. */
internal object Live {
    fun now(): Long = System.currentTimeMillis()

    // per player timers (UUID -> time in ms when it ends)
    val invincible: MutableMap<UUID, Long> = HashMap()
    val vampire: MutableMap<UUID, Long> = HashMap()
    val frozen: MutableMap<UUID, Long> = HashMap()
    val justiceReady: MutableMap<UUID, Long> = HashMap()
    val gauntlet: MutableMap<UUID, Int> = HashMap()

    /** No fall damage until then (after a Blaze Rider ride ends in the air). */
    val noFall: MutableMap<UUID, Long> = HashMap()

    fun isInvincible(player: Player): Boolean = (invincible[player.uniqueId] ?: 0L) > now()
    fun isFrozen(player: Player): Boolean = (frozen[player.uniqueId] ?: 0L) > now()
    fun isVampire(player: Player): Boolean = (vampire[player.uniqueId] ?: 0L) > now()
    fun hasNoFall(player: Player): Boolean = (noFall[player.uniqueId] ?: 0L) > now()

    /**
     * The real armor a lucky hat / boots / pants replaced (player -> slot -> item). BedWars1058 locks the armor slots,
     * so it is kept here instead of in the inventory and put back when the lucky piece goes (death, disguise end).
     */
    val underArmor: MutableMap<UUID, MutableMap<String, ItemStack?>> = HashMap()

    /** The armor a disguised player wore before (helmet, chestplate, leggings, boots). */
    class Disguise(val armor: Array<ItemStack?>, val until: Long)
    val disguised: MutableMap<UUID, Disguise> = HashMap()

    /** A trap lying on the floor: waits for an enemy to step on it. */
    class Trap(val type: String, val owner: UUID)
    val traps: MutableMap<Location, Trap> = HashMap()

    /** A lit Hot Potato: who holds it and when it goes off. */
    class Potato(var holder: UUID, val explodeAt: Long, var lastCount: Long = -1L)
    val potatoes: MutableList<Potato> = ArrayList()

    /**
     * Thrown things that have a special effect when they land (projectile uuid -> kind). An entry goes when its
     * projectile lands; the size cap drops the oldest ones of projectiles that fell into the void and never landed.
     */
    val projectiles: MutableMap<UUID, String> = object : LinkedHashMap<UUID, String>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<UUID, String>?): Boolean = size > 500
    }

    /** A Time Warp Pearl in flight: where its thrower snaps back to. landedAt is set when the pearl hits something. */
    class Warp(val origin: Location, val arena: String?, var landedAt: Long = 0L)
    val warpBack: MutableMap<UUID, Warp> = HashMap()

    /** A lucky projectile item right-clicked this moment (player -> type and time), for when the last one leaves the hand. */
    class Throw(val type: String, val at: Long)
    val throwing: MutableMap<UUID, Throw> = HashMap()

    class Beam(val player: UUID, val start: Location, val until: Long)
    val beams: MutableList<Beam> = ArrayList()

    /** Entities that follow rules every tick (blaze rider, giant, explosive chicken). */
    class Rider(val blaze: Entity, val rider: UUID, val until: Long)
    val riders: MutableList<Rider> = ArrayList()
    class Giant(val entity: Entity, val until: Long, var lastHit: Long = 0L)
    val giants: MutableList<Giant> = ArrayList()
    class Chicken(val entity: Entity, val owner: UUID, val until: Long)
    val chickens: MutableList<Chicken> = ArrayList()

    /** Villager shops (Resource Trader, Jerry): villager uuid -> the villager, kind and end time. */
    class Trader(val entity: Entity, val kind: String, val until: Long, val owner: UUID)
    val traders: MutableMap<UUID, Trader> = HashMap()

    /** Players who have the companion menu open. */
    val companionMenu: MutableSet<UUID> = HashSet()

    /** Blocks put back to what they were (water, lava, fire...). */
    class Temp(val at: Location, val expect: Set<Material>, val restore: Material, val time: Long)
    val temps: MutableList<Temp> = ArrayList()

    /** Extra beds of a team (arena name + team name -> bed locations), and the team they belong to. */
    val backupBeds: MutableMap<String, MutableSet<Location>> = HashMap()
    val backupTeams: MutableMap<String, ITeam> = HashMap()

    /** Teams whose main bed was destroyed while they had a backup bed. */
    val mainBedGone: MutableSet<String> = HashSet()

    /** The backup beds of a team that still stand (beds blown up by TNT or removed by a Magic Toy Stick are dropped). */
    fun standingBackupBeds(key: String): Set<Location> {
        val spots = backupBeds[key] ?: return emptySet()
        spots.removeAll { it.world == null || !Kit.isBed(it.block.type) }
        return spots
    }

    fun tempBlock(at: Location, restore: Material, ms: Long, vararg expect: Material) {
        temps.add(Temp(at, expect.toSet(), restore, now() + ms))
    }

    fun restoreTemps(all: Boolean) {
        val it = temps.iterator()
        val now = now()
        while (it.hasNext()) {
            val t = it.next()
            if (all || t.time <= now) {
                if (t.at.world != null && t.expect.contains(t.at.block.type)) t.at.block.type = t.restore
                it.remove()
            }
        }
    }

    /** Forget what belongs to one world (a game ended there). Other games running at the same time are left alone. */
    fun clearWorld(world: World?) {
        if (world == null) return
        traps.keys.removeAll { it.world == world }
        warpBack.values.removeAll { it.origin.world == world }
        // projectiles: each entry goes when its projectile lands (ProjectileHitEvent), so none are left to forget here
        beams.removeAll { it.start.world == world }
        riders.removeAll { r -> (r.blaze.world == world).also { if (it) r.blaze.remove() } }
        giants.removeAll { g -> (g.entity.world == world).also { if (it) g.entity.remove() } }
        chickens.removeAll { c -> (c.entity.world == world).also { if (it) c.entity.remove() } }
        traders.values.removeAll { t -> (!t.entity.isValid || t.entity.world == world).also { if (it) t.entity.remove() } }
        temps.filter { it.at.world == world }.forEach { if (it.expect.contains(it.at.block.type)) it.at.block.type = it.restore }
        temps.removeAll { it.at.world == world }
        backupBeds.values.forEach { set -> set.removeAll { it.world == null || it.world == world } }
        backupBeds.values.removeAll { it.isEmpty() }
        backupTeams.keys.retainAll(backupBeds.keys)
        mainBedGone.retainAll(backupBeds.keys)
        val players = world.players.map { it.uniqueId }.toSet()
        for (map in listOf(invincible, vampire, frozen, justiceReady, noFall)) map.keys.removeAll(players)
        gauntlet.keys.removeAll(players)
        disguised.keys.removeAll(players)
        underArmor.keys.removeAll(players)
        throwing.keys.removeAll(players)
        potatoes.removeAll { players.contains(it.holder) }
        companionMenu.removeAll(players)
        // a Player Launcher not used before the game ended must not swallow the player's chat in the next game
        for (p in world.players) dev.etran.luckyblock.drops.DropPlayerLauncher.removeLauncher(p)
        Allies.clearWorld(world)
    }

    /** Forget one player (death or quit). */
    fun forgetPlayer(id: UUID, quit: Boolean) {
        vampire.remove(id); frozen.remove(id); gauntlet.remove(id); invincible.remove(id); noFall.remove(id)
        Uses.forgetUse(id)
        potatoes.removeAll { it.holder == id }
        warpBack.remove(id); throwing.remove(id)
        beams.removeAll { it.player == id }
        disguised.remove(id)
        // on death the lucky armor is taken off (and the real armor put back) by LuckyItemListener
        if (quit) {
            underArmor.remove(id)
            justiceReady.remove(id)
        }
    }

    fun clearAll() {
        restoreTemps(true)
        // plugin disabled mid-game: take disguises and lucky armor off again
        for (id in HashSet(disguised.keys)) Bukkit.getPlayer(id)?.let { Disguises.end(it) }
        for (id in HashSet(underArmor.keys)) Bukkit.getPlayer(id)?.let { Kit.takeOffLucky(it) }
        for (r in riders) r.blaze.remove()
        for (g in giants) g.entity.remove()
        for (c in chickens) c.entity.remove()
        for (t in traders.values) t.entity.remove()
        riders.clear(); giants.clear(); chickens.clear()
        traders.clear(); traps.clear(); warpBack.clear(); projectiles.clear(); throwing.clear(); beams.clear()
        invincible.clear(); vampire.clear(); frozen.clear(); justiceReady.clear(); gauntlet.clear(); noFall.clear()
        potatoes.clear(); companionMenu.clear(); backupBeds.clear(); backupTeams.clear(); mainBedGone.clear()
        disguised.clear(); underArmor.clear()
        Allies.clearAll()
    }
}
