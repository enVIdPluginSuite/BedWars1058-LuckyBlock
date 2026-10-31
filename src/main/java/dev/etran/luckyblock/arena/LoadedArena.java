package dev.etran.luckyblock.arena;

import dev.etran.luckyblock.generator.GeneratorKind;
import dev.etran.luckyblock.generator.LoadedGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The manual generators of one arena (the contents of data.yml). */
public class LoadedArena {
  private static final Map<String, LoadedArena> arenas = new HashMap<>();

  private final String arenaName;

  private final List<LoadedGenerator> generators = new ArrayList<>();

  private LoadedArena(String arenaName) {
    this.arenaName = arenaName;
  }

  public static LoadedArena getOrCreate(String name) {
    return arenas.computeIfAbsent(name, LoadedArena::new);
  }

  public static LoadedArena byName(String name) {
    return arenas.get(name);
  }

  public String getArenaName() {
    return this.arenaName;
  }

  public List<LoadedGenerator> getGenerators() {
    return this.generators;
  }

  /** Next free id (highest existing id + 1, or 0). */
  public int nextId() {
    int max = -1;
    for (LoadedGenerator generator : this.generators)
      max = Math.max(max, generator.getId());
    return max + 1;
  }

  public LoadedGenerator addGenerator(int id, GeneratorKind kind, String team, double x, double y, double z, int cooldown) {
    LoadedGenerator generator = new LoadedGenerator(this.arenaName, kind, team, id, x, y, z, cooldown);
    this.generators.add(generator);
    return generator;
  }

  public void addGenerator(LoadedGenerator generator) {
    this.generators.add(generator);
  }

  public boolean removeGenerator(int id) {
    LoadedGenerator generator = getGeneratorById(id);
    return (generator != null) && this.generators.remove(generator);
  }

  public LoadedGenerator getGeneratorById(int id) {
    for (LoadedGenerator generator : this.generators) {
      if (generator.getId() == id)
        return generator;
    }
    return null;
  }

  public LoadedGenerator getGeneratorAtBlock(String blockKey) {
    for (LoadedGenerator generator : this.generators) {
      if (generator.getBlockKey().equals(blockKey))
        return generator;
    }
    return null;
  }
}
