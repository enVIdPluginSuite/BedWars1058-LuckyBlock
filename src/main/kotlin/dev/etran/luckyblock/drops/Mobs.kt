package dev.etran.luckyblock.drops

import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.luckyblock.LuckyBlocks
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.Wolf
import java.util.Random

/** The mob and explosive drops: wolves and raining TNT. */
internal object Mobs {

    /** A random living player of another team, or null. */
    fun randomEnemy(player: Player, arena: IArena?, random: Random): Player? {
        if (arena == null) return null
        val own = arena.getTeam(player)
        val enemies = arena.players.filter { it != player && !it.isDead && !arena.isSpectator(it) && arena.getTeam(it) !== own }
        return if (enemies.isEmpty()) null else enemies[random.nextInt(enemies.size)]
    }

    /** Removes the mobs again after a while so they never pile up. */
    private fun expire(mobs: List<Entity>, ticks: Long) {
        Bukkit.getScheduler().runTaskLater(LuckyBlocks.getInstance(), {
            for (mob in mobs) if (mob.isValid) mob.remove()
        }, ticks)
    }

    fun tntRain(at: Location, player: Player, arena: IArena?, random: Random): String? {
        val enemy = randomEnemy(player, arena, random) ?: return "There is no enemy to rain TNT on!"
        for (i in 0 until 6) {
            val spot = enemy.location.add((random.nextInt(9) - 4).toDouble(), 7.0, (random.nextInt(9) - 4).toDouble())
            val tnt = enemy.world.spawn(spot, TNTPrimed::class.java)
            tnt.fuseTicks = 50 + random.nextInt(20)
        }
        enemy.sendMessage(ChatColor.RED.toString() + "TNT is raining down on you!")
        return "TNT rains on ${enemy.name}!"
    }

    fun wolves(at: Location, player: Player, arena: IArena?, random: Random): String? {
        val list = ArrayList<Entity>()
        for (i in 0 until 3) {
            val spot = player.location.add((random.nextInt(3) - 1).toDouble(), 0.0, (random.nextInt(3) - 1).toDouble())
            val wolf = player.world.spawnEntity(spot, EntityType.WOLF) as Wolf
            wolf.isTamed = true
            wolf.owner = player
            wolf.setAdult()
            list.add(wolf)
        }
        expire(list, 20L * 90L)
        return "Three wolves came to fight for you!"
    }
}
