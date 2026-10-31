package dev.etran.luckyblock

import com.andrei1058.bedwars.api.BedWars
import dev.etran.luckyblock.arena.LoadedArena
import dev.etran.luckyblock.arena.LuckyBlockArena
import dev.etran.luckyblock.command.MainCommand
import dev.etran.luckyblock.data.ArenaOverrides
import dev.etran.luckyblock.data.CachedConfig
import dev.etran.luckyblock.data.GroupStore
import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.data.LoadData
import dev.etran.luckyblock.data.YmlFile
import dev.etran.luckyblock.drops.Drop
import dev.etran.luckyblock.drops.DropCatalog
import dev.etran.luckyblock.listeners.AsyncPlayerChatListener
import dev.etran.luckyblock.listeners.BlockBreakListener
import dev.etran.luckyblock.listeners.BlockPlaceListener
import dev.etran.luckyblock.listeners.GameStateChangeListener
import dev.etran.luckyblock.listeners.ItemMergeListener
import dev.etran.luckyblock.listeners.LuckyItemListener
import dev.etran.luckyblock.listeners.PlayerInteractListener
import dev.etran.luckyblock.listeners.PlayerQuitListener
import dev.etran.luckyblock.listeners.SleepDustListener
import dev.etran.luckyblock.luckyblock.LuckyBlock
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.logging.Level

class LuckyBlocks : JavaPlugin() {
    private var bedWars: BedWars? = null

    lateinit var dataFile: YmlFile
        private set
    lateinit var texturesFile: YmlFile
        private set
    lateinit var langFile: YmlFile
        private set
    lateinit var arenaOverrides: ArenaOverrides
        private set
    lateinit var groupStore: GroupStore
        private set
    lateinit var cachedConfig: CachedConfig
        private set
    lateinit var language: Language
        private set

    override fun onEnable() {
        instance = this

        val provider = server.servicesManager.getRegistration(BedWars::class.java)
        if (provider == null) {
            logger.severe("BedWars1058 API service not found. Is BedWars1058 installed and enabled?")
            server.pluginManager.disablePlugin(this)
            return
        }
        bedWars = provider.provider

        migrateOldDataFolder()

        logger.info("BedWars1058-LuckyBlock ${description.version} by etran (original plugin by PmzHero and NGT_YT)")

        saveDefaultConfig()

        dataFile = YmlFile(this, "data.yml")
        texturesFile = YmlFile(this, "textures.yml")
        langFile = YmlFile(this, "lang.yml")
        arenaOverrides = ArenaOverrides(YmlFile(this, "arena-settings.yml"))
        groupStore = GroupStore(YmlFile(this, "groups.yml"))

        applyAll()

        if (!YamlConfiguration.loadConfiguration(File(dataFolder, "config.yml")).contains("groups")) {
            logger.info("Your config.yml is from an older version, so the built-in defaults are used for the new automatic generator options.")
            logger.info("Delete config.yml and restart to get a fresh one that shows all of them.")
        }
        if (cachedConfig.getLuckyGroups().isEmpty()) {
            logger.info("No lucky block groups are set, so only manually added generators will run. Add one with /bwlb group add <group>.")
        }

        val mainCommand = MainCommand()
        val command = getCommand("bwlb")!!
        command.setExecutor(mainCommand)
        command.tabCompleter = mainCommand

        val manager = server.pluginManager
        manager.registerEvents(BlockPlaceListener(), this)
        manager.registerEvents(BlockBreakListener(), this)
        manager.registerEvents(GameStateChangeListener(), this)
        manager.registerEvents(ItemMergeListener(), this)
        manager.registerEvents(PlayerQuitListener(), this)
        manager.registerEvents(PlayerInteractListener(language), this)
        manager.registerEvents(AsyncPlayerChatListener(language), this)
        manager.registerEvents(SleepDustListener(), this)
        manager.registerEvents(LuckyItemListener(), this)
    }

    /** (Re)builds everything the plugin keeps in memory from its files. */
    private fun applyAll() {
        LoadedArena.clearAll()
        LoadData.loadData(dataFile.getConfig(), logger)
        cachedConfig = LoadData.loadConfig(config)
        cachedConfig.setExtraGroups(groupStore.getGroups())
        LuckyBlockTexture.clear()
        LoadData.loadTextures(texturesFile.getConfig())
        LoadData.validateConfig(cachedConfig)
        language = LoadData.loadLang(langFile.getConfig())
        Drop.clear()
        DropCatalog.registerAll(language)
        LoadData.validateLoot(cachedConfig)
    }

    /**
     * Re-reads every file (config.yml, textures.yml, lang.yml, data.yml, groups.yml, arena-settings.yml).
     * Games that are already running keep the generators they started with; the next game uses the new settings.
     * @return null on success, otherwise what went wrong
     */
    fun reloadAll(): String? {
        return try {
            reloadConfig()
            dataFile.reloadConfig()
            texturesFile.reloadConfig()
            langFile.reloadConfig()
            arenaOverrides.reload()
            groupStore.reload()
            applyAll()
            null
        } catch (e: RuntimeException) {
            logger.log(Level.SEVERE, "Reload failed", e)
            e.toString()
        }
    }

    /** Re-reads config.yml only (used after /bwlb config changes a value). */
    fun reloadSettings() {
        reloadConfig()
        cachedConfig = LoadData.loadConfig(config)
        cachedConfig.setExtraGroups(groupStore.getGroups())
        LuckyBlockTexture.clear()
        LoadData.loadTextures(texturesFile.getConfig())
        LoadData.validateConfig(cachedConfig)
        LoadData.validateLoot(cachedConfig)
    }

    /**
     * This plugin used to be called "LuckyBlocks". If its old folder is still there and the new one has no
     * config yet, copy the old files over so no settings or generators are lost.
     */
    private fun migrateOldDataFolder() {
        val current = dataFolder
        val old = File(current.parentFile, "LuckyBlocks")
        if (!old.isDirectory || File(current, "config.yml").exists()) return
        val files = old.listFiles() ?: return
        current.mkdirs()
        var copied = 0
        for (file in files) {
            if (!file.isFile || !file.name.endsWith(".yml")) continue
            try {
                Files.copy(file.toPath(), File(current, file.name).toPath())
                copied++
            } catch (e: IOException) {
                logger.warning("Could not copy ${file.name} from plugins/LuckyBlocks: ${e.message}")
            }
        }
        if (copied > 0) logger.info("Copied $copied file(s) from plugins/LuckyBlocks, so your settings and generators carry over.")
    }

    override fun onDisable() {
        LuckyBlockArena.stopAll()
        LuckyBlock.destroyAll()
    }

    companion object {
        private var instance: LuckyBlocks? = null

        @JvmStatic
        fun getInstance(): LuckyBlocks = instance!!

        /** The official BedWars1058 API (registered by BedWars1058 as a Bukkit service). */
        @JvmStatic
        fun getBedWars(): BedWars = instance!!.bedWars!!
    }
}
