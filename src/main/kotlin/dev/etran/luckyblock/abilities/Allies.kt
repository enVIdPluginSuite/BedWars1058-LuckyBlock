package dev.etran.luckyblock.abilities

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Creature
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Mobs that fight for the player who got them (skeleton, snowman, wither...). Mobs choose their own targets, so
 * every second they are pointed at the nearest enemy of the owner's team, and they are never allowed to target
 * the owner's team mates. They disappear after a while.
 */
internal object Allies {
    class Ally(val entity: LivingEntity, val owner: UUID, val until: Long, val range: Double)

    private val allies: MutableMap<UUID, Ally> = HashMap()

    /** Spawns a mob for the player. Returns null if the mob type does not exist on this version. */
    fun spawn(type: EntityType?, at: Location, owner: Player, seconds: Int, name: String?, range: Double = 30.0): LivingEntity? {
        if (type == null) return null
        val world = at.world ?: return null
        val entity = world.spawnEntity(at, type) as? LivingEntity ?: return null
        if (name != null) {
            entity.customName = name
            entity.isCustomNameVisible = true
        }
        allies[entity.uniqueId] = Ally(entity, owner.uniqueId, Live.now() + seconds * 1000L, range)
        return entity
    }

    fun isAlly(entityId: UUID): Boolean = allies.containsKey(entityId)

    /** True if the mob must not attack this player (they are on the owner's team). */
    fun isFriend(entityId: UUID, player: Player): Boolean {
        val ally = allies[entityId] ?: return false
        val owner = org.bukkit.Bukkit.getPlayer(ally.owner) ?: return false
        return Kit.sameTeam(owner, player)
    }

    /** True if both mobs fight for players of the same team. */
    fun sameSide(a: UUID, b: UUID): Boolean {
        val one = allies[a] ?: return false
        val two = allies[b] ?: return false
        if (one.owner == two.owner) return true
        val p1 = org.bukkit.Bukkit.getPlayer(one.owner) ?: return false
        val p2 = org.bukkit.Bukkit.getPlayer(two.owner) ?: return false
        return Kit.sameTeam(p1, p2)
    }

    fun tick() {
        val now = Live.now()
        val it = allies.values.iterator()
        while (it.hasNext()) {
            val ally = it.next()
            val entity = ally.entity
            if (now >= ally.until || !entity.isValid) {
                if (entity.isValid) entity.remove()
                it.remove()
                continue
            }
            val owner = org.bukkit.Bukkit.getPlayer(ally.owner) ?: continue
            if (entity !is Creature) continue
            val target = entity.target
            if (target is Player && Kit.isPlaying(target) && !Kit.sameTeam(owner, target)) continue
            val enemy = Kit.enemies(owner)
                .filter { it.world == entity.world && it.location.distanceSquared(entity.location) <= ally.range * ally.range }
                .minByOrNull { it.location.distanceSquared(entity.location) }
            entity.target = enemy
        }
    }

    fun clearWorld(world: World) {
        val it = allies.values.iterator()
        while (it.hasNext()) {
            val a = it.next()
            if (a.entity.world == world) {
                a.entity.remove()
                it.remove()
            }
        }
    }

    fun clearAll() {
        for (a in allies.values) a.entity.remove()
        allies.clear()
    }
}
