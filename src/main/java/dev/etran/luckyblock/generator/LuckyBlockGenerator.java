package dev.etran.luckyblock.generator;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.generator.GeneratorType;
import com.andrei1058.bedwars.api.arena.generator.IGenerator;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.arena.LuckyBlockArena;
import dev.etran.luckyblock.compat.Compat;
import dev.etran.luckyblock.data.CachedConfig;
import dev.etran.luckyblock.drops.Rarity;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;
import dev.etran.luckyblock.tasks.GeneratorSpawnTask;
import dev.etran.luckyblock.utils.ColorUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/**
 * A running lucky block generator. It drops a lucky block when its timer runs out, until it reaches the limit.
 * The timer follows the real BedWars1058 generator next to it (so forge and diamond/emerald upgrades speed
 * it up by the same percentage), and a hologram shows both timers and the rarity of the next lucky block.
 */
public class LuckyBlockGenerator {

    private static final Vector ZERO = new Vector(0, 0, 0);
    private static final Random RANDOM = new Random();

    /** How often the generator updates, in ticks. */
    public static final long TICK_PERIOD = 10L;

    private final LoadedGenerator spec;
    private final LuckyBlockArena luckyBlockArena;
    private final World world;
    private final Location location;
    private final ITeam team;
    private final int limit;
    private final boolean requireLivingTeam;
    private final double centrality;
    private final IGenerator realGenerator;
    private final double baseRate;
    private final GeneratorHologram hologram;

    /** The dropped lucky block items that are still lying around (they may be merged into stacks). */
    private final Set<UUID> items = new HashSet<>();

    private BukkitTask task;
    private boolean destroyed = false;

    /** 0 to 1: how far the timer to the next lucky block has run. */
    private double progress;
    private Rarity nextRarity;
    private boolean waitingForSpace = false;
    private long lastTick = System.currentTimeMillis();

    /**
     * @param centrality 0 at the edge of the map, 1 in the very middle (better rarities the closer to the middle)
     */
    public LuckyBlockGenerator(LoadedGenerator spec, IArena arena, LuckyBlockArena luckyBlockArena, double centrality) {

        CachedConfig config = LuckyBlocks.getInstance().getCachedConfig();

        this.spec = spec;
        this.luckyBlockArena = luckyBlockArena;
        this.world = arena.getWorld();
        this.location = new Location(world, spec.getX(), spec.getY(), spec.getZ());
        this.limit = config.getGeneratorLimit();
        this.requireLivingTeam = config.isDoNotSpawnIfTeamDead();
        this.centrality = centrality;

        if (spec.getKind() == GeneratorKind.TEAM) {
            this.team = arena.getTeam(spec.getTeam());
            if (this.team == null) {
                LuckyBlocks.getInstance().getLogger().warning(
                        "Lucky block generator " + spec.getDisplayId() + " in arena " + spec.getArenaName()
                                + " uses the team \"" + spec.getTeam() + "\", which does not exist. It will not spawn anything.");
            }
        } else {
            this.team = null;
        }

        this.realGenerator = findRealGenerator(arena);
        this.baseRate = rateOf(realGenerator);
        this.hologram = config.getHologram().isEnabled() ? new GeneratorHologram(hologramBase(config), 2) : null;

        // the first lucky block comes after a few seconds, so a working generator is visible right away
        double cooldown = Math.max(1, spec.getCooldown());
        this.progress = 1.0D - Math.min(cooldown, 5.0D) / cooldown;
        this.nextRarity = config.rollRarity(spec.getKind(), centrality, RANDOM);

        this.task = GeneratorSpawnTask.start(this);
    }

    private Location hologramBase(CachedConfig config) {
        double height = (spec.getKind() == GeneratorKind.TEAM) ? config.getHologram().getTeamHeight() : config.getHologram().getOreHeight();
        return location.clone().add(0.0D, height, 0.0D);
    }

    /** The BedWars1058 generator that makes the item this lucky block generator goes with (iron / diamond / emerald). */
    private IGenerator findRealGenerator(IArena arena) {
        try {
            GeneratorType wanted = (spec.getKind() == GeneratorKind.TEAM) ? GeneratorType.IRON
                    : (spec.getKind() == GeneratorKind.DIAMOND) ? GeneratorType.DIAMOND : GeneratorType.EMERALD;
            List<IGenerator> pool = new ArrayList<>();
            if (spec.getKind() == GeneratorKind.TEAM) {
                if (team != null && team.getGenerators() != null)
                    pool.addAll(team.getGenerators());
            } else if (arena.getOreGenerators() != null) {
                pool.addAll(arena.getOreGenerators());
            }
            IGenerator best = null;
            double bestDistance = Double.MAX_VALUE;
            for (IGenerator candidate : pool) {
                if (candidate.getType() != wanted || candidate.getLocation() == null || candidate.getLocation().getWorld() != world)
                    continue;
                double distance = candidate.getLocation().distanceSquared(location);
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
            // a diamond/emerald lucky generator belongs to the BedWars1058 generator on the same spot
            if (best != null && spec.getKind() != GeneratorKind.TEAM && bestDistance > 36.0D)
                return null;
            return best;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    /** Items per second of a real generator (amount / delay). */
    private static double rateOf(IGenerator generator) {
        if (generator == null)
            return 0.0D;
        try {
            int delay = Math.max(1, generator.getDelay());
            return Math.max(1, generator.getAmount()) / (double) delay;
        } catch (RuntimeException | LinkageError e) {
            return 0.0D;
        }
    }

    /** How much faster than at the start the real generator runs now (1 = same speed). */
    public double getSpeedFactor() {
        if (!LuckyBlocks.getInstance().getCachedConfig().isSyncUpgrades() || realGenerator == null || baseRate <= 0.0D)
            return 1.0D;
        double now = rateOf(realGenerator);
        if (now <= 0.0D)
            return 1.0D;
        return Math.max(0.25D, Math.min(10.0D, now / baseRate));
    }

    /** Seconds until the next lucky block, at the current speed (0 if it is only waiting for space). */
    public double getSecondsLeft() {
        double cooldown = Math.max(1, spec.getCooldown());
        return Math.max(0.0D, (1.0D - progress) * cooldown / getSpeedFactor());
    }

    public Rarity getNextRarity() {
        return nextRarity;
    }

    /** Called every {@link #TICK_PERIOD} ticks. */
    public void tick() {

        if (destroyed) return;

        if (spec.getKind() == GeneratorKind.TEAM) {

            // a team that doesn't exist, or has been eliminated, doesn't get lucky blocks anymore
            if (team == null || (requireLivingTeam && team.getMembers().isEmpty())) {
                destroy();
                luckyBlockArena.removeGenerator(this);
                return;
            }
        }

        long now = System.currentTimeMillis();
        double seconds = Math.min(2.0D, (now - lastTick) / 1000.0D);
        lastTick = now;

        if (progress < 1.0D)
            progress = Math.min(1.0D, progress + seconds * getSpeedFactor() / Math.max(1, spec.getCooldown()));

        if (progress >= 1.0D) {
            if (getCount() < limit) {
                spawnLuckyBlock();
                progress = 0.0D;
                waitingForSpace = false;
                nextRarity = LuckyBlocks.getInstance().getCachedConfig().rollRarity(spec.getKind(), centrality, RANDOM);
            } else {
                waitingForSpace = true;
            }
        }

        updateHologram();
    }

    private void spawnLuckyBlock() {

        Item item = world.dropItem(location, LuckyBlockTexture.randomTexture(nextRarity).getBukkitItem().clone());

        item.setVelocity(ZERO);

        items.add(item.getUniqueId());
    }

    private void updateHologram() {

        if (hologram == null) return;

        List<String> lines = new ArrayList<>();

        // line 1: the real generator's timer for its item
        if (realGenerator != null) {
            lines.add(oreLabel(realGenerator.getType()) + ChatColor.GRAY + " spawns in " + ChatColor.WHITE + Math.max(0, realGenerator.getNextSpawn()) + "s");
        } else {
            lines.add(ChatColor.GRAY + "Lucky Block Generator");
        }

        // line 2: the next lucky block, its rarity and when it comes
        String rarityName = ColorUtils.colorize(LuckyBlocks.getInstance().getCachedConfig().getRarity(nextRarity).getDisplay());
        String when = waitingForSpace ? (ChatColor.RED + "full") : (ChatColor.WHITE.toString() + (int) Math.ceil(getSecondsLeft()) + "s");
        lines.add(ChatColor.LIGHT_PURPLE + "✦ " + rarityName + ChatColor.LIGHT_PURPLE + " Lucky Block " + ChatColor.GRAY + "in " + when);

        hologram.setLines(lines);
    }

    private static String oreLabel(GeneratorType type) {
        switch (type) {
            case DIAMOND: return ChatColor.AQUA + "Diamond";
            case EMERALD: return ChatColor.GREEN + "Emerald";
            case GOLD: return ChatColor.GOLD + "Gold";
            default: return ChatColor.WHITE + "Iron";
        }
    }

    /**
     * How many lucky blocks from this generator are still lying around, picking up the changes from
     * players collecting them, items despawning and stacks merging.
     */
    public int getCount() {

        int total = 0;

        Iterator<UUID> iterator = items.iterator();

        while (iterator.hasNext()) {

            Entity entity = Compat.findEntity(world, iterator.next());

            if (!(entity instanceof Item) || !entity.isValid()) {
                iterator.remove();
                continue;
            }

            total += ((Item) entity).getItemStack().getAmount();
        }

        return total;
    }

    public boolean isTracking(UUID item) {
        return items.contains(item);
    }

    public void track(UUID item) {
        items.add(item);
    }

    public void destroy() {

        destroyed = true;

        if (task != null) task.cancel();
        task = null;

        if (hologram != null) hologram.destroy();

        items.clear();
    }

    public LoadedGenerator getSpec() { return spec; }

    public GeneratorKind getKind() { return spec.getKind(); }

    public int getCooldown() { return spec.getCooldown(); }

    public Location getLocation() { return location; }

    public ITeam getTeam() { return team; }
}
