package dev.etran.luckyblock.drops;

import dev.etran.luckyblock.compat.Items;
import dev.etran.luckyblock.data.Language;
import dev.etran.luckyblock.luckyblock.texture.GlassColor;
import dev.etran.luckyblock.utils.NBTUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import lombok.Generated;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DropRainbowWool extends Drop {
  @Generated
  public DropRainbowWool(ItemStack rainbowWool, Language language) {
    this.rainbowWool = rainbowWool;
    this.language = language;
  }
  
  private static final List<GlassColor> colors = Arrays.asList(
      GlassColor.LIME,
      GlassColor.CYAN,
      GlassColor.GREEN,
      GlassColor.GRAY,
      GlassColor.PINK,
      GlassColor.RED,
      GlassColor.YELLOW,
      GlassColor.BLUE,
      GlassColor.LIGHT_GRAY);
  
  private static final Random random = new Random();
  
  private final ItemStack rainbowWool;
  
  private final Language language;
  
  public DropRainbowWool(Language language) {
    ItemStack stack = Items.wool(GlassColor.WHITE).stack(64);
    ItemMeta meta = stack.getItemMeta();
    meta.setDisplayName(language.getRainbowWoolName());
    meta.setLore(language.getRainbowWoolLore());
    stack.setItemMeta(meta);
    this.rainbowWool = NBTUtils.applyNBTTags(stack, "rainbowWool");
    this.language = language;
  }
  
  public static Items.Type randomWool() {
    return Items.wool(colors.get(random.nextInt(colors.size())));
  }
  
  public void drop(Location location, Player player) {
    location.getWorld().dropItemNaturally(location, this.rainbowWool);
    this.language.sendReceivedItemMessage(player, this.language.getRainbowWoolName(), 64);
  }
}
