package dev.etran.luckyblock.compat;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Small helpers that let one jar run on Minecraft 1.8 up to the newest version BedWars1058 supports.
 * The plugin is compiled against the 1.8.8 API, so anything newer is reached by reflection here.
 */
public final class Compat {
  private Compat() {}

  /** True on 1.13+ ("flattened" materials such as WHITE_WOOL); false on 1.8 - 1.12 (WOOL + data value). */
  public static final boolean FLATTENED = Material.getMaterial("WHITE_WOOL") != null;

  private static final Method GET_HAND = findGetHand();

  private static Method findGetHand() {
    try {
      return PlayerInteractEvent.class.getMethod("getHand");
    } catch (ReflectiveOperationException | RuntimeException e) {
      return null; // 1.8: there is only one hand
    }
  }

  /** False only for the off-hand copy of an interact event (1.9+ fires the event once per hand). */
  public static boolean isMainHand(PlayerInteractEvent event) {
    if (GET_HAND == null)
      return true;
    try {
      Object hand = GET_HAND.invoke(event);
      return hand == null || "HAND".equals(((Enum<?>) hand).name());
    } catch (ReflectiveOperationException | RuntimeException e) {
      return true;
    }
  }

  /** Calls target.<method>(boolean) if the running server has it (e.g. setInvulnerable, 1.9+). */
  public static void setIfPresent(Object target, String method, boolean value) {
    try {
      target.getClass().getMethod(method, boolean.class).invoke(target, value);
    } catch (ReflectiveOperationException | RuntimeException ignored) {
      // not available on this version
    }
  }

  private static final Method GET_ENTITY = findGetEntity();

  private static Method findGetEntity() {
    try {
      return Bukkit.class.getMethod("getEntity", UUID.class);
    } catch (ReflectiveOperationException | RuntimeException e) {
      return null; // before 1.12
    }
  }

  /** Finds an entity by UUID (Bukkit.getEntity needs 1.12+, so older servers search the world). May return null. */
  public static Entity findEntity(World world, UUID id) {
    if (GET_ENTITY != null) {
      try {
        return (Entity) GET_ENTITY.invoke(null, id);
      } catch (ReflectiveOperationException | RuntimeException e) {
        return null;
      }
    }
    if (world == null)
      return null;
    for (Entity entity : world.getEntities()) {
      if (entity.getUniqueId().equals(id))
        return entity;
    }
    return null;
  }
}
