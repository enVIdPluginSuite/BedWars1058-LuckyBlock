package dev.etran.luckyblock.generator

import java.util.Locale

/** Where a lucky block generator sits: on a team island, or at a diamond / emerald generator. */
enum class GeneratorKind(
    /** Key used under "generators:" in config.yml. */
    val configKey: String,
    /**
     * Value stored in the "team" column of data.yml for non-team generators
     * (team generators store the team name there, exactly like older versions did).
     */
    val token: String?
) {
    TEAM("team", null),
    DIAMOND("diamond", "@diamond"),
    EMERALD("emerald", "@emerald");

    companion object {
        /** data.yml "team" column -> kind. Anything that isn't a reserved token is a team name. */
        @JvmStatic
        fun fromTeamColumn(column: String?): GeneratorKind {
            if (column != null) {
                for (kind in values()) {
                    if (kind.token != null && kind.token.equals(column.trim(), ignoreCase = true)) return kind
                }
            }
            return TEAM
        }

        /** "diamond" / "emerald" typed in a command. Returns null for anything else (e.g. a team name). */
        @JvmStatic
        fun fromCommandWord(word: String?): GeneratorKind? {
            if (word == null) return null
            return when (word.lowercase(Locale.ROOT)) {
                "diamond" -> DIAMOND
                "emerald" -> EMERALD
                else -> null
            }
        }
    }
}
