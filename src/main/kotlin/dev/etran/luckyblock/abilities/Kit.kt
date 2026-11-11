package dev.etran.luckyblock.abilities

import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.drops.ArmorDrop
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector
import java.util.Random

/**
 * Helpers shared by all the lucky block items. Everything that differs between Minecraft versions (material,
 * enchantment, potion and entity names) is looked up by name, because the plugin is compiled against 1.8.8.
 *
 * Items carry ONE tag: key "lb" with the item's type as value (1.8 - 1.12 can only hold a single tag). Worn items
 * use the key "luckyArmor" instead, so they can be taken off when their wearer dies.
 */
internal object Kit {
    @JvmField
    val random = Random()

    const val USE_KEY = "lb"

    // ---- items ----

    fun mat(vararg names: String): Material = Items.material(*names)

    fun stack(amount: Int, vararg names: String): ItemStack = ItemStack(mat(*names), amount)

    fun named(item: ItemStack, name: String, vararg lore: String): ItemStack {
        val meta = item.itemMeta ?: return item
        meta.displayName = name
        if (lore.isNotEmpty()) meta.lore = lore.map { ChatColor.GRAY.toString() + it }
        item.itemMeta = meta
        return item
    }

    /** An item with a name, a lore and its type tag (the type decides what right clicking it does). */
    fun mark(item: ItemStack, type: String, name: String, vararg lore: String): ItemStack =
        NBTUtils.setValue(named(item, name, *lore), USE_KEY, type)

    fun typeOf(item: ItemStack?): String? = NBTUtils.getValue(item, USE_KEY)

    fun isType(item: ItemStack?, type: String): Boolean = typeOf(item) == type

    /** Armor or hat that is worn straight away and taken off again when its wearer dies. */
    fun wearable(item: ItemStack, type: String, name: String, vararg lore: String): ItemStack =
        NBTUtils.setValue(named(item, name, *lore), ArmorDrop.TAG, type)

    fun wornType(item: ItemStack?): String? = NBTUtils.getValue(item, ArmorDrop.TAG)

    fun leather(piece: String, color: Color): ItemStack {
        val item = ItemStack(mat("LEATHER_$piece"))
        val meta = item.itemMeta as LeatherArmorMeta
        meta.color = color
        item.itemMeta = meta
        return item
    }

    fun enchant(item: ItemStack, level: Int, vararg names: String): ItemStack {
        for (name in names) {
            val enchantment = Enchantment.getByName(name)
            if (enchantment != null) {
                item.addUnsafeEnchantment(enchantment, level)
                break
            }
        }
        return item
    }

    /** Takes one of the item out of the player's hand / inventory. */
    fun consume(player: Player, stack: ItemStack) {
        if (stack.amount > 1) {
            stack.amount = stack.amount - 1
        } else {
            player.inventory.removeItem(stack)
        }
    }

    /** Gives the items to the player; whatever does not fit is dropped at their feet. */
    fun give(player: Player, vararg items: ItemStack) {
        for (item in items) {
            val left = player.inventory.addItem(item)
            for (rest in left.values) player.world.dropItem(player.location, rest)
        }
    }

    // ---- potions and entities ----

    fun potionType(vararg names: String): PotionEffectType? {
        for (name in names) {
            val type = PotionEffectType.getByName(name)
            if (type != null) return type
        }
        return null
    }

    fun effect(target: LivingEntity, seconds: Int, amplifier: Int, vararg names: String) {
        val type = potionType(*names) ?: return
        target.addPotionEffect(PotionEffect(type, seconds * 20, amplifier), true)
    }

    fun entityType(vararg names: String): EntityType? {
        for (name in names) {
            try {
                return EntityType.valueOf(name)
            } catch (e: IllegalArgumentException) {
                // try the next name
            }
        }
        return null
    }

    // ---- teams ----

    fun arenaOf(player: Player): IArena? = try {
        LuckyBlocks.getBedWars().arenaUtil.getArenaByPlayer(player)
    } catch (e: RuntimeException) {
        null
    }

    /**
     * True if the player is really in the game right now: alive, not a spectator and not waiting to respawn.
     * (BedWars1058 spectators are not in spectator game mode on 1.8, so the game mode alone is not enough.)
     */
    fun isPlaying(player: Player): Boolean {
        if (!player.isOnline || player.isDead || player.gameMode.name == "SPECTATOR") return false
        val arena = arenaOf(player) ?: return false
        return try {
            !arena.isSpectator(player) && !arena.isReSpawning(player)
        } catch (e: RuntimeException) {
            true
        }
    }

    /** True if both are playing and on the same BedWars team. */
    fun sameTeam(a: Player, b: Player): Boolean {
        if (a == b) return true
        val arena = arenaOf(a) ?: return false
        val team = arena.getTeam(a) ?: return false
        return team === arena.getTeam(b)
    }

    /** Living players in the game that are not on the player's team (spectators excluded). */
    fun enemies(player: Player): List<Player> {
        val arena = arenaOf(player) ?: return emptyList()
        return arena.players.filter { it != player && it.world == player.world && isPlaying(it) && !sameTeam(player, it) }
    }

    /** The enemy closest to the point of view of the player (within range blocks and a narrow cone), or null. */
    fun aimedEnemy(player: Player, range: Double, minDot: Double = 0.92): Player? {
        val eye = player.eyeLocation
        val look = eye.direction.normalize()
        var best: Player? = null
        var bestDistance = Double.MAX_VALUE
        for (enemy in enemies(player)) {
            val to = enemy.eyeLocation.toVector().subtract(eye.toVector())
            val distance = to.length()
            if (distance > range || distance < 0.5) continue
            if (to.normalize().dot(look) < minDot) continue
            if (distance < bestDistance) {
                best = enemy
                bestDistance = distance
            }
        }
        return best
    }

    fun nearestEnemy(player: Player, range: Double): Player? =
        enemies(player).filter { it.location.distanceSquared(player.location) <= range * range }
            .minByOrNull { it.location.distanceSquared(player.location) }

    // ---- effects in the world ----

    /** Throws the entity away from the point. */
    fun push(entity: Entity, from: Location, power: Double, up: Double) {
        val away = entity.location.toVector().subtract(from.toVector()).setY(0)
        if (away.lengthSquared() < 1.0E-4) away.x = 0.01
        entity.velocity = away.normalize().multiply(power).setY(up)
    }

    /** An explosion that hurts and throws away living things but breaks no blocks. */
    fun blast(at: Location, radius: Double, damage: Double, power: Double, source: Player?) {
        val world = at.world ?: return
        try {
            world.createExplosion(at.x, at.y, at.z, 0.0f, false, false)
        } catch (e: RuntimeException) {
            // no visual
        }
        for (entity in world.getNearbyEntities(at, radius, radius, radius)) {
            if (entity !is LivingEntity || !canHit(entity)) continue
            if (entity is Player && Live.isInvincible(entity)) continue
            if (damage > 0) {
                if (source != null && entity !== source) entity.damage(damage, source) else entity.damage(damage)
            }
            push(entity, at, power, 0.5)
        }
    }

    /**
     * Things the lucky items may hurt and push: players in the game and mobs, but never armor stands (holograms of
     * generators and beds), BedWars1058 shop and upgrade villagers, the lucky traders or anyone not playing.
     */
    fun canHit(entity: Entity): Boolean {
        if (entity !is LivingEntity || entity.isDead || !entity.isValid) return false
        if (entity is Player) return isPlaying(entity)
        val type = entity.type.name
        if (type == "ARMOR_STAND" || type == "VILLAGER" || type == "WANDERING_TRADER") return false
        return !Live.traders.containsKey(entity.uniqueId)
    }

    /** A material that is a bed block on any version (and not bedrock, whose name also contains "BED"). */
    fun isBed(material: Material): Boolean {
        val n = material.name
        return n == "BED_BLOCK" || n == "BED" || n.endsWith("_BED")
    }

    /**
     * True if a player or a mob overlaps the block with any part of its hitbox (a block placed there would trap or
     * suffocate it, or let it slip through when it stands on the edge). Works on every version by using the
     * entity's width and height, and ignores armor stands, items and spectators like vanilla does.
     */
    fun occupied(block: Block): Boolean {
        val world = block.world
        val centre = block.location.add(0.5, 0.5, 0.5)
        val eps = 1.0E-4
        for (e in world.getNearbyEntities(centre, 2.0, 3.0, 2.0)) {
            if (e !is LivingEntity || e.type.name == "ARMOR_STAND" || e.isDead) continue
            if (e is Player && e.gameMode.name == "SPECTATOR") continue
            val loc = e.location
            val half = hitboxWidth(e) / 2.0
            val height = hitboxHeight(e)
            val overlapX = loc.x + half > block.x + eps && loc.x - half < block.x + 1 - eps
            val overlapZ = loc.z + half > block.z + eps && loc.z - half < block.z + 1 - eps
            val overlapY = loc.y + height > block.y + eps && loc.y < block.y + 1 - eps
            if (overlapX && overlapZ && overlapY) return true
        }
        return false
    }

    private fun hitboxWidth(e: LivingEntity): Double = when (e.type.name) {
        "GHAST" -> 4.0
        "GIANT" -> 3.6
        "SLIME", "MAGMA_CUBE" -> 0.51 * ((e as? org.bukkit.entity.Slime)?.size ?: 1)
        "BLAZE", "ZOMBIE", "SKELETON", "CREEPER", "VILLAGER", "IRON_GOLEM" -> if (e.type.name == "IRON_GOLEM") 1.4 else 0.6
        "CHICKEN" -> 0.4
        else -> 0.6
    }

    private fun hitboxHeight(e: LivingEntity): Double {
        if (e is Player) return if (e.isSneaking) 1.5 else 1.8
        return when (e.type.name) {
            "GHAST" -> 4.0
            "GIANT" -> 12.0
            "SLIME", "MAGMA_CUBE" -> 0.51 * ((e as? org.bukkit.entity.Slime)?.size ?: 1)
            "CHICKEN" -> 0.7
            "IRON_GOLEM" -> 2.7
            "BLAZE" -> 1.8
            else -> 1.95
        }
    }

    /**
     * True if lucky items may put a block there: inside the arena's build rules (not in a protected region, at a
     * spawn, shop, generator or past the border), and not where someone stands.
     */
    fun canBuild(block: Block, arena: IArena?): Boolean {
        if (block.y < 0 || block.y > block.world.maxHeight - 1) return false
        if (arena != null) {
            try {
                if (arena.isProtected(block.location)) return false
            } catch (e: LinkageError) {
                // older BedWars1058 without isProtected
            } catch (e: RuntimeException) {
                // not a game world
            }
        }
        return !occupied(block)
    }

    /**
     * Places a block and tells BedWars1058 it is a player-placed block, so players may break it. Returns false (and
     * places nothing) where players may not build or where someone stands.
     */
    fun place(block: Block, material: Material, arena: IArena?): Boolean {
        if (!canBuild(block, arena)) return false
        block.type = material
        try {
            arena?.addPlacedBlock(block)
        } catch (e: RuntimeException) {
            // not a game world
        }
        return true
    }

    /**
     * Sets a velocity one tick later. Inside a damage event the hit's own knockback is applied after the event and
     * would overwrite a velocity set there.
     */
    fun throwLater(entity: Entity, velocity: Vector) {
        runLater(1L, Runnable {
            // the victim may have died (and respawned at base) during that tick: never throw a respawned player
            if (!entity.isValid || entity.isDead) return@Runnable
            if (entity is Player && !isPlaying(entity)) return@Runnable
            entity.velocity = velocity
        })
    }

    fun runLater(ticks: Long, task: Runnable) {
        org.bukkit.Bukkit.getScheduler().runTaskLater(LuckyBlocks.getInstance(), task, ticks)
    }

    /** True while the arena's game is running (not ended, not restarting). */
    fun gameRunning(arena: IArena?): Boolean = arena != null && try {
        arena.status == com.andrei1058.bedwars.api.arena.GameState.playing
    } catch (e: RuntimeException) {
        false
    }

    /**
     * Runs the task later, but only if that game is still running then. A delayed effect (a bomb, a nuke, a block
     * placed a tick later) must never reach into a game that ended meanwhile or a map that is being reset.
     */
    fun runInGame(arena: IArena?, ticks: Long, task: () -> Unit) {
        runLater(ticks, Runnable { if (gameRunning(arena)) task() })
    }

    fun direction(player: Player): Vector = player.location.direction.normalize()

    /** The first solid block the player looks at within max blocks (own ray, because getTargetBlock differs between versions). */
    fun targetBlock(player: Player, max: Int): Block? {
        val eye = player.eyeLocation
        val step = eye.direction.normalize().multiply(0.4)
        val at = eye.clone()
        var travelled = 0.0
        while (travelled < max) {
            at.add(step)
            travelled += 0.4
            val block = at.block
            if (block.type != Material.AIR && block.type.isSolid) return block
        }
        return null
    }

    fun water(): Material = mat("WATER", "STATIONARY_WATER")
    fun lava(): Material = mat("LAVA", "STATIONARY_LAVA")
    fun air(): Material = Material.AIR

    fun count(player: Player, material: Material): Int =
        player.inventory.contents.filterNotNull().filter { it.type == material }.sumOf { it.amount }

    /** Takes that many of the material out of the inventory (the caller checked there are enough). */
    fun take(player: Player, material: Material, amount: Int) {
        var left = amount
        val contents = player.inventory.contents
        for (i in contents.indices) {
            val s = contents[i] ?: continue
            if (s.type != material || left <= 0) continue
            if (s.amount <= left) {
                left -= s.amount
                player.inventory.setItem(i, null)
            } else {
                s.amount = s.amount - left
                left = 0
            }
        }
    }

    val SLOTS = listOf("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")

    fun armorIn(player: Player, slot: String): ItemStack? {
        val inv = player.inventory
        val item = when (slot) {
            "HELMET" -> inv.helmet
            "CHESTPLATE" -> inv.chestplate
            "LEGGINGS" -> inv.leggings
            else -> inv.boots
        }
        return if (item == null || item.type == Material.AIR) null else item
    }

    fun setArmor(player: Player, slot: String, item: ItemStack?) {
        val inv = player.inventory
        when (slot) {
            "HELMET" -> inv.helmet = item
            "CHESTPLATE" -> inv.chestplate = item
            "LEGGINGS" -> inv.leggings = item
            else -> inv.boots = item
        }
    }

    /**
     * Puts the lucky item on the player in that slot (HELMET, CHESTPLATE, LEGGINGS or BOOTS). BedWars1058 locks the
     * armor slots, so the real armor that was there is not put in the inventory (it could never be worn again): it
     * is kept aside and comes back when the lucky piece goes (see [takeOffLucky]). A lucky piece that was there is
     * simply replaced.
     */
    fun equip(player: Player, slot: String, item: ItemStack) {
        val old = armorIn(player, slot)
        if (wornType(old) == null) {
            val kept = Live.underArmor.getOrPut(player.uniqueId) { HashMap() }
            if (!kept.containsKey(slot)) kept[slot] = old
        }
        setArmor(player, slot, item)
    }

    /** Takes every lucky piece off the player and puts back the real armor it replaced. */
    fun takeOffLucky(player: Player) {
        val kept = Live.underArmor.remove(player.uniqueId)
        for (slot in SLOTS) {
            if (wornType(armorIn(player, slot)) != null) setArmor(player, slot, kept?.get(slot))
        }
    }

    /** False only for the off-hand copy of an interact-entity event (1.9+ fires it once per hand). */
    fun isMainHand(event: org.bukkit.event.Event): Boolean {
        return try {
            val hand = event.javaClass.getMethod("getHand").invoke(event)
            hand == null || (hand as Enum<*>).name == "HAND"
        } catch (e: ReflectiveOperationException) {
            true
        } catch (e: RuntimeException) {
            true
        }
    }

    fun freeze(entity: LivingEntity) {
        dev.etran.luckyblock.compat.Compat.setIfPresent(entity, "setAI", false)
        dev.etran.luckyblock.compat.Compat.setIfPresent(entity, "setInvulnerable", true)
        effect(entity, 600, 9, "SLOW", "SLOWNESS")
    }
}
