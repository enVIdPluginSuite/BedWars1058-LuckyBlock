package dev.etran.luckyblock.luckyblock

import dev.etran.luckyblock.compat.Compat
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType

/**
 * The floating skull that is shown inside the stained glass of a lucky block.
 *
 * It is a normal invisible marker ArmorStand wearing the skull, which works on any version.
 */
class LuckyBlockHead(private val location: Location, skin: LuckyBlockTexture) {
    private val helmet = skin.bukkitItem.clone()
    private var stand: ArmorStand? = null
    private var destroyed = false

    /** Spawns the head (does nothing if it is already spawned or was destroyed). */
    fun spawn() {
        if (destroyed || stand != null) return
        val world = location.world ?: return

        val armorStand = world.spawnEntity(location, EntityType.ARMOR_STAND) as ArmorStand
        armorStand.isVisible = false
        armorStand.setGravity(false)
        armorStand.setMarker(true)
        Compat.setIfPresent(armorStand, "setInvulnerable", true) // 1.9+
        Compat.setIfPresent(armorStand, "setSilent", true)       // 1.9+
        armorStand.setBasePlate(false)
        armorStand.setArms(false)
        Compat.setIfPresent(armorStand, "setCollidable", false)  // 1.9+
        Compat.setIfPresent(armorStand, "setPersistent", false)  // 1.16+
        armorStand.equipment.helmet = helmet

        stand = armorStand
    }

    fun destroy() {
        destroyed = true
        stand?.remove()
        stand = null
    }
}
