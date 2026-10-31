package dev.etran.luckyblock;

import com.andrei1058.bedwars.api.BedWars;
import dev.etran.luckyblock.command.MainCommand;
import dev.etran.luckyblock.arena.LuckyBlockArena;
import dev.etran.luckyblock.data.ArenaOverrides;
import dev.etran.luckyblock.data.CachedConfig;
import dev.etran.luckyblock.data.Language;
import dev.etran.luckyblock.data.LoadData;
import dev.etran.luckyblock.data.YmlFile;
import dev.etran.luckyblock.drops.Drop;
import dev.etran.luckyblock.drops.DropBridgeEgg;
import dev.etran.luckyblock.drops.DropFireball;
import dev.etran.luckyblock.drops.DropIceBridge;
import dev.etran.luckyblock.drops.DropInventoryCopier;
import dev.etran.luckyblock.drops.DropLightningScroll;
import dev.etran.luckyblock.drops.DropPlayerLauncher;
import dev.etran.luckyblock.drops.DropRainbowWool;
import dev.etran.luckyblock.drops.DropSleepingDust;
import dev.etran.luckyblock.drops.DropTnt;
import dev.etran.luckyblock.drops.DropWool;
import dev.etran.luckyblock.listeners.AsyncPlayerChatListener;
import dev.etran.luckyblock.listeners.BlockBreakListener;
import dev.etran.luckyblock.listeners.BlockPlaceListener;
import dev.etran.luckyblock.listeners.GameStateChangeListener;
import dev.etran.luckyblock.listeners.PlayerInteractListener;
import dev.etran.luckyblock.listeners.ItemMergeListener;
import dev.etran.luckyblock.listeners.PlayerQuitListener;
import dev.etran.luckyblock.listeners.SleepDustListener;
import dev.etran.luckyblock.luckyblock.LuckyBlock;
import java.io.File;
import lombok.Generated;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckyBlocks extends JavaPlugin {
  private static LuckyBlocks instance;

  private BedWars bedWars;

  private YmlFile dataFile;
  
  private YmlFile texturesFile;
  
  private YmlFile langFile;

  private ArenaOverrides arenaOverrides;

  private CachedConfig cachedConfig;
  
  private Language language;
  
  @Generated
  public YmlFile getDataFile() {
    return this.dataFile;
  }
  
  @Generated
  public YmlFile getTexturesFile() {
    return this.texturesFile;
  }
  
  @Generated
  public YmlFile getLangFile() {
    return this.langFile;
  }
  
  public ArenaOverrides getArenaOverrides() {
    return this.arenaOverrides;
  }

  @Generated
  public CachedConfig getCachedConfig() {
    return this.cachedConfig;
  }
  
  @Generated
  public Language getLanguage() {
    return this.language;
  }
  
  @Generated
  public static LuckyBlocks getInstance() {
    return instance;
  }

  /** The official BedWars1058 API (registered by BedWars1058 as a Bukkit service). */
  public static BedWars getBedWars() {
    return instance.bedWars;
  }

  public void onEnable() {

	    instance = this;

	    RegisteredServiceProvider<BedWars> provider = getServer().getServicesManager().getRegistration(BedWars.class);
	    if (provider == null) {
	      getLogger().severe("BedWars1058 API service not found. Is BedWars1058 installed and enabled?");
	      getServer().getPluginManager().disablePlugin(this);
	      return;
	    }
	    this.bedWars = provider.getProvider();

	    migrateOldDataFolder();

	    getLogger().info("BedWars1058-LuckyBlock " + getDescription().getVersion() + " by etran (original plugin by PmzHero and NGT_YT)");

	    saveDefaultConfig();

	    this.dataFile = new YmlFile((Plugin)this, "data.yml");
	    this.texturesFile = new YmlFile((Plugin)this, "textures.yml");
	    this.langFile = new YmlFile((Plugin)this, "lang.yml");
	    this.arenaOverrides = new ArenaOverrides(new YmlFile((Plugin)this, "arena-settings.yml"));

	    LoadData.loadData(this.dataFile.getConfig(), getLogger());
	    LoadData.loadTextures(this.texturesFile.getConfig());
	    this.cachedConfig = LoadData.loadConfig(getConfig());
	    LoadData.validateConfig(this.cachedConfig);
	    this.language = LoadData.loadLang(this.langFile.getConfig());

	    if (!YamlConfiguration.loadConfiguration(new File(getDataFolder(), "config.yml")).contains("groups")) {
	      getLogger().info("Your config.yml is from an older version, so the built-in defaults are used for the new automatic generator options.");
	      getLogger().info("Delete config.yml and restart to get a fresh one that shows all of them.");
	    }
	    if (this.cachedConfig.getLuckyGroups().isEmpty())
	      getLogger().info("No lucky block groups are set in config.yml, so only manually added generators will run.");

	    MainCommand mainCommand = new MainCommand();
	    getCommand("luckyblock").setExecutor((CommandExecutor)mainCommand);
	    getCommand("luckyblock").setTabCompleter(mainCommand);

	    Drop.registerDrop("fireball", (Drop)new DropFireball(this.language));
	    Drop.registerDrop("tnt", (Drop)new DropTnt(this.language));
	    Drop.registerDrop("wool", (Drop)new DropWool(this.language));
	    Drop.registerDrop("inventory_copier", (Drop)new DropInventoryCopier(this.language));
	    Drop.registerDrop("lightning_scroll", (Drop)new DropLightningScroll(this.language));
	    Drop.registerDrop("player_launcher", (Drop)new DropPlayerLauncher(this.language));
	    Drop.registerDrop("rainbow_wool", (Drop)new DropRainbowWool(this.language));
	    Drop.registerDrop("bridge_egg", (Drop)new DropBridgeEgg(this.language));
	    Drop.registerDrop("ice_bridge", (Drop)new DropIceBridge(this.language));
	    Drop.registerDrop("sleeping_dust", (Drop)new DropSleepingDust(this.language));
	    LoadData.validateLoot(this.cachedConfig);

	    PluginManager manager = getServer().getPluginManager();

	    manager.registerEvents(new BlockPlaceListener(), this);
	    manager.registerEvents(new BlockBreakListener(), this);
	    manager.registerEvents(new GameStateChangeListener(), this);
	    manager.registerEvents(new ItemMergeListener(), this);
	    manager.registerEvents(new PlayerQuitListener(), this);
	    manager.registerEvents(new PlayerInteractListener(this.language), this);
	    manager.registerEvents(new AsyncPlayerChatListener(this.language), this);
	    manager.registerEvents(new SleepDustListener(), this);
	}
  
  /**
   * This plugin used to be called "LuckyBlocks". If its old folder is still there and the new one has no
   * config yet, copy the old files over so no settings or generators are lost.
   */
  private void migrateOldDataFolder() {
    File current = getDataFolder();
    File old = new File(current.getParentFile(), "LuckyBlocks");
    if (!old.isDirectory() || new File(current, "config.yml").exists())
      return;
    File[] files = old.listFiles();
    if (files == null)
      return;
    current.mkdirs();
    int copied = 0;
    for (File file : files) {
      if (!file.isFile() || !file.getName().endsWith(".yml"))
        continue;
      try {
        java.nio.file.Files.copy(file.toPath(), new File(current, file.getName()).toPath());
        copied++;
      } catch (java.io.IOException e) {
        getLogger().warning("Could not copy " + file.getName() + " from plugins/LuckyBlocks: " + e.getMessage());
      }
    }
    if (copied > 0)
      getLogger().info("Copied " + copied + " file(s) from plugins/LuckyBlocks, so your settings and generators carry over.");
  }

  public void onDisable() {
    LuckyBlockArena.stopAll();
    LuckyBlock.destroyAll();
  }
}
