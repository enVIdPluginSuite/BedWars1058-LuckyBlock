package dev.etran.luckyblock.luckyblock;

import dev.etran.luckyblock.compat.Compat;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

/**
 * The floating skull that is shown inside the stained glass of a lucky block.
 *
 * The old version sent raw NMS packets (1.8.8 only). This one uses a normal invisible
 * marker ArmorStand wearing the skull, which works on any modern version.
 */
public class LuckyBlockHead {

    private final Location location;
    private final ItemStack helmet;

    private ArmorStand stand;
    private boolean destroyed = false;

    public LuckyBlockHead(final Location location, LuckyBlockTexture skin) {
        this.location = location;
        this.helmet = skin.getBukkitItem().clone();
    }

    /** Spawns the head (does nothing if it is already spawned or was destroyed). */
    public void spawn() {

        if (destroyed || stand != null) return;

        World world = location.getWorld();
        if (world == null) return;

        ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);

        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setMarker(true);
        Compat.setIfPresent(armorStand, "setInvulnerable", true); // 1.9+
        Compat.setIfPresent(armorStand, "setSilent", true);       // 1.9+
        armorStand.setBasePlate(false);
        armorStand.setArms(false);
        Compat.setIfPresent(armorStand, "setCollidable", false);  // 1.9+
        Compat.setIfPresent(armorStand, "setPersistent", false);  // 1.16+
        armorStand.getEquipment().setHelmet(helmet);

        this.stand = armorStand;
    }

    public void destroy() {

        destroyed = true;

        if (stand != null) {
            stand.remove();
            stand = null;
        }
    }
}
