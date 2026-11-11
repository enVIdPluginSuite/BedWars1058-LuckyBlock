package dev.etran.luckyblock.generator

import com.andrei1058.bedwars.api.arena.IArena
import com.andrei1058.bedwars.api.arena.generator.GeneratorType
import com.andrei1058.bedwars.api.arena.generator.IGenerator
import com.andrei1058.bedwars.api.arena.team.ITeam
import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.arena.LuckyBlockArena
import dev.etran.luckyblock.compat.Compat
import dev.etran.luckyblock.data.CachedConfig
import dev.etran.luckyblock.drops.Rarity
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import dev.etran.luckyblock.tasks.GeneratorSpawnTask
import dev.etran.luckyblock.utils.ColorUtils
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import java.util.Random
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A running lucky block generator. It drops a lucky block when its timer runs out, until it reaches the limit.
 * The timer follows the real BedWars1058 generator next to it (so forge and diamond/emerald upgrades speed
 * it up by the same percentage), and a hologram shows both timers and the rarity of the next lucky block.
 *
 * @param centrality 0 at the edge of the map, 1 in the very middle (better rarities the closer to the middle)
 */
class LuckyBlockGenerator(
    val spec: LoadedGenerator,
    arena: IArena,
    private val luckyBlockArena: LuckyBlockArena,
    private val centrality: Double
) {
    private val world = arena.world
    val location: Location = Location(world, spec.x, spec.y, spec.z)
    val team: ITeam?
    private val limit: Int
    private val requireLivingTeam: Boolean
    private val realGenerator: IGenerator?
    private val baseRate: Double
    private val hologram: GeneratorHologram?

    /** The dropped lucky block items that are still lying around (they may be merged into stacks). */
    private val items: MutableSet<UUID> = HashSet()

    private var task: BukkitTask? = null
    private var destroyed = false

    /** 0 to 1: how far the timer to the next lucky block has run. */
    private var progress: Double
    var nextRarity: Rarity
        private set
    private var waitingForSpace = false
    private var reportedFailure = false
    private var lastTick = System.currentTimeMillis()

    val kind: GeneratorKind get() = spec.kind
    val cooldown: Int get() = spec.cooldown

    init {
        val config = LuckyBlocks.getInstance().cachedConfig
        limit = config.generatorLimit
        requireLivingTeam = config.isDoNotSpawnIfTeamDead

        if (spec.kind == GeneratorKind.TEAM) {
            team = arena.getTeam(spec.team)
            if (team == null) {
                LuckyBlocks.getInstance().logger.warning(
                    "Lucky block generator ${spec.displayId} in arena ${spec.arenaName}" +
                        " uses the team \"${spec.team}\", which does not exist. It will not spawn anything."
                )
            }
        } else {
            team = null
        }

        realGenerator = findRealGenerator(arena)
        baseRate = rateOf(realGenerator)
        hologram = if (config.hologram.isEnabled) GeneratorHologram(hologramBase(config), 2) else null

        // the first lucky block comes after a few seconds, so a working generator is visible right away
        val cd = max(1, spec.cooldown).toDouble()
        progress = 1.0 - min(cd, 5.0) / cd
        nextRarity = config.rollRarity(spec.kind, centrality, RANDOM)

        task = GeneratorSpawnTask.start(this)
    }

    private fun hologramBase(config: CachedConfig): Location {
        val height = if (spec.kind == GeneratorKind.TEAM) config.hologram.teamHeight else config.hologram.oreHeight
        return location.clone().add(0.0, height, 0.0)
    }

    /** The BedWars1058 generator that makes the item this lucky block generator goes with (iron / diamond / emerald). */
    private fun findRealGenerator(arena: IArena): IGenerator? {
        try {
            val wanted = when (spec.kind) {
                GeneratorKind.TEAM -> GeneratorType.IRON
                GeneratorKind.DIAMOND -> GeneratorType.DIAMOND
                else -> GeneratorType.EMERALD
            }
            val pool = ArrayList<IGenerator>()
            if (spec.kind == GeneratorKind.TEAM) {
                val generators = team?.generators
                if (generators != null) pool.addAll(generators)
            } else if (arena.oreGenerators != null) {
                pool.addAll(arena.oreGenerators)
            }
            var best: IGenerator? = null
            var bestDistance = Double.MAX_VALUE
            for (candidate in pool) {
                val at = candidate.location
                if (candidate.type != wanted || at == null || at.world != world) continue
                val distance = at.distanceSquared(location)
                if (distance < bestDistance) {
                    best = candidate
                    bestDistance = distance
                }
            }
            // a diamond/emerald lucky generator belongs to the BedWars1058 generator on the same spot
            if (best != null && spec.kind != GeneratorKind.TEAM && bestDistance > 36.0) return null
            return best
        } catch (e: RuntimeException) {
            return null
        } catch (e: LinkageError) {
            return null
        }
    }

    /** How much faster than at the start the real generator runs now (1 = same speed). */
    fun getSpeedFactor(): Double {
        if (!LuckyBlocks.getInstance().cachedConfig.isSyncUpgrades || realGenerator == null || baseRate <= 0.0) return 1.0
        val now = rateOf(realGenerator)
        if (now <= 0.0) return 1.0
        return max(0.25, min(10.0, now / baseRate))
    }

    /** Seconds until the next lucky block, at the current speed (0 if it is only waiting for space). */
    fun getSecondsLeft(): Double {
        val cd = max(1, spec.cooldown).toDouble()
        return max(0.0, (1.0 - progress) * cd / getSpeedFactor())
    }

    /** Called every [TICK_PERIOD] ticks. */
    fun tick() {
        if (destroyed) return

        if (spec.kind == GeneratorKind.TEAM) {
            // a team that doesn't exist, or has been eliminated, doesn't get lucky blocks anymore
            if (team == null || (requireLivingTeam && team.members.isEmpty())) {
                LuckyBlocks.getInstance().logger.info(
                    "Lucky block generator ${spec.displayId} of team ${spec.team} in ${spec.arenaName} stopped: " +
                        if (team == null) "the team does not exist." else "the team has no players."
                )
                destroy()
                luckyBlockArena.removeGenerator(this)
                return
            }
        }

        val now = System.currentTimeMillis()
        val seconds = min(2.0, (now - lastTick) / 1000.0)
        lastTick = now

        if (progress < 1.0) {
            progress = min(1.0, progress + seconds * getSpeedFactor() / max(1, spec.cooldown))
        }

        if (progress >= 1.0) {
            if (getCount() < limit) {
                try {
                    spawnLuckyBlock()
                } catch (e: RuntimeException) {
                    if (!reportedFailure) {
                        reportedFailure = true
                        LuckyBlocks.getInstance().logger.log(
                            java.util.logging.Level.SEVERE, "Lucky block generator ${spec.displayId} in ${spec.arenaName} could not drop a lucky block", e)
                    }
                    return
                }
                progress = 0.0
                waitingForSpace = false
                nextRarity = LuckyBlocks.getInstance().cachedConfig.rollRarity(spec.kind, centrality, RANDOM)
            } else {
                waitingForSpace = true
            }
        }

        updateHologram()
    }

    private fun spawnLuckyBlock() {
        val item = world.dropItem(location, LuckyBlockTexture.randomTexture(nextRarity).bukkitItem.clone())
        item.velocity = ZERO
        items.add(item.uniqueId)
    }

    private fun updateHologram() {
        val hologram = hologram ?: return
        val lines = ArrayList<String>()

        // line 1: the real generator's timer for its item
        if (realGenerator != null) {
            lines.add(oreLabel(realGenerator.type) + ChatColor.GRAY + " spawns in " + ChatColor.WHITE + max(0, realGenerator.nextSpawn) + "s")
        } else {
            lines.add(ChatColor.GRAY.toString() + "Lucky Block Generator")
        }

        // line 2: the next lucky block, its rarity and when it comes
        val rarityName = ColorUtils.colorize(LuckyBlocks.getInstance().cachedConfig.getRarity(nextRarity).display)
        val whenText = if (waitingForSpace) ChatColor.RED.toString() + "full" else ChatColor.WHITE.toString() + ceil(getSecondsLeft()).toInt() + "s"
        lines.add(ChatColor.LIGHT_PURPLE.toString() + "✦ " + rarityName + ChatColor.LIGHT_PURPLE + " Lucky Block " + ChatColor.GRAY + "in " + whenText)

        hologram.setLines(lines)
    }

    /**
     * How many lucky blocks from this generator are still lying around, picking up the changes from
     * players collecting them, items despawning and stacks merging.
     */
    fun getCount(): Int {
        var total = 0
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val entity = Compat.findEntity(world, iterator.next())
            if (entity !is Item || !entity.isValid) {
                iterator.remove()
                continue
            }
            total += entity.itemStack.amount
        }
        return total
    }

    fun isTracking(item: UUID): Boolean = items.contains(item)

    fun track(item: UUID) {
        items.add(item)
    }

    fun destroy() {
        destroyed = true
        task?.cancel()
        task = null
        hologram?.destroy()
        items.clear()
    }

    companion object {
        private val ZERO = Vector(0, 0, 0)
        private val RANDOM = Random()

        /** How often the generator updates, in ticks. */
        const val TICK_PERIOD = 10L

        /** Items per second of a real generator (amount / delay). */
        private fun rateOf(generator: IGenerator?): Double {
            if (generator == null) return 0.0
            return try {
                val delay = max(1, generator.delay)
                max(1, generator.amount) / delay.toDouble()
            } catch (e: RuntimeException) {
                0.0
            } catch (e: LinkageError) {
                0.0
            }
        }

        private fun oreLabel(type: GeneratorType): String = when (type) {
            GeneratorType.DIAMOND -> ChatColor.AQUA.toString() + "Diamond"
            GeneratorType.EMERALD -> ChatColor.GREEN.toString() + "Emerald"
            GeneratorType.GOLD -> ChatColor.GOLD.toString() + "Gold"
            else -> ChatColor.WHITE.toString() + "Iron"
        }
    }
}
