package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.data.ConfigEditor
import dev.etran.luckyblock.drops.Drop
import dev.etran.luckyblock.drops.Rarity
import dev.etran.luckyblock.generator.GeneratorKind
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Locale

/**
 * Changes values of config.yml from in-game commands. The file is edited in place (comments stay), then
 * re-read, so the change is active at once for the games that start from now on.
 */
internal object ConfigChanges {

    /** Short names accepted next to the full path: "diamond.cooldown" means "generators.diamond.cooldown". */
    fun normalize(path: String): String {
        val lower = path.lowercase(Locale.ROOT)
        for (kind in GeneratorKind.values()) {
            if (lower.startsWith(kind.configKey + ".")) return "generators.$lower"
        }
        return lower
    }

    /** Every setting that holds a single value, in the short form. */
    fun scalarSettings(): List<String> {
        val list = ArrayList<String>()
        list.add("generator-limit")
        list.add("do-not-spawn-if-team-dead")
        list.add("sync-upgrades")
        list.add("middle-upgrade")
        list.add("hologram.enabled")
        for (kind in GeneratorKind.values()) {
            list.add(kind.configKey + ".enabled")
            list.add(kind.configKey + ".cooldown")
        }
        return list
    }

    /** The per-rarity color lists: "common.textures" ... */
    fun textureSettings(): List<String> = Rarity.values().map { "rarities.${it.key}.textures" }

    fun kindOf(name: String): GeneratorKind? = GeneratorKind.values().firstOrNull { it.configKey.equals(name, ignoreCase = true) }

    /** Accepts "rare.textures" as well as "rarities.rare.textures". */
    fun textureKey(typed: String): String {
        val lower = typed.lowercase(Locale.ROOT)
        if (lower.matches(Regex("(common|uncommon|rare|legendary)\\.textures"))) return "rarities.$lower"
        return lower
    }

    /** Sets a single value. Returns null on success, otherwise the problem. */
    fun set(typedPath: String, typedValue: String): String? {
        val path = normalize(typedPath)
        val value = typedValue.lowercase(Locale.ROOT)
        if (path == "generator-limit" || path.matches(Regex("generators\\.(team|diamond|emerald)\\.cooldown"))) {
            val number = parseInt(value)
            if (number == null || number < 1) return "$typedPath needs a whole number of 1 or more."
            return write(path, number.toString(), null, number)
        }
        if (path == "middle-upgrade") {
            val number = parseInt(value)
            if (number == null || number < 0 || number > 100) return "$typedPath needs a whole number from 0 to 100."
            return write(path, number.toString(), null, number)
        }
        if (path == "do-not-spawn-if-team-dead" || path == "sync-upgrades" || path == "hologram.enabled" ||
            path.matches(Regex("generators\\.(team|diamond|emerald)\\.enabled"))
        ) {
            if (value != "true" && value != "false") return "$typedPath needs true or false."
            return write(path, value, null, value.toBoolean())
        }
        if (textureKey(typedPath).matches(Regex("rarities\\.[a-z]+\\.textures"))) {
            return "$typedPath is a list: use \"$typedPath add <color>\" or \"$typedPath remove <color>\"."
        }
        return "Unknown setting \"$typedPath\". Run /bwlb config to see them."
    }

    /** Adds a lucky block color to (or removes it from) what a rarity uses. */
    fun editTextures(typedPath: String, add: Boolean, color: String): String? {
        val path = textureKey(typedPath)
        if (!path.matches(Regex("rarities\\.(common|uncommon|rare|legendary)\\.textures"))) {
            return "$typedPath is not a list of colors. Use common.textures, uncommon.textures, rare.textures or legendary.textures."
        }
        val name = color.lowercase(Locale.ROOT)
        val rarity = Rarity.fromKey(path.split(".")[1]) ?: return "Unknown rarity."
        val items = ArrayList(LuckyBlocks.getInstance().cachedConfig.getRarity(rarity).textures)
        if (add) {
            if (!LuckyBlockTexture.allNames().contains(name)) {
                return "There is no lucky block color \"$color\". They are: " + LuckyBlockTexture.allNames().joinToString(", ")
            }
            if (items.contains(name)) return "${rarity.key} already uses $name."
            items.add(name)
        } else {
            if (!items.remove(name)) return "${rarity.key} does not use $name."
            if (items.isEmpty()) return "That would leave ${rarity.key} with no colors. Add another one first."
        }
        return write(path, null, items, items)
    }

    /** Sets how likely a drop is within one rarity. */
    fun setLoot(rarity: Rarity, drop: String, weight: String): String? {
        val id = drop.lowercase(Locale.ROOT)
        if (!Drop.ids().contains(id)) return "There is no drop \"$drop\". They are: " + Drop.ids().joinToString(", ")
        val number = parseInt(weight)
        if (number == null || number < 0) return "The weight must be a whole number, 0 or more (0 = never)."
        return write("loot.${rarity.key}.$id", number.toString(), null, number)
    }

    /** Sets the weight of a rarity in the roll of one generator kind. */
    fun setRarityChance(kindName: String, rarityName: String, weight: String): String? {
        val kind = kindOf(kindName) ?: return "The generator must be team, diamond or emerald."
        val rarity = Rarity.fromKey(rarityName) ?: return "The rarity must be common, uncommon, rare or legendary."
        val number = parseInt(weight)
        if (number == null || number < 0) return "The weight must be a whole number, 0 or more."
        return write("rarity-chances.${kind.configKey}.${rarity.key}", number.toString(), null, number)
    }

    private fun parseInt(text: String): Int? = text.trim().toIntOrNull()

    private fun write(path: String, text: String?, list: List<String>?, value: Any): String? {
        val plugin = LuckyBlocks.getInstance()
        val file = File(plugin.dataFolder, "config.yml")
        try {
            val lines = ArrayList(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8))
            var edited: Boolean
            if (list != null) {
                edited = ConfigEditor.setList(lines, path, list)
            } else if (path.startsWith("rarity-chances.")) {
                val parts = path.split(".")
                edited = ConfigEditor.setFlowEntry(lines, "rarity-chances." + parts[1], parts[2], text!!)
            } else {
                edited = ConfigEditor.setScalar(lines, path, text!!)
                if (!edited && path.startsWith("loot.")) {
                    val dot = path.lastIndexOf('.')
                    edited = ConfigEditor.insertChild(lines, path.substring(0, dot), path.substring(dot + 1), text)
                }
            }
            if (edited) {
                Files.write(file.toPath(), lines, StandardCharsets.UTF_8)
            } else {
                // the key is not in the file (an older config.yml): let Bukkit write it, which drops the comments
                plugin.config.set(path, value)
                plugin.saveConfig()
            }
        } catch (e: IOException) {
            return "Could not write config.yml: " + e.message
        }
        plugin.reloadSettings()
        return null
    }
}
