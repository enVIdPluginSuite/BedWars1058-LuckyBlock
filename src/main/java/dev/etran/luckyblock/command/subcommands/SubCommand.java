package dev.etran.luckyblock.command.subcommands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class SubCommand {
  /** The word after /bwlb, e.g. "list". */
  public abstract String getName();

  /** Usage without the leading "/bwlb ", e.g. "list <arena>". */
  public abstract String getUsage();

  public abstract String getDescription();

  /** True if the command uses the sender's location. */
  public boolean requiresPlayer() {
    return false;
  }

  /** @param args the arguments after the sub command name */
  public abstract void execute(CommandSender sender, String[] args);

  /** @param args the arguments after the sub command name (the last one is being typed) */
  public List<String> tabComplete(CommandSender sender, String[] args) {
    return Collections.emptyList();
  }

  protected void sendUsage(CommandSender sender) {
    sender.sendMessage(ChatColor.RED + "/bwlb " + getUsage());
  }

  /** Sends a clickable message to players, or its plain text to the console. */
  protected static void send(CommandSender sender, BaseComponent component) {
    if (sender instanceof Player) {
      ((Player)sender).spigot().sendMessage(component);
    } else {
      sender.sendMessage(component.toLegacyText());
    }
  }

  /** Options that start with what the player has typed so far. */
  protected static List<String> filter(Collection<String> options, String typed) {
    String prefix = typed.toLowerCase(Locale.ROOT);
    List<String> matches = new ArrayList<>();
    for (String option : options) {
      if (option.toLowerCase(Locale.ROOT).startsWith(prefix))
        matches.add(option);
    }
    return matches;
  }

  /** A cooldown in seconds typed by an admin, or null if it isn't a sensible number. */
  protected static Integer parseCooldown(String text) {
    try {
      int seconds = Integer.parseInt(text.trim());
      return (seconds >= 1 && seconds <= 86400) ? Integer.valueOf(seconds) : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
