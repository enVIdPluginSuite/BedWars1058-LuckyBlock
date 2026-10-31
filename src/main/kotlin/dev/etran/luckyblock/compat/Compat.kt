package dev.etran.luckyblock.compat

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.event.player.PlayerInteractEvent
import java.lang.reflect.Method
import java.util.UUID

/**
 * Small helpers that let one jar run on Minecraft 1.8 up to the newest version BedWars1058 supports.
 * The plugin is compiled against the 1.8.8 API, so anything newer is reached by reflection here.
 */
object Compat {
    /** True on 1.13+ ("flattened" materials such as WHITE_WOOL); false on 1.8 - 1.12 (WOOL + data value). */
    @JvmField
    val FLATTENED: Boolean = Material.getMaterial("WHITE_WOOL") != null

    private val getHand: Method? = try {
        PlayerInteractEvent::class.java.getMethod("getHand")
    } catch (e: ReflectiveOperationException) {
        null // 1.8: there is only one hand
    } catch (e: RuntimeException) {
        null
    }

    /** False only for the off-hand copy of an interact event (1.9+ fires the event once per hand). */
    @JvmStatic
    fun isMainHand(event: PlayerInteractEvent): Boolean {
        val method = getHand ?: return true
        return try {
            val hand = method.invoke(event)
            hand == null || (hand as Enum<*>).name == "HAND"
        } catch (e: ReflectiveOperationException) {
            true
        } catch (e: RuntimeException) {
            true
        }
    }

    /** Calls target.<method>(boolean) if the running server has it (e.g. setInvulnerable, 1.9+). */
    @JvmStatic
    fun setIfPresent(target: Any, method: String, value: Boolean) {
        try {
            target.javaClass.getMethod(method, java.lang.Boolean.TYPE).invoke(target, value)
        } catch (ignored: ReflectiveOperationException) {
            // not available on this version
        } catch (ignored: RuntimeException) {
        }
    }

    private val getEntity: Method? = try {
        Bukkit::class.java.getMethod("getEntity", UUID::class.java)
    } catch (e: ReflectiveOperationException) {
        null // before 1.12
    } catch (e: RuntimeException) {
        null
    }

    /** Finds an entity by UUID (Bukkit.getEntity needs 1.12+, so older servers search the world). May return null. */
    @JvmStatic
    fun findEntity(world: World?, id: UUID): Entity? {
        if (getEntity != null) {
            return try {
                getEntity.invoke(null, id) as Entity?
            } catch (e: ReflectiveOperationException) {
                null
            } catch (e: RuntimeException) {
                null
            }
        }
        if (world == null) return null
        return world.entities.firstOrNull { it.uniqueId == id }
    }
}
