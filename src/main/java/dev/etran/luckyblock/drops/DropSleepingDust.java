package dev.etran.luckyblock.drops;

import dev.etran.luckyblock.compat.Items;
import dev.etran.luckyblock.data.Language;
import dev.etran.luckyblock.luckyblock.texture.GlassColor;
import dev.etran.luckyblock.utils.NBTUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@RequiredArgsConstructor
public class DropSleepingDust extends Drop {

    private final ItemStack drop;
    private final Language language;

    public DropSleepingDust(Language language) {

        ItemStack stack = Items.carpet(GlassColor.PURPLE).stack(1);

        ItemMeta meta = stack.getItemMeta();

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Sleeping Dust");

        stack.setItemMeta(meta);

        this.drop = NBTUtils.applyNBTTags(stack, "sleepDust");
        this.language = language;
    }

    @Override
    public void drop(Location location, Player player) {

        ItemStack clone = drop.clone();

        int amount = getRandom().nextInt(2) + 1;

        clone.setAmount(amount);

        location.getWorld().dropItem(location, clone);

        if (player != null) {
            language.sendReceivedItemMessage(
                    player,
                    ChatColor.LIGHT_PURPLE + "Sleeping Dust",
                    amount
            );
        }
    }
}