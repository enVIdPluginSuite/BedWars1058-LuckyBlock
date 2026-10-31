package dev.etran.luckyblock.command.subcommands;

import com.andrei1058.bedwars.api.arena.IArena;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.arena.LuckyBlockArena;
import dev.etran.luckyblock.generator.GeneratorPlan;
import dev.etran.luckyblock.generator.LuckyBlockGenerator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/** Shows the BedWars1058 games that are running right now and whether lucky blocks are running in them. */
public class SubStatus extends SubCommand {
  public String getName() {
    return "status";
  }

  public String getUsage() {
    return "status";
  }

  public String getDescription() {
    return "Shows the running games and what their lucky block generators are doing";
  }

  public void execute(CommandSender sender, String[] args) {
    java.util.List<IArena> games = LuckyBlocks.getBedWars().getArenaUtil().getArenas();
    sender.sendMessage(ChatColor.GOLD + "Lucky blocks in running BedWars1058 games:");
    if (games.isEmpty())
      sender.sendMessage(ChatColor.GRAY + " No games are loaded right now.");
    for (IArena game : games) {
      String name = game.getArenaName();
      LuckyBlockArena running = LuckyBlockArena.byArenaName(name);
      if (running != null) {
        int lying = 0;
        for (LuckyBlockGenerator generator : running.getGenerators())
          lying += generator.getCount();
        sender.sendMessage(ChatColor.GREEN + " " + name + ChatColor.GRAY + " [" + game.getStatus() + "]: " + running.getGenerators().size() +
            " generators running, " + lying + " lucky block(s) lying around");
        continue;
      }
      GeneratorPlan.Result plan = GeneratorPlan.forArena(name, game.getGroup(), game.getConfig().getYml());
      sender.sendMessage(ChatColor.GRAY + " " + name + " [" + game.getStatus() + "]: not running - " + plan.getAutoReason() +
          (plan.isAutoEnabled() ? "" : ChatColor.YELLOW + " (fix: /bwlb group add " + plan.getGroup() + ")"));
    }
  }
}
