package dev.etran.luckyblock.generator;

import java.util.Locale;

/** Where a lucky block generator sits: on a team island, or at a diamond / emerald generator. */
public enum GeneratorKind {
  TEAM("team", null),
  DIAMOND("diamond", "@diamond"),
  EMERALD("emerald", "@emerald");

  private final String configKey;

  private final String token;

  GeneratorKind(String configKey, String token) {
    this.configKey = configKey;
    this.token = token;
  }

  /** Key used under "generators:" in config.yml. */
  public String getConfigKey() {
    return this.configKey;
  }

  /**
   * Value stored in the "team" column of data.yml for non-team generators
   * (team generators store the team name there, exactly like older versions did).
   */
  public String getToken() {
    return this.token;
  }

  /** data.yml "team" column -> kind. Anything that isn't a reserved token is a team name. */
  public static GeneratorKind fromTeamColumn(String column) {
    if (column != null)
      for (GeneratorKind kind : values()) {
        if (kind.token != null && kind.token.equalsIgnoreCase(column.trim()))
          return kind;
      }
    return TEAM;
  }

  /** "diamond" / "emerald" typed in a command. Returns null for anything else (e.g. a team name). */
  public static GeneratorKind fromCommandWord(String word) {
    if (word == null)
      return null;
    String lower = word.toLowerCase(Locale.ROOT);
    if (lower.equals("diamond"))
      return DIAMOND;
    if (lower.equals("emerald"))
      return EMERALD;
    return null;
  }
}
