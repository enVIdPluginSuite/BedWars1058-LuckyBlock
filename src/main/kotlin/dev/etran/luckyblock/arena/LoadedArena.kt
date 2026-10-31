package dev.etran.luckyblock.arena

import dev.etran.luckyblock.generator.GeneratorKind
import dev.etran.luckyblock.generator.LoadedGenerator

/** The manual generators of one arena (the contents of data.yml). */
class LoadedArena private constructor(val arenaName: String) {
    private val generators: MutableList<LoadedGenerator> = ArrayList()

    fun getGenerators(): MutableList<LoadedGenerator> = generators

    /** Next free id (highest existing id + 1, or 0). */
    fun nextId(): Int = (generators.maxOfOrNull { it.id } ?: -1) + 1

    fun addGenerator(id: Int, kind: GeneratorKind, team: String?, x: Double, y: Double, z: Double, cooldown: Int): LoadedGenerator {
        val generator = LoadedGenerator(arenaName, kind, team, id, x, y, z, cooldown)
        generators.add(generator)
        return generator
    }

    fun addGenerator(generator: LoadedGenerator) {
        generators.add(generator)
    }

    fun removeGenerator(id: Int): Boolean {
        val generator = getGeneratorById(id)
        return generator != null && generators.remove(generator)
    }

    fun getGeneratorById(id: Int): LoadedGenerator? = generators.firstOrNull { it.id == id }

    fun getGeneratorAtBlock(blockKey: String): LoadedGenerator? = generators.firstOrNull { it.blockKey == blockKey }

    companion object {
        private val arenas: MutableMap<String, LoadedArena> = HashMap()

        @JvmStatic
        fun getOrCreate(name: String): LoadedArena = arenas.getOrPut(name) { LoadedArena(name) }

        /** Forgets every arena (used when data.yml is reloaded). */
        @JvmStatic
        fun clearAll() {
            arenas.clear()
        }

        @JvmStatic
        fun byName(name: String): LoadedArena? = arenas[name]
    }
}
