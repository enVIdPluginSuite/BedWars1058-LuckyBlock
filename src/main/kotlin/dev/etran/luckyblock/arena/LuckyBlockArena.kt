package dev.etran.luckyblock.arena

import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.generator.GeneratorKind
import dev.etran.luckyblock.generator.GeneratorPlan
import dev.etran.luckyblock.generator.LuckyBlockGenerator
import java.util.EnumMap
import java.util.logging.Level
import kotlin.math.hypot

/** The lucky block generators running in one BedWars1058 game. */
class LuckyBlockArena private constructor(val arenaName: String, val worldName: String) {
    private val generators: MutableSet<LuckyBlockGenerator> = HashSet()

    fun getGenerators(): MutableSet<LuckyBlockGenerator> = generators

    fun removeGenerator(generator: LuckyBlockGenerator) {
        generators.remove(generator)
    }

    fun destroy() {
        for (generator in ArrayList(generators)) generator.destroy()
        generators.clear()
    }

    companion object {
        private val arenas: MutableMap<String, LuckyBlockArena> = HashMap()

        /** Called when a game starts: works out the generators for the arena and starts them. */
        @JvmStatic
        fun start(arena: IArena) {
            val name = arena.arenaName
            // games of one arena share its name (only the world differs), so a running game is told apart by its world
            val key = arena.worldName
            stop(key)
            try {
                val plan = GeneratorPlan.forArena(name, arena.group, arena.config.yml)
                val luckyBlockArena = LuckyBlockArena(name, key)
                val counts = EnumMap<GeneratorKind, Int>(GeneratorKind::class.java)
                var manual = 0
                val active = plan.getActive()

                // the middle of the map is the middle of all its generators; the closer a generator is, the better its lucky blocks
                var centerX = 0.0
                var centerZ = 0.0
                for (entry in active) {
                    centerX += entry.spec.x / active.size
                    centerZ += entry.spec.z / active.size
                }
                var radius = 0.0
                for (entry in active) radius = maxOf(radius, hypot(entry.spec.x - centerX, entry.spec.z - centerZ))

                for (entry in active) {
                    val distance = hypot(entry.spec.x - centerX, entry.spec.z - centerZ)
                    val centrality = if (radius <= 0.0) 0.0 else maxOf(0.0, 1.0 - distance / radius)
                    luckyBlockArena.generators.add(LuckyBlockGenerator(entry.spec, arena, luckyBlockArena, centrality))
                    counts.merge(entry.spec.kind, 1) { a, b -> a + b }
                    if (!entry.spec.isAuto) manual++
                }

                val logger = LuckyBlocks.getInstance().logger
                // say what happened to every spot found in the arena file, so a silent arena can be explained from the log
                val skipped = plan.entries.filter { !it.isActive }.groupingBy { it.status.text }.eachCount()
                if (plan.entries.isEmpty()) {
                    logger.info("Lucky blocks in $name: no generator spots found (no manual generators, and none in the arena file).")
                } else if (skipped.isNotEmpty()) {
                    logger.info("Lucky blocks in $name: ${plan.entries.size} spots found, not used: " +
                        skipped.entries.joinToString(", ") { "${it.value} ${it.key}" })
                }
                if (luckyBlockArena.generators.isEmpty()) {
                    logger.info(
                        "No lucky blocks in $name: ${plan.autoReason}" +
                            (if (plan.isAutoEnabled) "" else ". To turn them on, use /bwlb group add ${plan.group}") + "."
                    )
                    return
                }
                arenas[key] = luckyBlockArena
                logger.info(
                    "Lucky blocks started in $name (group ${plan.group}): " +
                        "${counts[GeneratorKind.TEAM] ?: 0} team, ${counts[GeneratorKind.DIAMOND] ?: 0} diamond, " +
                        "${counts[GeneratorKind.EMERALD] ?: 0} emerald generators ($manual manual)."
                )
            } catch (e: RuntimeException) {
                LuckyBlocks.getInstance().logger.log(Level.SEVERE, "Could not start the lucky block generators of $name", e)
            } catch (e: LinkageError) {
                LuckyBlocks.getInstance().logger.log(Level.SEVERE, "Could not start the lucky block generators of $name", e)
            }
        }

        @JvmStatic
        fun stop(worldName: String) {
            arenas.remove(worldName)?.destroy()
        }

        @JvmStatic
        fun stopAll() {
            for (arena in ArrayList(arenas.values)) arena.destroy()
            arenas.clear()
        }

        @JvmStatic
        fun all(): Collection<LuckyBlockArena> = arenas.values

        @JvmStatic
        fun byWorldName(worldName: String): LuckyBlockArena? = arenas[worldName]
    }
}
