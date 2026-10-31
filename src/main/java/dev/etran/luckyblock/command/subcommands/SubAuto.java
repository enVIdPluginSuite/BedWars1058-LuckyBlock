package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.utils.SetupUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SubAuto extends SubCommand {
  public String getName() {
    return "auto";
  }

  public String getUsage() {
    return "auto <arena> <on|off|default>";
  }

  public String getDescription() {
    return "Forces automatic generators on/off for one arena (default = follow its group)";
  }

  public void execute(CommandSender sender, String[] args) {
    if (args.length != 2) {
      sendUsage(sender);
      return;
    }
    ArenaContext arena = ArenaContext.load(sender, args[0]);
    if (arena == null)
      return;
    Boolean value;
    String mode = args[1].toLowerCase(Locale.ROOT);
    if (mode.equals("on")) {
      value = Boolean.TRUE;
    } else if (mode.equals("off")) {
      value = Boolean.FALSE;
    } else if (mode.equals("default")) {
      value = null;
    } else {
      sendUsage(sender);
      return;
    }
    LuckyBlocks.getInstance().getArenaOverrides().setAuto(arena.name, value);
    if (value == null) {
      boolean lucky = LuckyBlocks.getInstance().getCachedConfig().isLuckyGroup(arena.group);
      sender.sendMessage(ChatColor.GREEN + "Automatic generators in " + ChatColor.AQUA + arena.name + ChatColor.GREEN + " now follow its group (" + arena.group + "): " + (lucky ? "ON" : "OFF") + ".");
    } else {
      sender.sendMessage(ChatColor.GREEN + "Automatic generators in " + ChatColor.AQUA + arena.name + ChatColor.GREEN + " are now forced " + (value.booleanValue() ? "ON" : "OFF") + "." +
          (value.booleanValue() ? "" : (ChatColor.GRAY + " Manually added generators still run.")));
    }
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    if (args.length == 1)
      return filter(SetupUtils.arenaNames(), args[0]);
    if (args.length == 2)
      return filter(Arrays.asList(new String[] { "on", "off", "default" }), args[1]);
    return super.tabComplete(sender, args);
  }
}
