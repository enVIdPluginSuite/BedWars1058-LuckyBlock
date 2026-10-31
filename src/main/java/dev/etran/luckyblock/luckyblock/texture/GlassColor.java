package dev.etran.luckyblock.luckyblock.texture;

import lombok.Generated;
import org.bukkit.DyeColor;

public enum GlassColor {
  WHITE(0),
  ORANGE(1),
  MAGENTA(2),
  LIGHT_BLUE(3),
  YELLOW(4),
  LIME(5),
  PINK(6),
  GRAY(7),
  LIGHT_GRAY(8),
  CYAN(9),
  PURPLE(10),
  BLUE(11),
  BROWN(12),
  GREEN(13),
  RED(14),
  BLACK(15);

  @Generated
  GlassColor(int data) {
    this.data = data;
  }

  /** Legacy (pre-1.13) data value. Kept for reference only. */
  @Generated
  public int getData() {
    return this.data;
  }

  /** The matching color for a dye color (the old name SILVER is light gray). Unknown colors become white. */
  public static GlassColor fromDye(DyeColor dye) {
    String name = dye.name().equals("SILVER") ? "LIGHT_GRAY" : dye.name();
    for (GlassColor color : values()) {
      if (color.name().equals(name))
        return color;
    }
    return WHITE;
  }

  private final int data;
}
