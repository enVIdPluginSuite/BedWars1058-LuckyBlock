package dev.etran.luckyblock.generator;

/**
 * A lucky block generator definition (not a running one).
 *
 * Manual generators come from data.yml and have an id >= 0.
 * Automatic generators are detected from the BedWars1058 arena file and have a negative id.
 */
public class LoadedGenerator {
  private final String arenaName;

  private final GeneratorKind kind;

  /** Team name, only set when kind == TEAM. */
  private final String team;

  private final int id;

  private final double x;

  private final double y;

  private final double z;

  private final int cooldown;

  public LoadedGenerator(String arenaName, GeneratorKind kind, String team, int id, double x, double y, double z, int cooldown) {
    this.arenaName = arenaName;
    this.kind = kind;
    this.team = (kind == GeneratorKind.TEAM) ? team : null;
    this.id = id;
    this.x = x;
    this.y = y;
    this.z = z;
    this.cooldown = cooldown;
  }

  public String getArenaName() {
    return this.arenaName;
  }

  public GeneratorKind getKind() {
    return this.kind;
  }

  public String getTeam() {
    return this.team;
  }

  public int getId() {
    return this.id;
  }

  public double getX() {
    return this.x;
  }

  public double getY() {
    return this.y;
  }

  public double getZ() {
    return this.z;
  }

  public int getCooldown() {
    return this.cooldown;
  }

  public boolean isAuto() {
    return this.id < 0;
  }

  /** What the player types in commands: "3" for manual generator 3, "a2" for automatic generator 2. */
  public String getDisplayId() {
    return isAuto() ? ("a" + -this.id) : String.valueOf(this.id);
  }

  /** "TEAM Red", "DIAMOND", "EMERALD" */
  public String describe() {
    return (this.kind == GeneratorKind.TEAM) ? ("TEAM " + this.team) : this.kind.name();
  }

  /** The block this generator sits in, e.g. "12,64,-30". Used to spot duplicates and exclusions. */
  public String getBlockKey() {
    return blockKey(this.x, this.y, this.z);
  }

  public static String blockKey(double x, double y, double z) {
    return (int)Math.floor(x) + "," + (int)Math.floor(y) + "," + (int)Math.floor(z);
  }

  /** data.yml value: "x,y,z,cooldown,team" (team column holds @diamond / @emerald for those kinds). */
  public String serialize() {
    String column = (this.kind == GeneratorKind.TEAM) ? this.team : this.kind.getToken();
    return this.x + "," + this.y + "," + this.z + "," + this.cooldown + "," + column;
  }

  /**
   * Reads a data.yml entry.
   *
   * @throws IllegalArgumentException if the value is malformed
   */
  public static LoadedGenerator parse(String arenaName, String id, String raw) {
    if (raw == null)
      throw new IllegalArgumentException("empty value");
    String[] split = raw.split(",");
    if (split.length < 5)
      throw new IllegalArgumentException("expected \"x,y,z,cooldown,team\" but got \"" + raw + "\"");
    int cooldown = Integer.parseInt(split[3].trim());
    if (cooldown < 1)
      throw new IllegalArgumentException("cooldown must be at least 1 second");
    String column = split[4].trim();
    return new LoadedGenerator(
        arenaName,
        GeneratorKind.fromTeamColumn(column),
        column,
        Integer.parseInt(id.trim()),
        Double.parseDouble(split[0].trim()),
        Double.parseDouble(split[1].trim()),
        Double.parseDouble(split[2].trim()),
        cooldown);
  }
}
