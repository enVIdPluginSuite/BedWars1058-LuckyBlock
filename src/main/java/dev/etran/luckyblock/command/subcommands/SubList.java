package dev.etran.luckyblock.command.subcommands;

import dev.etran.luckyblock.generator.GeneratorPlan;
import dev.etran.luckyblock.generator.LoadedGenerator;
import dev.etran.luckyblock.utils.SetupUtils;
import java.util.List;
import java.util.Locale;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SubList extends SubCommand {
  public String getName() {
    return "list";
  }

  public String getUsage() {
    return "list <arena>";
  }

  public String getDescription() {
    return "Shows every lucky block generator of an arena";
  }

  public void execute(CommandSender sender, String[] args) {
    if (args.length != 1) {
      sendUsage(sender);
      return;
    }
    ArenaContext arena = ArenaContext.load(sender, args[0]);
    if (arena == null)
      return;
    GeneratorPlan.Result plan = arena.plan;
    sender.sendMessage(ChatColor.GOLD + "Lucky block generators of " + ChatColor.AQUA + arena.name + ChatColor.GRAY + " (BedWars group: " + arena.group + ")");
    sender.sendMessage(ChatColor.GRAY + "Automatic generators: " + (plan.isAutoEnabled() ? (ChatColor.GREEN + "ON") : (ChatColor.RED + "OFF")) + ChatColor.GRAY + " - " + plan.getAutoReason());
    if (plan.getEntries().isEmpty()) {
      sender.sendMessage(ChatColor.YELLOW + "This arena has no generators yet. Add one with /bwlb addgenerator " + arena.name + " <team|diamond|emerald>");
      return;
    }
    for (GeneratorPlan.Entry entry : plan.getEntries())
      sendEntry(sender, arena.name, entry);
    sender.sendMessage(ChatColor.GRAY + "Tip: /bwlb show " + arena.name + " labels them in the world.");
  }

  private void sendEntry(CommandSender sender, String arena, GeneratorPlan.Entry entry) {
    LoadedGenerator generator = entry.getSpec();
    ChatColor color = entry.isActive() ? ChatColor.GREEN : ((entry.getStatus() == GeneratorPlan.Status.EXCLUDED) ? ChatColor.RED : ChatColor.DARK_GRAY);
    String text = color + " [" + generator.getDisplayId() + "] " + ChatColor.WHITE + generator.describe() +
      ChatColor.GRAY + String.format(Locale.ROOT, " at %.1f %.1f %.1f", new Object[] { Double.valueOf(generator.getX()), Double.valueOf(generator.getY()), Double.valueOf(generator.getZ()) }) +
      ChatColor.GRAY + ", every " + generator.getCooldown() + "s" +
      (generator.isAuto() ? "" : (ChatColor.AQUA + " (manual)")) +
      (entry.isActive() ? "" : (ChatColor.GRAY + " - " + entry.getStatus().getText())) + " ";
    TextComponent line = new TextComponent(TextComponent.fromLegacyText(text));
    if (entry.getStatus() == GeneratorPlan.Status.EXCLUDED) {
      line.addExtra((BaseComponent)button(ChatColor.GREEN + "[Restore]", "/bwlb restore " + arena + " " + generator.getDisplayId(), "Bring this generator back"));
    } else if (entry.isActive()) {
      line.addExtra((BaseComponent)button(ChatColor.RED + "[Remove]", "/bwlb removegenerator " + arena + " " + generator.getDisplayId(), "Remove this generator"));
    }
    send(sender, (BaseComponent)line);
  }

  private static TextComponent button(String label, String command, String hover) {
    TextComponent button = new TextComponent(TextComponent.fromLegacyText(label));
    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
    button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));
    return button;
  }

  public List<String> tabComplete(CommandSender sender, String[] args) {
    return (args.length == 1) ? filter(SetupUtils.arenaNames(), args[0]) : super.tabComplete(sender, args);
  }
}
