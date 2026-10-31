package dev.etran.luckyblock.tasks;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.generator.LuckyBlockGenerator;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GeneratorSpawnTask extends BukkitRunnable {

    private final LuckyBlockGenerator generator;

    public GeneratorSpawnTask(LuckyBlockGenerator generator) {
        this.generator = generator;
    }

    public static BukkitTask start(LuckyBlockGenerator generator) {

        int cooldown = generator.getCooldown();

        long ticks = 20L * cooldown;

        // the first lucky block comes after 5 seconds (or the cooldown if that is shorter), so a generator
        // that works is visible right away instead of after a whole cooldown
        long firstDelay = Math.min(ticks, 100L);

        return new GeneratorSpawnTask(generator)
                .runTaskTimer((Plugin) LuckyBlocks.getInstance(), firstDelay, ticks);
    }

    @Override
    public void run() {
        generator.generateItem();
    }
}