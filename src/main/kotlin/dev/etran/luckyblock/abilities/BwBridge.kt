package dev.etran.luckyblock.abilities

import com.andrei1058.bedwars.api.arena.IArena
import com.andrei1058.bedwars.api.arena.team.ITeam
import com.andrei1058.bedwars.api.events.upgrades.UpgradeBuyEvent
import com.andrei1058.bedwars.api.upgrades.EnemyBaseEnterTrap
import com.andrei1058.bedwars.api.upgrades.MenuContent
import com.andrei1058.bedwars.api.upgrades.UpgradesIndex
import com.andrei1058.bedwars.api.upgrades.TeamUpgrade
import com.andrei1058.bedwars.api.upgrades.UpgradeAction
import dev.etran.luckyblock.LuckyBlocks
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.Random
import java.util.UUID

/**
 * Uses BedWars1058's own team traps, team upgrades and shop tiers, so a lucky item behaves exactly like buying it:
 * the same trap objects in `team.getActiveTraps()` (with the same queue limit and the same trigger when an enemy is
 * on the island), the same upgrade tiers and actions of upgrades2.yml, and the same shop tiers for pickaxe and axe.
 *
 * The traps, upgrades and shop tiers live in BedWars1058's own classes (not in its API), so they are reached through
 * reflection. Every function returns null when that is not possible (a different BedWars1058 version) so the caller
 * can fall back.
 */
internal object BwBridge {

    private const val MANAGER = "com.andrei1058.bedwars.upgrades.UpgradesManager"

    private fun upgradesYml(): FileConfiguration? = try {
        val config = LuckyBlocks.getBedWars().configs.upgradesConfig
        config.javaClass.getMethod("getYml").invoke(config) as? FileConfiguration
    } catch (e: ReflectiveOperationException) {
        null
    } catch (e: RuntimeException) {
        null
    } catch (e: LinkageError) {
        null
    }

    private fun content(name: String): Any? = try {
        Class.forName(MANAGER).getMethod("getMenuContent", String::class.java).invoke(null, name)
    } catch (e: ReflectiveOperationException) {
        null
    } catch (e: RuntimeException) {
        null
    } catch (e: LinkageError) {
        null
    }

    private fun setting(arena: IArena, path: String): Int {
        val yml = upgradesYml() ?: return 0
        val own = yml.getInt(arena.group.lowercase() + "-upgrades-settings." + path)
        return if (own != 0) own else yml.getInt("default-upgrades-settings.$path")
    }

    // ----------------------------------------------------------------- beds

    /**
     * Sets the team's "bed destroyed" flag without touching the bed block. BedWars1058's setBedDestroyed(false)
     * expects the bed to be there and logs a SEVERE error when it isn't (the main bed is gone, a backup bed stands),
     * so the field is set directly; the setter is only the fallback.
     */
    fun setBedFlag(team: ITeam, destroyed: Boolean) {
        var c: Class<*>? = team.javaClass
        while (c != null) {
            try {
                val field = c.getDeclaredField("bedDestroyed")
                field.isAccessible = true
                field.setBoolean(team, destroyed)
                return
            } catch (e: NoSuchFieldException) {
                c = c.superclass
            } catch (e: ReflectiveOperationException) {
                break
            } catch (e: RuntimeException) {
                break
            }
        }
        team.isBedDestroyed = destroyed
    }

    // ---------------------------------------------------------------- traps

    /** The base traps of upgrades2.yml (base-trap-*), as BedWars1058 loaded them. */
    fun baseTraps(): List<EnemyBaseEnterTrap> {
        val yml = upgradesYml() ?: return emptyList()
        return yml.getKeys(false).filter { it.startsWith("base-trap-") }.sorted().mapNotNull { content(it) as? EnemyBaseEnterTrap }
    }

    /** Adds the trap to the team's queue like buying it does. False if the queue is full. */
    fun queueTrap(team: ITeam, trap: EnemyBaseEnterTrap, player: Player): Boolean {
        val arena = team.arena
        var limit = setting(arena, "trap-queue-limit")
        if (limit <= 0) limit = 3
        if (team.activeTraps.size >= limit) return false
        // other plugins see it like a bought trap and may stop it
        if (trap is TeamUpgrade) {
            val event = UpgradeBuyEvent(trap, player, team)
            Bukkit.getPluginManager().callEvent(event)
            if (event.isCancelled) return false
        }
        team.activeTraps.add(trap)
        // like the shop: an enemy already standing on the island springs it at once
        for (other in arena.players) {
            if (team.isMember(other) || arena.isReSpawning(other)) continue
            if (other.world == team.bed.world && other.location.distance(team.bed) <= arena.islandRadius) {
                team.activeTraps.removeAt(0).trigger(team, other)
                break
            }
        }
        return true
    }

    /** Fills the team's trap queue with random base traps. Returns how many were added, or null without BedWars traps. */
    fun fillTraps(team: ITeam, player: Player, random: Random): Int? {
        val inMenu = menuNames(team.arena)
        val traps = baseTraps().filter { inMenu == null || (it as? MenuContent)?.name in inMenu || it !is MenuContent }
        if (traps.isEmpty()) return null
        var added = 0
        while (added < 10) {
            if (!queueTrap(team, traps[random.nextInt(traps.size)], player)) break
            added++
        }
        return added
    }

    // -------------------------------------------------------------- upgrades

    /**
     * Gives the team the next tier of a random team upgrade (sharpness, protection, haste, forge, heal pool...) for free,
     * the way MenuUpgrade does after the payment. Returns the text for the player, or null without BedWars upgrades.
     */
    fun randomUpgrade(player: Player, team: ITeam, random: Random): String? {
        val yml = upgradesYml() ?: return null
        // only upgrades this arena's group can buy (each group can have its own upgrade menu)
        val inMenu = menuNames(team.arena)
        val open = ArrayList<Pair<String, Any>>()
        for (key in yml.getKeys(false)) {
            if (!key.startsWith("upgrade-")) continue
            if (inMenu != null && key !in inMenu) continue
            val upgrade = content(key) ?: continue
            if (upgrade !is TeamUpgrade) continue
            val tiers = tiersOf(upgrade) ?: continue
            val bought = team.teamUpgradeTiers[key] ?: -1
            if (tiers.size - 1 > bought) open.add(key to upgrade)
        }
        if (open.isEmpty()) return "Your team already has every team upgrade."
        val (key, upgrade) = open[random.nextInt(open.size)]
        val tiers = tiersOf(upgrade)!!
        val next = (team.teamUpgradeTiers[key] ?: -1) + 1
        val event = UpgradeBuyEvent(upgrade as TeamUpgrade, player, team)
        Bukkit.getPluginManager().callEvent(event)
        if (event.isCancelled) return "Another plugin stopped the team upgrade."
        team.teamUpgradeTiers[key] = next
        try {
            val actions = tiers[next].javaClass.getMethod("getUpgradeActions").invoke(tiers[next]) as List<*>
            for (action in actions) (action as UpgradeAction).onBuy(player, team)
        } catch (e: ReflectiveOperationException) {
            return null
        }
        val tierName = try { tiers[next].javaClass.getMethod("getName").invoke(tiers[next]) as String } catch (e: ReflectiveOperationException) { "" }
        val nice = key.removePrefix("upgrade-").replace('-', ' ')
        for (m in team.members) if (m != player) m.sendMessage(org.bukkit.ChatColor.GOLD.toString() + "${player.name}'s lucky block gave your team $nice $tierName!")
        return "Team upgrade! Your team got $nice $tierName."
    }

    /**
     * Names of everything in the arena's upgrade menu, including what is inside its categories (the traps category),
     * or null if the menu can't be read (then nothing is filtered out).
     */
    private fun menuNames(arena: IArena): Set<String>? = try {
        val index = Class.forName(MANAGER).getMethod("getMenuForArena", IArena::class.java).invoke(null, arena) as UpgradesIndex
        val names = HashSet<String>()
        for (content in index.menuContentBySlot.values) {
            names.add(content.name)
            // a category lists its contents in upgrades2.yml as "name,slot"
            for (line in upgradesYml()?.getStringList(content.name + ".category-content") ?: emptyList()) {
                names.add(line.substringBefore(',').trim().lowercase())
            }
        }
        names
    } catch (e: ReflectiveOperationException) {
        null
    } catch (e: RuntimeException) {
        null
    } catch (e: LinkageError) {
        null
    }

    private fun tiersOf(upgrade: Any): List<Any>? = try {
        @Suppress("UNCHECKED_CAST")
        upgrade.javaClass.getMethod("getTiers").invoke(upgrade) as List<Any>
    } catch (e: ReflectiveOperationException) {
        null
    }

    // ----------------------------------------------------------------- tools

    /**
     * Moves the player one tier up in the shop's pickaxe or axe line, as buying the next tier would: the old tool is
     * replaced, the shop remembers the new tier and it comes back after death like a bought one.
     * [name] is "pickaxe" or "axe". Returns the text for the player, or null if the shop could not be reached.
     */
    fun upgradeTool(player: Player, arena: IArena, name: String): String? {
        try {
            val shop = Class.forName("com.andrei1058.bedwars.shop.ShopManager").getMethod("getShop").invoke(null) ?: return null
            val categories = shop.javaClass.getMethod("getCategoryList").invoke(shop) as List<*>
            var found: Any? = null
            for (category in categories) {
                for (cc in category!!.javaClass.getMethod("getCategoryContentList").invoke(category) as List<*>) {
                    val id = cc!!.javaClass.getMethod("getIdentifier").invoke(cc) as String
                    if (id.substringAfterLast('.').equals(name, true)) found = cc
                }
            }
            val cc = found ?: return null
            val cacheClass = Class.forName("com.andrei1058.bedwars.shop.ShopCache")
            val cache = cacheClass.getMethod("getShopCache", UUID::class.java).invoke(null, player.uniqueId)
                ?: cacheClass.getConstructor(UUID::class.java).newInstance(player.uniqueId)
            val id = cc.javaClass.getMethod("getIdentifier").invoke(cc) as String
            val tierCount = (cc.javaClass.getMethod("getContentTiers").invoke(cc) as List<*>).size
            val has = cacheClass.getMethod("hasCachedItem", cc.javaClass).invoke(cache, cc) as Boolean
            val tier = cacheClass.getMethod("getContentTier", String::class.java).invoke(cache, id) as Int
            if (has && tier >= tierCount) return "You already have the best $name."
            if (!has) {
                // first one: the item enters the shop's memory at tier 1
                val itemClass = Class.forName("com.andrei1058.bedwars.shop.ShopCache\$CachedItem")
                itemClass.getConstructor(cacheClass, cc.javaClass).newInstance(cache, cc)
            } else {
                val item = cacheClass.getMethod("getCachedItem", cc.javaClass).invoke(cache, cc)
                val field = item.javaClass.getDeclaredField("tier")
                field.isAccessible = true
                field.setInt(item, field.getInt(item) + 1)
                // the old tool goes, like ShopCache.CachedItem.upgrade does
                val support = LuckyBlocks.getBedWars().versionSupport
                for (stack: ItemStack? in player.inventory.contents.copyOf()) {
                    if (stack == null || stack.type == org.bukkit.Material.AIR) continue
                    if (support.getShopUpgradeIdentifier(stack) == id) player.inventory.remove(stack)
                }
            }
            cc.javaClass.getMethod("giveItems", Player::class.java, cacheClass, IArena::class.java).invoke(cc, player, cache, arena)
            player.updateInventory()
            val now = cacheClass.getMethod("getContentTier", String::class.java).invoke(cache, id) as Int
            return "Your $name is now tier $now of $tierCount!"
        } catch (e: ReflectiveOperationException) {
            LuckyBlocks.getInstance().logger.warning("Could not use the BedWars1058 shop for the $name upgrade: ${e.cause ?: e}")
            return null
        } catch (e: RuntimeException) {
            return null
        } catch (e: LinkageError) {
            return null
        }
    }
}
