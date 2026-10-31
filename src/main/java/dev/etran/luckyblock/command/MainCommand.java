package dev.etran.luckyblock.command;

import dev.etran.luckyblock.command.subcommands.SubAddGenerator;
import dev.etran.luckyblock.command.subcommands.SubAuto;
import dev.etran.luckyblock.command.subcommands.SubAutoSetup;
import dev.etran.luckyblock.command.subcommands.SubCommand;
import dev.etran.luckyblock.command.subcommands.SubList;
import dev.etran.luckyblock.command.subcommands.SubLoot;
import dev.etran.luckyblock.command.subcommands.SubRemoveGenerator;
import dev.etran.luckyblock.command.subcommands.SubRestore;
import dev.etran.luckyblock.command.subcommands.SubShow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class MainCommand implements TabExecutor {
  private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();

  public MainCommand() {
    register(new SubList());
    register(new SubShow());
    register(new SubAddGenerator());
    register(new SubRemoveGenerator());
    register(new SubRestore());
    register(new SubAuto());
    register(new SubAutoSetup());
    register(new SubLoot());
  }

  private void register(SubCommand subCommand) {
    this.subCommands.put(subCommand.getName(), subCommand);
  }

  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    SubCommand subCommand = (args.length == 0) ? null : this.subCommands.get(args[0].toLowerCase(Locale.ROOT));
    if (subCommand == null) {
      sendHelp(sender);
      return true;
    }
    if (subCommand.requiresPlayer() && !(sender instanceof Player)) {
      sender.sendMessage(ChatColor.RED + "Cannot execute this command as console");
      return true;
    }
    subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    return true;
  }

  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length <= 1) {
      List<String> names = new ArrayList<>(this.subCommands.keySet());
      return filter(names, (args.length == 0) ? "" : args[0]);
    }
    SubCommand subCommand = this.subCommands.get(args[0].toLowerCase(Locale.ROOT));
    if (subCommand == null)
      return new ArrayList<>();
    return subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
  }

  private static List<String> filter(List<String> options, String typed) {
    List<String> matches = new ArrayList<>();
    for (String option : options) {
      if (option.startsWith(typed.toLowerCase(Locale.ROOT)))
        matches.add(option);
    }
    return matches;
  }

  private void sendHelp(CommandSender sender) {
    sender.sendMessage(ChatColor.GOLD + "LuckyBlocks " + ChatColor.GRAY + "- lucky blocks spawn automatically in arenas of the lucky block groups.");
    for (SubCommand subCommand : this.subCommands.values())
      sender.sendMessage(ChatColor.YELLOW + "/luckyblock " + subCommand.getUsage() + ChatColor.GRAY + " - " + subCommand.getDescription());
  }
}
