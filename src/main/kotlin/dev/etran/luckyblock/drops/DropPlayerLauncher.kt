package dev.etran.luckyblock.drops

import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class DropPlayerLauncher(private val language: Language) : Drop() {
    private val playerLauncher: ItemStack

    init {
        val stack = ItemStack(Material.PAPER)
        val meta = stack.itemMeta
        meta.displayName = language.playerLauncherName
        meta.lore = language.playerLauncherLore
        stack.itemMeta = meta
        playerLauncher = NBTUtils.applyNBTTags(stack, "playerLauncher")
    }

    override fun drop(location: Location, player: Player) {
        location.world.dropItem(location, playerLauncher)
        language.sendReceivedItemMessage(player, language.playerLauncherName, 1)
    }

    companion object {
        private val launchers: MutableSet<String> = HashSet()

        @JvmStatic
        fun addLauncher(launcher: Player) {
            launchers.add(launcher.name)
        }

        @JvmStatic
        fun removeLauncher(launcher: Player) {
            launchers.remove(launcher.name)
        }

        @JvmStatic
        fun isLauncher(player: Player): Boolean = launchers.contains(player.name)

        /**
         * A push that sends the target off the side of what they stand on: toward the nearest edge (a column
         * with no ground), or away from the launcher if there is no edge close by.
         */
        @JvmStatic
        fun pushOffSide(target: Player, launcher: Player): Vector {
            val origin = target.location
            var bestDistance = Int.MAX_VALUE
            var best: Vector? = null
            for (step in 0 until 16) {
                val angle = Math.PI * 2 * step / 16
                val direction = Vector(cos(angle), 0.0, sin(angle))
                for (distance in 1..MAX_EDGE) {
                    val x = origin.blockX + (direction.x * distance).roundToInt()
                    val z = origin.blockZ + (direction.z * distance).roundToInt()
                    if (!hasGround(origin, x, z)) {
                        if (distance < bestDistance) {
                            bestDistance = distance
                            best = direction
                        }
                        break
                    }
                }
            }
            val away = best ?: origin.toVector().subtract(launcher.location.toVector()).setY(0.0).let {
                if (it.lengthSquared() > 0.0) it.normalize() else origin.direction.setY(0.0).normalize()
            }
            return away.multiply(1.4).setY(0.5)
        }

        /** True if there is a solid block within a few blocks below the target's feet in this column. */
        private fun hasGround(origin: Location, x: Int, z: Int): Boolean {
            val world = origin.world ?: return true
            for (dy in 1..4) {
                if (world.getBlockAt(x, origin.blockY - dy, z).type.isSolid) return true
            }
            return false
        }

        private const val MAX_EDGE = 10
    }
}
