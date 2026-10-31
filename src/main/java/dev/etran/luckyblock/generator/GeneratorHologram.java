package dev.etran.luckyblock.generator;

import dev.etran.luckyblock.compat.Compat;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

/** Floating text above a generator: a few lines, each an invisible name-tag armor stand. */
public final class GeneratorHologram {
  private static final double LINE_SPACING = 0.28D;

  private final List<ArmorStand> stands = new ArrayList<>();

  private final List<String> texts = new ArrayList<>();

  private final Location base;

  private final int lines;

  public GeneratorHologram(Location base, int lines) {
    this.base = base.clone();
    this.lines = lines;
  }

  /** Sets the text of every line (index 0 is the top line). Stands are created on first use and when they got lost. */
  public void setLines(List<String> text) {
    World world = this.base.getWorld();
    if (world == null)
      return;
    for (int i = 0; i < this.lines && i < text.size(); i++) {
      while (this.stands.size() <= i) {
        this.stands.add(null);
        this.texts.add(null);
      }
      ArmorStand stand = this.stands.get(i);
      if (stand == null || !stand.isValid()) {
        Location at = this.base.clone().add(0.0D, (this.lines - 1 - i) * LINE_SPACING, 0.0D);
        stand = (ArmorStand) world.spawnEntity(at, EntityType.ARMOR_STAND);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setMarker(true);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        Compat.setIfPresent(stand, "setInvulnerable", true);
        Compat.setIfPresent(stand, "setSilent", true);
        Compat.setIfPresent(stand, "setCollidable", false);
        Compat.setIfPresent(stand, "setPersistent", false);
        stand.setCustomNameVisible(true);
        this.stands.set(i, stand);
        this.texts.set(i, null);
      }
      String line = text.get(i);
      if (!line.equals(this.texts.get(i))) {
        stand.setCustomName(line);
        this.texts.set(i, line);
      }
    }
  }

  public void destroy() {
    for (ArmorStand stand : this.stands) {
      if (stand != null)
        stand.remove();
    }
    this.stands.clear();
    this.texts.clear();
  }
}
