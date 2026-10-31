package dev.etran.luckyblock.data

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class YmlFile(private val plugin: Plugin, private val fileName: String) {
    private var dataConfig: FileConfiguration? = null
    private var configFile: File? = null

    init {
        saveDefaultConfig()
    }

    fun reloadConfig() {
        val file = configFile ?: File(plugin.dataFolder, fileName).also { configFile = it }
        val config = YamlConfiguration.loadConfiguration(file)
        dataConfig = config
        val defaultStream = plugin.getResource(fileName)
        if (defaultStream != null) {
            config.setDefaults(YamlConfiguration.loadConfiguration(InputStreamReader(defaultStream)))
        }
    }

    fun getConfig(): FileConfiguration {
        if (dataConfig == null) reloadConfig()
        return dataConfig!!
    }

    fun saveConfig() {
        try {
            val file = configFile
            if (dataConfig == null || file == null) return
            getConfig().save(file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun saveDefaultConfig() {
        val file = configFile ?: File(plugin.dataFolder, fileName).also { configFile = it }
        if (!file.exists()) plugin.saveResource(fileName, false)
    }
}
