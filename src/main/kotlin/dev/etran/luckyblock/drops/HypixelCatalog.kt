package dev.etran.luckyblock.drops

import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.luckyblock.abilities.Allies
import dev.etran.luckyblock.abilities.BwBridge
import dev.etran.luckyblock.abilities.Companions
import dev.etran.luckyblock.abilities.Disguises
import dev.etran.luckyblock.abilities.Kit
import dev.etran.luckyblock.abilities.Live
import dev.etran.luckyblock.abilities.Traders
import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.luckyblock.texture.GlassColor
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Horse
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Slime
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.util.Random

/**
 * The lucky block items of Hypixel Bed Wars (https://hypixel.net/threads/5832144/), by lucky block type:
 * Normal, Promising, Fortunate, Offensive and Miracle. What each does is in the lore of the item; the behavior lives
 * in the abilities package.
 */
internal object HypixelCatalog {

    private lateinit var lang: Language

    fun registerAll(language: Language) {
        lang = language
        normal()
        promising()
        fortunate()
        offensive()
        miracle()
    }

    // ------------------------------------------------------------------ helpers

    private fun item(id: String, label: String, maker: (Player, Random) -> Array<ItemStack>) {
        Drop.registerDrop(id, SimpleDrop(lang, label, SimpleDrop.Maker { p, r -> maker(p, r) }))
    }

    private fun act(id: String, fn: (Location, Player, IArena?, Random) -> String?) {
        Drop.registerDrop(id, ActionDrop(ActionDrop.Action { at, p, arena, r -> fn(at, p, arena, r) }))
    }

    private fun wear(id: String, label: String, slot: String, maker: (Random) -> ItemStack) {
        Drop.registerDrop(id, WearDrop(lang, label, slot, maker))
    }

    private fun one(item: ItemStack) = arrayOf(item)

    private fun red(s: String) = ChatColor.RED.toString() + s
    private fun gold(s: String) = ChatColor.GOLD.toString() + s
    private fun aqua(s: String) = ChatColor.AQUA.toString() + s
    private fun green(s: String) = ChatColor.GREEN.toString() + s
    private fun purple(s: String) = ChatColor.LIGHT_PURPLE.toString() + s
    private fun white(s: String) = ChatColor.WHITE.toString() + s

    private fun carpet(color: GlassColor) = Items.carpet(color).stack(1)

    /**
     * Runs 2 ticks later (the lucky block is still in the world while its break event is handled), and only if the
     * game is still running then.
     */
    private fun later(arena: IArena?, task: () -> Unit) = Kit.runInGame(arena, 2L, task)

    /** The trap item of a trap type again (for picking a placed trap back up). */
    fun trapStack(type: String): ItemStack? =
        trapTypes().firstOrNull { it.first == type }?.let { trapItem(type, it.second.first, it.second.second) }

    private fun trapItem(type: String, color: GlassColor, name: String) =
        Kit.mark(carpet(color), type, name, "Place it on the floor. It springs", "when an enemy steps on it.")

    private fun trapTypes() = listOf(
        "trap_arrow" to Pair(GlassColor.RED, red("Arrow Rain Trap")),
        "trap_damage" to Pair(GlassColor.ORANGE, gold("Damage Trap")),
        "trap_slow" to Pair(GlassColor.GRAY, ChatColor.GRAY.toString() + "Slow Trap"),
        "trap_freeze" to Pair(GlassColor.LIGHT_BLUE, aqua("Freeze Trap")),
        "trap_poison" to Pair(GlassColor.GREEN, green("Poison Trap"))
    )

    private fun scroll(type: String, name: String, vararg lore: String) = Kit.mark(ItemStack(Kit.mat("PAPER")), type, name, *lore)

    private fun witherSkull(): ItemStack {
        val modern = Material.getMaterial("WITHER_SKELETON_SKULL")
        return if (modern != null) ItemStack(modern) else ItemStack(Kit.mat("SKULL_ITEM"), 1, 1.toShort())
    }

    /** Next tool in the chain (wood, stone, iron, gold, diamond) than the best one the player has; null if already diamond. */
    private fun upgradeTool(p: Player, kind: String, label: String): String {
        val chain = listOf(
            listOf("WOOD_$kind", "WOODEN_$kind"), listOf("STONE_$kind"), listOf("IRON_$kind"),
            listOf("GOLD_$kind", "GOLDEN_$kind"), listOf("DIAMOND_$kind")
        )
        var best = -1
        var bestSlot = -1
        val contents = p.inventory.contents
        for (i in contents.indices) {
            val s = contents[i] ?: continue
            val tier = chain.indexOfFirst { names -> names.any { it == s.type.name } }
            if (tier > best) {
                best = tier
                bestSlot = i
            }
        }
        if (best >= chain.size - 1) return "You already have a diamond $label."
        if (bestSlot >= 0) p.inventory.setItem(bestSlot, null)
        val next = ItemStack(Kit.mat(*chain[best + 1].toTypedArray()))
        Kit.give(p, next)
        return "Your $label was upgraded to ${next.type.name.lowercase().replace('_', ' ')}!"
    }

    /** A slime of the Slimes drop. It does not split when it dies (the spot respawns it instead). */
    fun spawnSlime(at: Location): Entity? {
        val world = at.world ?: return null
        val s = world.spawnEntity(at, EntityType.SLIME) as Slime
        s.size = 2
        return s
    }

    private fun spawnAbove(at: Location, up: Double): Location = at.clone().add(0.5, up, 0.5)

    // ------------------------------------------------------------------- Normal

    private fun normal() {
        for ((type, pair) in trapTypes()) {
            if (type == "trap_freeze" || type == "trap_poison") continue
            item(type.replace("trap_", "") + "_trap", ChatColor.stripColor(pair.second)) { _, _ -> one(trapItem(type, pair.first, pair.second)) }
        }
        item("bed_pointer", "Bed Pointer") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("COMPASS")), "bed_pointer", gold("Bed Pointer"), "Right click: your compass points", "to the closest enemy bed."))
        }
        item("emerald_block", "Block of Emerald") { _, _ -> one(Kit.named(ItemStack(Kit.mat("EMERALD_BLOCK")), green("Block of Emerald"))) }
        act("gold_block") { at, _, arena, _ ->
            later(arena) { if (at.block.type == Material.AIR && Kit.canBuild(at.block, arena)) at.block.type = Kit.mat("GOLD_BLOCK") }
            "A solid Block of Gold appeared! It can't be picked up."
        }
        item("chicken_bomb", "Chicken Bomb") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("EGG")), "chicken_bomb", gold("Chicken Bomb"), "Right click: a chicken explodes", "after 2 seconds and lays eggs."))
        }
        wear("chicken_hat", "Chicken Hat", "HELMET") {
            Kit.wearable(Kit.leather("HELMET", Color.WHITE), "chicken_hat", white("Chicken Hat"), "Lays an egg every few seconds.", "Throw them to knock enemies back.")
        }
        wear("disco_helmet", "Disco Helmet", "HELMET") {
            Kit.wearable(Kit.leather("HELMET", Color.RED), "disco_helmet", purple("Disco Helmet"), "Changes color all the time.")
        }
        item("explosive_chicken", "Explosive Chicken") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("EGG")), "explosive_chicken", red("Explosive Chicken"), "Place it by your bed or generator.", "It explodes when an enemy gets close."))
        }
        item("fling_bow", "Fling Bow & Arrows") { _, _ ->
            arrayOf(
                Kit.mark(ItemStack(Kit.mat("BOW")), "fling_bow", gold("Fling Bow"), "Arrows fling enemies away."),
                ItemStack(Kit.mat("ARROW"), 3)
            )
        }
        item("flint_and_steel", "Flint and Steel") { _, _ -> one(ItemStack(Kit.mat("FLINT_AND_STEEL"))) }
        act("gauntlet_upgrade") { _, p, _, _ ->
            val level = (Live.gauntlet[p.uniqueId] ?: 0) + 1
            Live.gauntlet[p.uniqueId] = level
            "Gauntlet Upgrade! Your hits do +$level damage until you die."
        }
        wear("hot_head", "Hot Head", "HELMET") {
            Kit.wearable(Kit.leather("HELMET", Color.ORANGE), "hot_head", red("Hot Head"), "Sets you and enemies close to you on fire.")
        }
        item("hot_potato", "Hot Potato") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("BAKED_POTATO")), "hot_potato", red("Hot Potato"), "Right click: explodes in 6 seconds.", "Hit an enemy to pass it on!"))
        }
        act("instant_heal") { _, p, _, _ ->
            p.health = p.maxHealth
            p.foodLevel = 20
            "Instant Heal! You are at full health."
        }
        item("lava_rune", "Lava Rune") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("MAGMA_CREAM")), "lava_rune", red("Lava Rune"), "Right click a block to place lava."))
        }
        item("note_bomb", "Note Bomb") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("NOTE_BLOCK")), "note_bomb", purple("Note Bomb"), "Right click to throw it.", "It explodes after 2.5 seconds, no block damage."))
        }
        act("permanent_pickaxe") { _, p, arena, _ -> (if (arena != null) BwBridge.upgradeTool(p, arena, "pickaxe") else null) ?: upgradeTool(p, "PICKAXE", "pickaxe") }
        act("permanent_axe") { _, p, arena, _ -> (if (arena != null) BwBridge.upgradeTool(p, arena, "axe") else null) ?: upgradeTool(p, "AXE", "axe") }
        act("random_team_upgrade") { _, p, arena, r ->
            val team = arena?.getTeam(p)
            if (team != null) BwBridge.randomUpgrade(p, team, r)?.let { return@act it }
            val members = team?.members ?: listOf(p)
            when (r.nextInt(4)) {
                0 -> {
                    for (m in members) for (s in m.inventory.contents) if (s != null && s.type.name.endsWith("SWORD")) Kit.enchant(s, 1, "DAMAGE_ALL", "SHARPNESS")
                    "Random team upgrade: Sharpened Swords!"
                }
                1 -> {
                    for (m in members) for (s in m.inventory.armorContents) if (s != null && s.type != Material.AIR) Kit.enchant(s, 1, "PROTECTION_ENVIRONMENTAL", "PROTECTION")
                    "Random team upgrade: Reinforced Armor!"
                }
                2 -> {
                    for (m in members) Kit.effect(m, 90, 0, "FAST_DIGGING", "HASTE")
                    "Random team upgrade: Maniac Miner!"
                }
                else -> {
                    for (m in members) Kit.effect(m, 30, 0, "REGENERATION")
                    "Random team upgrade: Heal Pool!"
                }
            }
        }
        act("resource_trader") { _, p, _, _ -> if (Traders.spawn(p, "resource", 25)) null else null }
        act("skeleton") { at, p, _, _ ->
            Allies.spawn(Kit.entityType("SKELETON"), spawnAbove(at, 0.5), p, 90, green("Skeleton Companion"))
            "A skeleton fights for you for 90 seconds!"
        }
        item("sleepinator", "Sleepinator") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("ENDER_EYE", "EYE_OF_ENDER")), "sleepinator", purple("Sleepinator"), "Right click while looking at an enemy:", "they go blind and slow. Keep hitting!"))
        }
        wear("slime_boots", "Slime Boots", "BOOTS") {
            Kit.wearable(Kit.leather("BOOTS", Color.LIME), "slime_boots", green("Slime Boots"), "No fall damage - you bounce instead.")
        }
        act("slimes") { at, p, _, _ ->
            for (i in 0 until 3) {
                val where = spawnAbove(at, 0.5)
                Live.slimeSpots.add(Live.SlimeSpot(where, spawnSlime(where)))
            }
            "Slimes! They come back every time they die - lead them off the edge."
        }
        item("water_balloon", "Water Balloons") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("SNOWBALL"), 8), "water_balloon", aqua("Water Balloon"), "Throw it: water appears where it lands."))
        }
    }

    // ---------------------------------------------------------------- Promising

    private fun promising() {
        act("armor_spikes") { _, p, _, _ ->
            for (s in p.inventory.armorContents) if (s != null && s.type != Material.AIR) Kit.enchant(s, 2, "THORNS")
            "Armor Spikes! Your armor now has Thorns II."
        }
        act("blaze_rider") { at, p, _, _ ->
            val type = Kit.entityType("BLAZE")
            val blaze = if (type == null) null else at.world.spawnEntity(spawnAbove(at, 1.0), type) as? LivingEntity
            if (blaze == null) return@act null
            blaze.passenger = p
            Live.riders.add(Live.Rider(blaze, p.uniqueId, Live.now() + 40000L))
            "You ride a Blaze for 40 seconds - it flies where you look!"
        }
        act("companion_selection") { _, p, arena, _ ->
            later(arena) { if (Kit.isPlaying(p)) Companions.open(p) }
            null
        }
        wear("cute_pants", "Cute Pants", "LEGGINGS") {
            Kit.enchant(Kit.wearable(Kit.leather("LEGGINGS", Color.FUCHSIA), "cute_pants", purple("Cute Pants"), "Protection III."), 3, "PROTECTION_ENVIRONMENTAL", "PROTECTION")
        }
        act("disguise") { _, p, _, _ -> if (Disguises.start(p, 45)) null else "There is no enemy team to disguise as." }
        act("exodus") { _, p, _, _ ->
            val helmet = Kit.enchant(Kit.wearable(ItemStack(Kit.mat("CHAINMAIL_HELMET")), "exodus", white("Exodus Helmet")), 2, "PROTECTION_ENVIRONMENTAL", "PROTECTION")
            val chest = Kit.enchant(Kit.wearable(ItemStack(Kit.mat("CHAINMAIL_CHESTPLATE")), "exodus", white("Exodus Chestplate")), 2, "PROTECTION_ENVIRONMENTAL", "PROTECTION")
            Kit.equip(p, "HELMET", helmet)
            Kit.equip(p, "CHESTPLATE", chest)
            "Exodus! You wear a helmet and chestplate."
        }
        act("ghast") { at, p, _, _ ->
            val type = Kit.entityType("GHAST")
            if (type != null) {
                val ghast = at.world.spawnEntity(spawnAbove(at, 7.0), type)
                Kit.runLater(20L * 45L, Runnable { if (ghast.isValid) ghast.remove() })
            }
            "A Ghast appeared! Kill it quickly."
        }
        item("instant_barrier", "Instant Barrier") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("OBSIDIAN")), "instant_barrier", ChatColor.DARK_PURPLE.toString() + "Instant Barrier", "Right click: a 5x3 obsidian wall", "rises in front of you."))
        }
        item("scrolls", "Scrolls") { _, r ->
            val all = listOf(
                scroll("scroll_creeper", green("Creeper Army Scroll"), "Right click: creepers fight for you."),
                scroll("scroll_heal", red("Heal Self Scroll"), "Right click: heal 5 hearts."),
                scroll("scroll_launch", aqua("Launch Scroll"), "Aim at an enemy and right click."),
                scroll("scroll_poison", ChatColor.DARK_GREEN.toString() + "Poison Bolt Scroll", "Aim at an enemy and right click."),
                scroll("scroll_smite", ChatColor.YELLOW.toString() + "Smite Scroll", "Right click: lightning strikes an enemy."),
                scroll("scroll_tnt", red("TNT Fountain Scroll"), "Look at a block and right click.")
            )
            Array(3) { all[r.nextInt(all.size)] }
        }
        item("scythe", "Scythe") { _, _ ->
            one(Kit.enchant(Kit.mark(ItemStack(Kit.mat("IRON_SWORD")), "scythe", ChatColor.DARK_GRAY.toString() + "Scythe"), 2, "DAMAGE_ALL", "SHARPNESS"))
        }
        wear("squid_boots", "Squid Boots", "BOOTS") {
            Kit.wearable(Kit.leather("BOOTS", Color.BLUE), "squid_boots", aqua("Squid Boots"), "Crouch to cover the ground around you", "in water. Stand up to travel.")
        }
        item("stasis", "Stasis") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("CLOCK", "WATCH")), "stasis", aqua("Stasis"), "Right click: enemies within 10 blocks", "are frozen for 5 seconds."))
        }
        item("sugar_cookie", "Sugar Cookie") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("COOKIE")), "sugar_cookie", gold("Sugar Cookie"), "Eat it for a burst of speed."))
        }
        item("time_warp_pearl", "Time Warp Pearl") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("ENDER_PEARL")), "time_warp", purple("Time Warp Pearl"), "Throw it, then you snap back to", "where you stood after 5 seconds."))
        }
        item("vampire_blood", "Vampire Blood") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("REDSTONE")), "vampire_blood", red("Vampire Blood"), "Right click: your hits heal you", "for 20 seconds."))
        }
    }

    // --------------------------------------------------------------- Fortunate

    private fun fortunate() {
        act("bed_cover") { _, p, arena, _ ->
            val bed = arena?.getTeam(p)?.bed ?: return@act "You have no bed to cover."
            if (arena.getTeam(p)?.isBedDestroyed == true) return@act "Your bed is already gone."
            val stone = Kit.mat("END_STONE", "ENDER_STONE")
            val bedBlocks = ArrayList<org.bukkit.block.Block>()
            for (dx in -1..1) for (dz in -1..1) {
                val b = bed.block.getRelative(dx, 0, dz)
                if (Kit.isBed(b.type)) bedBlocks.add(b)
            }
            val faces = listOf(
                org.bukkit.block.BlockFace.UP, org.bukkit.block.BlockFace.DOWN, org.bukkit.block.BlockFace.NORTH,
                org.bukkit.block.BlockFace.SOUTH, org.bukkit.block.BlockFace.EAST, org.bukkit.block.BlockFace.WEST
            )
            var placed = 0
            var blocked = 0
            for (b in bedBlocks) for (f in faces) {
                val n = b.getRelative(f)
                if (n.type != Material.AIR) continue
                if (Kit.place(n, stone, arena)) placed++ else blocked++
            }
            "Bed Cover! $placed blocks of end stone now surround your bed." + (if (blocked > 0) " ($blocked spots were protected or taken.)" else "")
        }
        item("end_stone", "End Stone") { _, _ -> one(ItemStack(Kit.mat("END_STONE", "ENDER_STONE"), 12)) }
        wear("frog_helmet", "Frog Helmet", "HELMET") {
            Kit.wearable(Kit.leather("HELMET", Color.GREEN), "frog_helmet", green("Frog Helmet"), "Jump Boost and no fall damage.")
        }
        act("giant") { at, _, _, _ ->
            val type = Kit.entityType("GIANT") ?: return@act null
            val giant = at.world.spawnEntity(spawnAbove(at, 1.0), type) as LivingEntity
            giant.maxHealth = 100.0
            giant.health = 100.0
            giant.customName = red("Giant")
            giant.isCustomNameVisible = true
            Live.giants.add(Live.Giant(giant, Live.now() + 45000L))
            "A GIANT! Kill it quickly - it hits hard."
        }
        item("grappling_hook", "Grappling Hook") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("FISHING_ROD")), "grapple", gold("Grappling Hook"), "Cast it at a block to pull yourself there."))
        }
        item("gravity_gun", "Gravity Gun") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("QUARTZ"), 3), "gravity_gun", purple("Gravity Gun"), "Aim at an enemy and right click", "to fling them where you look."))
        }
        wear("heat_resistant_boots", "Heat-Resistant Boots", "BOOTS") {
            Kit.wearable(Kit.leather("BOOTS", Color.ORANGE), "heat_boots", gold("Heat-Resistant Boots"), "Fire and lava can't hurt you.")
        }
        item("mystery_meat", "Mystery Meat") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("COOKED_BEEF")), "mystery_meat", ChatColor.DARK_RED.toString() + "Mystery Meat", "Who knows what it does?"))
        }
        item("obsidian_4", "Obsidian") { _, _ -> one(ItemStack(Kit.mat("OBSIDIAN"), 4)) }
        act("roulette_blitz") { _, p, arena, r ->
            val enemy = Mobs.randomEnemy(p, arena, r) ?: return@act "There is no enemy for Roulette Blitz!"
            enemy.damage(1000.0, p)
            enemy.sendMessage(red("Roulette Blitz picked you!"))
            "Roulette Blitz killed ${enemy.name}!"
        }
        item("sharp_spoon", "Sharp Spoon") { _, _ ->
            one(Kit.enchant(Kit.mark(ItemStack(Kit.mat("WOOD_SPADE", "WOODEN_SHOVEL")), "sharp_spoon", white("Sharp Spoon")), 4, "DAMAGE_ALL", "SHARPNESS"))
        }
        item("teleport_beam", "Teleport Beam") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("PRISMARINE_CRYSTALS", "NETHER_STAR")), "teleport_beam", aqua("Teleport Beam"), "Right click, then stand still for", "3 seconds to teleport to your base."))
        }
        item("wildcard", "Wildcard") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("NAME_TAG")), "wildcard", gold("Wildcard"), "Keep it in your inventory. When you would", "die you are sent back to your base instead."))
        }
        item("freeze_trap", "Freeze Trap") { _, _ -> one(trapItem("trap_freeze", GlassColor.LIGHT_BLUE, aqua("Freeze Trap"))) }
    }

    // --------------------------------------------------------------- Offensive

    private fun offensive() {
        item("angel_of_death_sword", "Angel of Death's Sword") { _, _ ->
            one(Kit.enchant(Kit.mark(ItemStack(Kit.mat("DIAMOND_SWORD")), "angel_sword", gold("Angel of Death's Sword"), "One strike with huge damage and", "knockback, then it is gone."), 5, "DAMAGE_ALL", "SHARPNESS"))
        }
        act("apocalypse") { at, p, arena, _ ->
            val type = Kit.entityType("FIREBALL", "LARGE_FIREBALL") ?: return@act null
            val world = at.world
            for (pl in world.players) pl.sendMessage(ChatColor.DARK_RED.toString() + "APOCALYPSE! Fireballs rain from the sky - take cover!")
            object : BukkitRunnable() {
                var n = 0
                override fun run() {
                    // stops when the game ends, so no fireballs fall on a map that is being reset
                    if (n++ >= 20 || !Kit.gameRunning(arena)) {
                        cancel()
                        return
                    }
                    for (pl in world.players) {
                        if (!Kit.isPlaying(pl)) continue
                        val from = pl.location.add(Kit.random.nextInt(9) - 4.0, 14.0, Kit.random.nextInt(9) - 4.0)
                        val ball = world.spawnEntity(from, type) as? org.bukkit.entity.Fireball ?: continue
                        ball.direction = Vector(0.0, -1.0, 0.0)
                        ball.velocity = Vector(0.0, -1.0, 0.0)
                    }
                }
            }.runTaskTimer(dev.etran.luckyblock.LuckyBlocks.getInstance(), 10L, 16L)
            null
        }
        act("baby_zombie") { at, p, _, _ ->
            val z = Allies.spawn(Kit.entityType("ZOMBIE"), spawnAbove(at, 0.5), p, 90, green("Baby Zombie")) as? org.bukkit.entity.Zombie
            if (z != null) {
                z.isBaby = true
                try {
                    z.equipment.itemInHand = ItemStack(Kit.mat("DIAMOND_SWORD"))
                } catch (e: LinkageError) {
                    // the sword is only a bonus
                }
            }
            "A baby zombie with a diamond sword fights for you!"
        }
        item("chicken_bow", "Chicken Bow & Arrow") { _, _ ->
            arrayOf(
                Kit.mark(ItemStack(Kit.mat("BOW")), "chicken_bow", gold("Chicken Bow"), "Its arrow explodes in a cloud of feathers."),
                ItemStack(Kit.mat("ARROW"), 1)
            )
        }
        item("cobweb", "Cobwebs") { _, _ -> one(ItemStack(Kit.mat("COBWEB", "WEB"), 5)) }
        item("dreadlord_skull", "Dreadlord Skull") { _, _ ->
            one(Kit.mark(witherSkull(), "dreadlord_skull", ChatColor.DARK_GRAY.toString() + "Dreadlord Skull", "Right click: fires a wither skull."))
        }
        act("fire") { at, _, arena, _ ->
            later(arena) {
                val fire = Kit.mat("FIRE")
                for (dx in -1..1) for (dz in -1..1) {
                    val b = at.block.getRelative(dx, 0, dz)
                    // fire needs ground under it, and stays out of protected spots
                    if (b.type == Material.AIR && b.getRelative(0, -1, 0).type.isSolid && Kit.canBuild(b, arena)) {
                        b.type = fire
                        Live.tempBlock(b.location, Material.AIR, 8000L, fire)
                    }
                }
            }
            "Fire! Put it out quickly."
        }
        act("horse_rider") { at, p, _, _ ->
            val horse = at.world.spawnEntity(spawnAbove(at, 0.5), EntityType.HORSE) as Horse
            horse.isTamed = true
            horse.owner = p
            horse.inventory.saddle = ItemStack(Kit.mat("SADDLE"))
            try {
                horse.inventory.armor = ItemStack(Kit.mat("GOLD_BARDING", "GOLDEN_HORSE_ARMOR"))
            } catch (e: RuntimeException) {
                // horse armor is optional
            }
            horse.passenger = p
            Kit.runLater(20L * 100L, Runnable { if (horse.isValid) horse.remove() })
            Kit.give(p, Kit.enchant(Kit.mark(ItemStack(Kit.mat("GOLD_SWORD", "GOLDEN_SWORD")), "justice_gold", gold("Sword of Justice"), "Heals you when you hit (cooldown)."), 2, "DAMAGE_ALL", "SHARPNESS"))
            "You ride a horse with the Sword of Justice!"
        }
        act("instant_trap_queue") { _, p, arena, r ->
            val team = arena?.getTeam(p)
            val added = if (team != null) BwBridge.fillTraps(team, p, r) else null
            when {
                added == null -> {
                    // no BedWars1058 traps to fill: hand out floor traps instead
                    val types = trapTypes()
                    repeat(3) { val t = types[r.nextInt(types.size)]; Kit.give(p, trapItem(t.first, t.second.first, t.second.second)) }
                    "Instant Trap Queue! You got 3 traps to place."
                }
                added == 0 -> "Your trap queue is already full."
                else -> {
                    for (m in team!!.members) m.sendMessage(green("Instant Trap Queue! $added trap(s) were added to your team's trap queue."))
                    null
                }
            }
        }
        act("instant_weapon_upgrade") { _, p, arena, _ -> upgradeSword(p, arena?.getTeam(p)) }
        item("jedi_force", "Jedi Force") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("BLAZE_ROD")), "jedi_force", aqua("Jedi Force"), "Right click: pushes everyone in front", "of you away."))
        }
        item("knockback_slimeball", "Knockback Slimeball") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("SLIME_BALL")), "kb_slimeball", green("Knockback Slimeball"), "Hit an enemy once to throw them far."))
        }
        act("lava") { at, _, arena, _ ->
            later(arena) {
                val lava = Kit.lava()
                if (at.block.type == Material.AIR && Kit.canBuild(at.block, arena)) {
                    at.block.type = lava
                    Live.tempBlock(at, Material.AIR, 10000L, lava, Items.material("STATIONARY_LAVA", "LAVA"))
                }
            }
            "Lava! Back away or block it."
        }
        item("poison_trap", "Poison Trap") { _, _ -> one(trapItem("trap_poison", GlassColor.GREEN, green("Poison Trap"))) }
        item("rush_pearl", "Rush Pearl") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("ENDER_PEARL")), "rush_pearl", purple("Rush Pearl"), "Flies straight and fast, ignoring gravity."))
        }
        act("snowman") { at, p, _, _ ->
            Allies.spawn(Kit.entityType("SNOWMAN", "SNOW_GOLEM"), spawnAbove(at, 0.5), p, 90, white("Snowman"))
            "A snowman guards you for 90 seconds!"
        }
        item("sword_of_justice", "Sword of Justice") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("IRON_SWORD")), "justice_iron", white("Sword of Justice"), "Unlimited uses. Heals you when you hit (cooldown)."))
        }
    }

    private fun upgradeSword(p: Player, team: com.andrei1058.bedwars.api.arena.team.ITeam?): String {
        val chain = listOf(listOf("WOOD_SWORD", "WOODEN_SWORD"), listOf("STONE_SWORD"), listOf("IRON_SWORD"), listOf("DIAMOND_SWORD"))
        var best = -1
        var bestSlot = -1
        val contents = p.inventory.contents
        for (i in contents.indices) {
            val s = contents[i] ?: continue
            val tier = chain.indexOfFirst { names -> names.any { it == s.type.name } }
            if (tier > best) {
                best = tier
                bestSlot = i
            }
        }
        if (best >= chain.size - 1) return "You already have a diamond sword."
        if (bestSlot >= 0) p.inventory.setItem(bestSlot, null)
        val next = ItemStack(Kit.mat(*chain[best + 1].toTypedArray()))
        // the team's Sharpened Swords (and other sword upgrades) apply to it like to a bought sword
        try {
            for (e in team?.swordsEnchantments ?: emptyList()) next.addUnsafeEnchantment(e.enchantment, e.amplifier)
        } catch (e: RuntimeException) {
            // no team upgrades
        }
        Kit.give(p, next)
        return "Instant Weapon Upgrade! You got a ${next.type.name.lowercase().replace('_', ' ')}."
    }

    // ----------------------------------------------------------------- Miracle

    private fun miracle() {
        item("bedrock", "Bedrock") { _, _ -> one(Kit.named(ItemStack(Kit.mat("BEDROCK")), ChatColor.DARK_GRAY.toString() + "Bedrock", "Only a Void Maker can remove it.")) }
        act("incredible_chest") { at, _, arena, _ ->
            later(arena) {
                val block = at.block
                if (block.type != Material.AIR || !Kit.canBuild(block, arena)) {
                    // no room: the loot just drops
                    for (item in listOf("DIAMOND_SWORD", "DIAMOND_PICKAXE", "DIAMOND_AXE", "SHEARS")) at.world.dropItemNaturally(at, ItemStack(Kit.mat(item)))
                    return@later
                }
                block.type = Kit.mat("CHEST")
                (block.state as? Chest)?.let { chest ->
                    chest.inventory.addItem(
                        ItemStack(Kit.mat("DIAMOND_SWORD")), ItemStack(Kit.mat("DIAMOND_PICKAXE")),
                        ItemStack(Kit.mat("DIAMOND_AXE")), ItemStack(Kit.mat("SHEARS"))
                    )
                }
                try {
                    arena?.addPlacedBlock(block)
                } catch (e: RuntimeException) {
                    // not tracked
                }
            }
            "An Incredible Chest appeared - take a diamond sword or tool!"
        }
        act("jerry") { _, p, _, _ -> if (Traders.spawn(p, "jerry", 40)) null else null }
        item("magic_toy_stick", "Magic Toy Stick") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("STICK"), 4), "magic_toy_stick", purple("Magic Toy Stick"), "Right click on a spot: it blasts nearby", "player-placed blocks away. 4 uses."))
        }
        item("nuke_targeting_device", "Nuke Targeting Device") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("REDSTONE_TORCH_ON", "REDSTONE_TORCH")), "nuke", ChatColor.DARK_RED.toString() + "Nuke Targeting Device", "Aim at a far spot and right click.", "A nuke lands there after 5 seconds."))
        }
        item("obsidian_6", "Obsidian") { _, _ -> one(ItemStack(Kit.mat("OBSIDIAN"), 6)) }
        wear("op_chainmail_helmet", "OP Chainmail Helmet", "HELMET") {
            Kit.enchant(Kit.enchant(Kit.wearable(ItemStack(Kit.mat("CHAINMAIL_HELMET")), "op_helmet", gold("OP Chainmail Helmet")), 4, "PROTECTION_ENVIRONMENTAL", "PROTECTION"), 2, "THORNS")
        }
        wear("op_diamond_helmet", "OP Diamond Helmet", "HELMET") {
            Kit.enchant(Kit.enchant(Kit.wearable(ItemStack(Kit.mat("DIAMOND_HELMET")), "op_helmet", gold("OP Diamond Helmet")), 4, "PROTECTION_ENVIRONMENTAL", "PROTECTION"), 2, "THORNS")
        }
        wear("op_iron_helmet", "OP Iron Helmet", "HELMET") {
            Kit.enchant(Kit.wearable(ItemStack(Kit.mat("IRON_HELMET")), "op_helmet", gold("OP Iron Helmet")), 3, "PROTECTION_ENVIRONMENTAL", "PROTECTION")
        }
        item("placeable_bed", "Placeable Bed") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("RED_BED", "BED")), "placeable_bed", red("Placeable Bed"), "Hide it well: while it stands your team", "is not out when the main bed is broken."))
        }
        act("placeable_wither") { at, p, _, _ ->
            val w = Allies.spawn(Kit.entityType("WITHER"), spawnAbove(at, 1.0), p, 60, ChatColor.DARK_GRAY.toString() + "Wither", 40.0)
            if (w != null) Kit.effect(w, 60, 1, "DAMAGE_RESISTANCE", "RESISTANCE")
            "A Wither fights for you for 60 seconds!"
        }
        item("spicy_sword", "Spicy Sword") { _, _ ->
            one(Kit.enchant(Kit.enchant(Kit.mark(ItemStack(Kit.mat("DIAMOND_SWORD")), "spicy_sword", red("Spicy Sword")), 2, "FIRE_ASPECT"), 1, "DAMAGE_ALL", "SHARPNESS"))
        }
        act("super_star") { _, p, _, _ ->
            Live.invincible[p.uniqueId] = Live.now() + 15000L
            Kit.runLater(20L * 15L, Runnable { if (p.isOnline) p.sendMessage(ChatColor.YELLOW.toString() + "Your Super Star runs out.") })
            "SUPER STAR! You can't be hurt for 15 seconds."
        }
        item("void_maker", "Void Maker") { _, _ ->
            one(Kit.mark(ItemStack(Kit.mat("DIAMOND_PICKAXE")), "void_maker", ChatColor.DARK_PURPLE.toString() + "Void Maker", "Right click obsidian or bedrock to", "make it vanish."))
        }
    }
}
