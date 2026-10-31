package dev.etran.luckyblock.arena;

import com.andrei1058.bedwars.api.arena.IArena;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.generator.GeneratorKind;
import dev.etran.luckyblock.generator.GeneratorPlan;
import dev.etran.luckyblock.generator.LuckyBlockGenerator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/** The lucky block generators running in one BedWars1058 game. */
public class LuckyBlockArena {
  private static final Map<String, LuckyBlockArena> arenas = new HashMap<>();

  private final Set<LuckyBlockGenerator> generators = new HashSet<>();

  private final String arenaName;

  private LuckyBlockArena(String arenaName) {
    this.arenaName = arenaName;
  }

  public Set<LuckyBlockGenerator> getGenerators() {
    return this.generators;
  }

  public String getArenaName() {
    return this.arenaName;
  }

  /** Called when a game starts: works out the generators for the arena and starts them. */
  public static void start(IArena arena) {
    String name = arena.getArenaName();
    stop(name);
    try {
      GeneratorPlan.Result plan = GeneratorPlan.forArena(name, arena.getGroup(), arena.getConfig().getYml());
      LuckyBlockArena luckyBlockArena = new LuckyBlockArena(name);
      Map<GeneratorKind, Integer> counts = new EnumMap<>(GeneratorKind.class);
      int manual = 0;
      for (GeneratorPlan.Entry entry : plan.getActive()) {
        luckyBlockArena.generators.add(new LuckyBlockGenerator(entry.getSpec(), arena, luckyBlockArena));
        counts.merge(entry.getSpec().getKind(), Integer.valueOf(1), Integer::sum);
        if (!entry.getSpec().isAuto())
          manual++;
      }
      if (luckyBlockArena.generators.isEmpty())
        return;
      arenas.put(name, luckyBlockArena);
      LuckyBlocks.getInstance().getLogger().info("Lucky blocks started in " + name + " (group " + plan.getGroup() + "): " +
          counts.getOrDefault(GeneratorKind.TEAM, Integer.valueOf(0)) + " team, " +
          counts.getOrDefault(GeneratorKind.DIAMOND, Integer.valueOf(0)) + " diamond, " +
          counts.getOrDefault(GeneratorKind.EMERALD, Integer.valueOf(0)) + " emerald generators (" + manual + " manual).");
    } catch (RuntimeException e) {
      LuckyBlocks.getInstance().getLogger().log(Level.SEVERE, "Could not start the lucky block generators of " + name, e);
    }
  }

  public static void stop(String arenaName) {
    LuckyBlockArena arena = arenas.remove(arenaName);
    if (arena != null)
      arena.destroy();
  }

  public static void stopAll() {
    for (LuckyBlockArena arena : new ArrayList<>(arenas.values()))
      arena.destroy();
    arenas.clear();
  }

  public static Collection<LuckyBlockArena> all() {
    return arenas.values();
  }

  public static LuckyBlockArena byArenaName(String name) {
    return arenas.get(name);
  }

  public void removeGenerator(LuckyBlockGenerator generator) {
    this.generators.remove(generator);
  }

  public void destroy() {
    for (LuckyBlockGenerator generator : new ArrayList<>(this.generators))
      generator.destroy();
    this.generators.clear();
  }
}
