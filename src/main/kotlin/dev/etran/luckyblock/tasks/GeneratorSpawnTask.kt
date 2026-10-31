package dev.etran.luckyblock.tasks

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.generator.LuckyBlockGenerator
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

class GeneratorSpawnTask(private val generator: LuckyBlockGenerator) : BukkitRunnable() {

    override fun run() {
        generator.tick()
    }

    companion object {
        @JvmStatic
        fun start(generator: LuckyBlockGenerator): BukkitTask =
            GeneratorSpawnTask(generator)
                .runTaskTimer(LuckyBlocks.getInstance(), LuckyBlockGenerator.TICK_PERIOD, LuckyBlockGenerator.TICK_PERIOD)
    }
}
