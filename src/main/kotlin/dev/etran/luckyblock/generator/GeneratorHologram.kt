package dev.etran.luckyblock.generator

import dev.etran.luckyblock.compat.Compat
import org.bukkit.Location
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType

/** Floating text above a generator: a few lines, each an invisible name-tag armor stand. */
class GeneratorHologram(base: Location, private val lines: Int) {
    private val stands: MutableList<ArmorStand?> = ArrayList()
    private val texts: MutableList<String?> = ArrayList()
    private val base: Location = base.clone()

    /** Sets the text of every line (index 0 is the top line). Stands are created on first use and when they got lost. */
    fun setLines(text: List<String>) {
        val world = base.world ?: return
        var i = 0
        while (i < lines && i < text.size) {
            while (stands.size <= i) {
                stands.add(null)
                texts.add(null)
            }
            var stand = stands[i]
            if (stand == null || !stand.isValid) {
                val at = base.clone().add(0.0, (lines - 1 - i) * LINE_SPACING, 0.0)
                stand = world.spawnEntity(at, EntityType.ARMOR_STAND) as ArmorStand
                stand.isVisible = false
                stand.setGravity(false)
                stand.setMarker(true)
                stand.isSmall = true
                stand.setBasePlate(false)
                stand.setArms(false)
                Compat.setIfPresent(stand, "setInvulnerable", true)
                Compat.setIfPresent(stand, "setSilent", true)
                Compat.setIfPresent(stand, "setCollidable", false)
                Compat.setIfPresent(stand, "setPersistent", false)
                stand.isCustomNameVisible = true
                stands[i] = stand
                texts[i] = null
            }
            val line = text[i]
            if (line != texts[i]) {
                stand.customName = line
                texts[i] = line
            }
            i++
        }
    }

    fun destroy() {
        for (stand in stands) stand?.remove()
        stands.clear()
        texts.clear()
    }

    private companion object {
        const val LINE_SPACING = 0.28
    }
}
