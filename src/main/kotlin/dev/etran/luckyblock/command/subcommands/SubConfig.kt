package dev.etran.luckyblock.command.subcommands

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.drops.Drop
import dev.etran.luckyblock.drops.Rarity
import dev.etran.luckyblock.generator.GeneratorKind
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import java.util.Locale

/** Shows and changes config.yml settings from in-game. */
class SubConfig : SubCommand() {
    override val name = "config"
    override val usage = "config [setting] [value]"
    override val description = "Shows the settings, or changes one (saved to config.yml and applied right away)"

    private val texturePattern = Regex("rarities\\.[a-z]+\\.textures")

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            showAll(sender)
            return
        }
        val setting = ConfigChanges.normalize(args[0])
        if (args.size == 1) {
            show(sender, args[0])
            return
        }
        val problem: String?
        if (ConfigChanges.textureKey(args[0]).matches(texturePattern)) {
            if (args.size != 3 || !(args[1].equals("add", true) || args[1].equals("remove", true))) {
                sender.sendMessage(ChatColor.RED.toString() + "/bwlb config " + args[0] + " <add|remove> <color>")
                return
            }
            problem = ConfigChanges.editTextures(args[0], args[1].equals("add", true), args[2])
        } else if (setting.startsWith("loot.")) {
            val parts = setting.split(".")
            val rarity = if (parts.size == 3) Rarity.fromKey(parts[1]) else null
            if (rarity == null || args.size != 2) {
                sender.sendMessage(ChatColor.RED.toString() + "/bwlb config loot.<rarity>.<drop> <weight>")
                return
            }
            problem = ConfigChanges.setLoot(rarity, parts[2], args[1])
        } else if (setting.startsWith("rarity-chances.") || setting.startsWith("rarity.")) {
            val parts = setting.split(".")
            if (parts.size != 3 || args.size != 2) {
                sender.sendMessage(ChatColor.RED.toString() + "/bwlb config rarity.<team|diamond|emerald>.<rarity> <weight>")
                return
            }
            problem = ConfigChanges.setRarityChance(parts[1], parts[2], args[1])
        } else if (args.size != 2) {
            sendUsage(sender)
            return
        } else {
            problem = ConfigChanges.set(args[0], args[1])
        }
        if (problem != null) {
            sender.sendMessage(ChatColor.RED.toString() + problem)
            return
        }
        sender.sendMessage(ChatColor.GREEN.toString() + "Saved to config.yml. " + ChatColor.GRAY + "It applies to games that start from now on.")
        show(sender, args[0])
    }

    private fun showAll(sender: CommandSender) {
        val config = LuckyBlocks.getInstance().cachedConfig
        sender.sendMessage(ChatColor.GOLD.toString() + "Settings " + ChatColor.GRAY + "(change with /bwlb config <setting> <value>)")
        sender.sendMessage(ChatColor.YELLOW.toString() + " generator-limit" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.generatorLimit)
        sender.sendMessage(ChatColor.YELLOW.toString() + " do-not-spawn-if-team-dead" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.isDoNotSpawnIfTeamDead)
        for (kind in GeneratorKind.values()) {
            val settings = config.getKind(kind)
            sender.sendMessage(
                ChatColor.YELLOW.toString() + " " + kind.configKey + ChatColor.GRAY + ": enabled " + ChatColor.WHITE + settings.isEnabled +
                    ChatColor.GRAY + ", cooldown " + ChatColor.WHITE + settings.cooldown + "s"
            )
        }
        sender.sendMessage(ChatColor.YELLOW.toString() + " sync-upgrades" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.isSyncUpgrades)
        sender.sendMessage(ChatColor.YELLOW.toString() + " middle-upgrade" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.middleUpgrade + "%")
        sender.sendMessage(ChatColor.YELLOW.toString() + " hologram.enabled" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.hologram.isEnabled)
        for (rarity in Rarity.values()) {
            sender.sendMessage(
                ChatColor.YELLOW.toString() + " " + rarity.key + ".textures" + ChatColor.GRAY + ": " + ChatColor.WHITE +
                    config.getRarity(rarity).textures.joinToString(", ")
            )
        }
        sender.sendMessage(
            ChatColor.YELLOW.toString() + " groups" + ChatColor.GRAY + ": " + ChatColor.WHITE + config.getLuckyGroups().joinToString(", ") +
                ChatColor.GRAY + "  (change with /bwlb group)"
        )
        sender.sendMessage(ChatColor.YELLOW.toString() + " loot / rarity" + ChatColor.GRAY + ": see /bwlb loot and /bwlb rarity")
    }

    private fun show(sender: CommandSender, typed: String) {
        val config = LuckyBlocks.getInstance().cachedConfig
        val setting = ConfigChanges.normalize(typed)
        val parts = setting.split(".")
        val value: String? = when {
            setting == "generator-limit" -> config.generatorLimit.toString()
            setting == "do-not-spawn-if-team-dead" -> config.isDoNotSpawnIfTeamDead.toString()
            setting == "sync-upgrades" -> config.isSyncUpgrades.toString()
            setting == "middle-upgrade" -> config.middleUpgrade.toString() + "%"
            setting == "hologram.enabled" -> config.hologram.isEnabled.toString()
            ConfigChanges.textureKey(typed).matches(texturePattern) -> {
                val rarity = Rarity.fromKey(ConfigChanges.textureKey(typed).split(".")[1])
                if (rarity == null) null else config.getRarity(rarity).textures.joinToString(", ")
            }
            setting.startsWith("generators.") && parts.size == 3 && ConfigChanges.kindOf(parts[1]) != null -> {
                val settings = config.getKind(ConfigChanges.kindOf(parts[1])!!)
                when (parts[2]) {
                    "enabled" -> settings.isEnabled.toString()
                    "cooldown" -> settings.cooldown.toString() + " seconds"
                    else -> null
                }
            }
            setting.startsWith("loot.") && parts.size == 3 && Rarity.fromKey(parts[1]) != null -> {
                val rarity = Rarity.fromKey(parts[1])!!
                "weight " + config.loot.getWeight(rarity, parts[2]) + " (" +
                    String.format(Locale.ROOT, "%.1f%%", config.loot.chance(rarity, parts[2], Drop.ids())) + " of " + rarity.key + " drops)"
            }
            (setting.startsWith("rarity.") || setting.startsWith("rarity-chances.")) && parts.size == 3 &&
                ConfigChanges.kindOf(parts[1]) != null && Rarity.fromKey(parts[2]) != null -> {
                val kind = ConfigChanges.kindOf(parts[1])!!
                val rarity = Rarity.fromKey(parts[2])!!
                "weight " + config.getRarityWeight(kind, rarity) + " (" +
                    String.format(Locale.ROOT, "%.1f%%", config.getRarityChance(kind, rarity)) + ")"
            }
            else -> null
        }
        if (value == null) {
            sender.sendMessage(ChatColor.RED.toString() + "Unknown setting \"" + typed + "\". Run /bwlb config to see them.")
            return
        }
        sender.sendMessage(ChatColor.YELLOW.toString() + typed.lowercase(Locale.ROOT) + ChatColor.GRAY + ": " + ChatColor.WHITE + value)
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        if (args.size == 1) {
            val options = ArrayList(ConfigChanges.scalarSettings())
            for (rarity in Rarity.values()) options.add(rarity.key + ".textures")
            for (kind in GeneratorKind.values()) {
                for (rarity in Rarity.values()) options.add("rarity." + kind.configKey + "." + rarity.key)
            }
            for (rarity in Rarity.values()) {
                for (drop in Drop.ids()) options.add("loot." + rarity.key + "." + drop)
            }
            return filter(options, args[0])
        }
        val setting = ConfigChanges.normalize(args[0])
        val textures = ConfigChanges.textureKey(args[0]).matches(texturePattern)
        if (args.size == 2) {
            if (setting == "do-not-spawn-if-team-dead" || setting == "sync-upgrades" || setting.endsWith(".enabled")) {
                return filter(listOf("true", "false"), args[1])
            }
            if (textures) return filter(listOf("add", "remove"), args[1])
        }
        if (args.size == 3 && textures) {
            val rarity = Rarity.fromKey(ConfigChanges.textureKey(args[0]).split(".")[1])
            if (rarity != null) {
                val current = LuckyBlocks.getInstance().cachedConfig.getRarity(rarity).textures
                if (args[1].equals("remove", true)) return filter(current, args[2])
                val others = ArrayList(LuckyBlockTexture.allNames())
                others.removeAll(current.toSet())
                return filter(others, args[2])
            }
        }
        return super.tabComplete(sender, args)
    }
}
