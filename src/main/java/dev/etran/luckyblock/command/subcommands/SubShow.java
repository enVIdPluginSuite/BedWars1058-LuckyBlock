package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.compat.Compat;
import dev.etran.luckyblock.generator.GeneratorPlan;
import dev.etran.luckyblock.generator.LoadedGenerator;
import dev.etran.luckyblock.utils.SetupUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

/** Floats a label above every generator of an arena for a few seconds, so you can see where they are. */
public class SubShow extends SubCommand {
  private static final long SHOW_TICKS = 300L;

  public String getName() {
    return "show";
  }

  public String getUsage() {
    return "show <arena>";
  }

  public String getDescription() {
    return "Labels the generators in the world for 15 seconds (be in the arena's world)";
  }

  public boolean requiresPlayer() {
    return true;
  }

  public void execute(CommandSender sender, String[] args) {
    if (args.length != 1) {
      sendUsage(sender);
      return;
    }
    ArenaContext arena = ArenaContext.load(sender, args[0]);
    if (arena == null)
      return;
    Player player = (Player)sender;
    World world = player.getWorld();
    final List<ArmorStand> labels = new ArrayList<>();
    for (GeneratorPlan.Entry entry : arena.plan.getEntries()) {
      LoadedGenerator generator = entry.getSpec();
      ChatColor color = entry.isActive() ? ChatColor.GREEN : ((entry.getStatus() == GeneratorPlan.Status.EXCLUDED) ? ChatColor.RED : ChatColor.GRAY);
      ArmorStand label = (ArmorStand)world.spawnEntity(new Location(world, generator.getX(), generator.getY() + 1.2D, generator.getZ()), EntityType.ARMOR_STAND);
      label.setVisible(false);
      label.setGravity(false);
      label.setMarker(true);
      Compat.setIfPresent(label, "setInvulnerable", true);
      label.setSmall(true);
      Compat.setIfPresent(label, "setPersistent", false);
      label.setCustomName(color + "[" + generator.getDisplayId() + "] " + generator.describe() + (entry.isActive() ? "" : (" (" + entry.getStatus().getText() + ")")));
      label.setCustomNameVisible(true);
      labels.add(label);
    }
    if (labels.isEmpty()) {
      player.sendMessage(ChatColor.YELLOW + "This arena has no generators to show.");
      return;
    }
    player.sendMessage(ChatColor.GREEN + "Showing " + labels.size() + " generator labels in " + ChatColor.AQUA + world.getName() + ChatColor.GREEN + " for 15 seconds." + ChatColor.GRAY + " (Green = running, red = removed, gray = inactive)");
    LuckyBlocks.getInstance().getServer().getScheduler().runTaskLater((org.bukkit.plugin.Plugin)LuckyBlocks.getInstance(), () -> {
          for (ArmorStand label : labels)
            label.remove();
        }, SHOW_TICKS);
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    return (args.length == 1) ? filter(SetupUtils.arenaNames(), args[0]) : super.tabComplete(sender, args);
  }
}
