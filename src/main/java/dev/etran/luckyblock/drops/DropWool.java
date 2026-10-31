package dev.etran.luckyblock.drops;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import com.andrei1058.bedwars.api.arena.team.TeamColor;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.compat.Items;
import dev.etran.luckyblock.data.Language;
import dev.etran.luckyblock.luckyblock.texture.GlassColor;
import java.util.Locale;
import lombok.Generated;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DropWool extends Drop {
  private final Language language;

  @Generated
  public DropWool(Language language) {
    this.language = language;
  }

  public void drop(Location location, Player player) {
    IArena arena = LuckyBlocks.getBedWars().getArenaUtil().getArenaByPlayer(player);
    if (arena == null)
      return;
    ITeam team = arena.getTeam(player);
    if (team == null)
      return;
    TeamColor color = team.getColor();
    // the team's wool color on every version (RED_WOOL on 1.13+, WOOL with data before)
    ItemStack wool = Items.wool(GlassColor.fromDye(color.dye())).stack(64);
    ItemMeta meta = wool.getItemMeta();
    String name = prettyName(color) + " Wool";
    meta.setDisplayName(name);
    wool.setItemMeta(meta);
    location.getWorld().dropItem(location, wool);
    this.language.sendReceivedItemMessage(player, name, 64);
  }

  // DARK_GREEN -> "Dark Green"
  private static String prettyName(TeamColor color) {
    StringBuilder builder = new StringBuilder();
    for (String part : color.name().split("_")) {
      if (builder.length() > 0)
        builder.append(' ');
      builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1).toLowerCase(Locale.ROOT));
    }
    return builder.toString();
  }
}
