package dev.etran.luckyblock.generator;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.arena.LuckyBlockArena;
import dev.etran.luckyblock.compat.Compat;
import dev.etran.luckyblock.data.CachedConfig;
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture;
import dev.etran.luckyblock.tasks.GeneratorSpawnTask;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

/** A running lucky block generator: drops a lucky block every few seconds until it reaches the limit. */
public class LuckyBlockGenerator {

    private static final Vector ZERO = new Vector(0, 0, 0);

    private final LoadedGenerator spec;
    private final LuckyBlockArena luckyBlockArena;
    private final World world;
    private final Location location;
    private final ITeam team;
    private final int limit;
    private final boolean requireLivingTeam;
    private final List<String> textures;

    /** The dropped lucky block items that are still lying around (they may be merged into stacks). */
    private final Set<UUID> items = new HashSet<>();

    private BukkitTask task;
    private boolean destroyed = false;

    public LuckyBlockGenerator(LoadedGenerator spec, IArena arena, LuckyBlockArena luckyBlockArena) {

        CachedConfig config = LuckyBlocks.getInstance().getCachedConfig();

        this.spec = spec;
        this.luckyBlockArena = luckyBlockArena;
        this.world = arena.getWorld();
        this.location = new Location(world, spec.getX(), spec.getY(), spec.getZ());
        this.limit = config.getGeneratorLimit();
        this.requireLivingTeam = config.isDoNotSpawnIfTeamDead();
        this.textures = config.getKind(spec.getKind()).getTextures();

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

        this.task = GeneratorSpawnTask.start(this);
    }

    public void generateItem() {

        if (destroyed) return;

        if (spec.getKind() == GeneratorKind.TEAM) {

            // a team that doesn't exist, or has been eliminated, doesn't get lucky blocks anymore
            if (team == null || (requireLivingTeam && team.getMembers().isEmpty())) {
                destroy();
                luckyBlockArena.removeGenerator(this);
                return;
            }
        }

        if (getCount() >= limit) return;

        Item item = world.dropItem(location, LuckyBlockTexture.randomTexture(textures).getBukkitItem().clone());

        item.setVelocity(ZERO);

        items.add(item.getUniqueId());
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

        items.clear();
    }

    public LoadedGenerator getSpec() { return spec; }

    public GeneratorKind getKind() { return spec.getKind(); }

    public int getCooldown() { return spec.getCooldown(); }

    public Location getLocation() { return location; }

    public ITeam getTeam() { return team; }
}
