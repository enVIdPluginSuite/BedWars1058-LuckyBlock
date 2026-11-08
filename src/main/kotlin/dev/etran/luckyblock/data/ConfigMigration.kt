package dev.etran.luckyblock.data

import dev.etran.luckyblock.LuckyBlocks
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Before v3.1.0 there were four rarities (common, uncommon, rare, legendary) and a different set of drops. A config.yml
 * from then is saved as config-before-3.1.yml and replaced by the new default, keeping the general settings
 * (groups, limits, cooldowns, hologram) so nothing has to be set up again. The loot tables and rarity chances are new.
 */
internal object ConfigMigration {

    private val scalars = listOf(
        "generator-limit", "do-not-spawn-if-team-dead", "sync-upgrades", "middle-upgrade",
        "generators.team.enabled", "generators.team.cooldown",
        "generators.diamond.enabled", "generators.diamond.cooldown",
        "generators.emerald.enabled", "generators.emerald.cooldown",
        "hologram.enabled", "hologram.height-team", "hologram.height-ore"
    )

    fun run(plugin: LuckyBlocks) {
        val file = File(plugin.dataFolder, "config.yml")
        if (!file.isFile) return
        val old = YamlConfiguration.loadConfiguration(file)
        if (!old.isSet("rarities.common")) return
        try {
            Files.copy(file.toPath(), File(plugin.dataFolder, "config-before-3.1.yml").toPath(), StandardCopyOption.REPLACE_EXISTING)
            Files.delete(file.toPath())
            plugin.saveDefaultConfig()
            val lines = ArrayList(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8))
            for (path in scalars) {
                val value = old.get(path) ?: continue
                ConfigEditor.setScalar(lines, path, value.toString())
            }
            val groups = old.getStringList("groups")
            if (groups.isNotEmpty()) ConfigEditor.setList(lines, "groups", groups)
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8)
            plugin.reloadConfig()
            plugin.logger.info("Your config.yml was from before v3.1.0 (four rarities). It was saved as config-before-3.1.yml and replaced by the new one with the 5 lucky block types; your groups, limits, cooldowns and hologram settings were kept.")
        } catch (e: IOException) {
            plugin.logger.warning("Could not update the old config.yml: " + e.message)
        }
    }
}
