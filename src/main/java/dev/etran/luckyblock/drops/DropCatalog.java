package dev.etran.luckyblock.drops;

import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.arena.team.ITeam;
import dev.etran.luckyblock.LuckyBlocks;
import dev.etran.luckyblock.compat.Items;
import dev.etran.luckyblock.data.Language;
import dev.etran.luckyblock.luckyblock.texture.GlassColor;
import dev.etran.luckyblock.utils.NBTUtils;
import java.util.Arrays;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Every drop a lucky block can give. The ids are the names used under "loot:" in config.yml.
 * Material, enchantment and potion names differ between Minecraft versions, so each is looked up with
 * the old and the new name.
 */
public final class DropCatalog {
  private DropCatalog() {}

  public static void registerAll(Language lang) {
    // the drops with their own behavior
    Drop.registerDrop("fireball", new DropFireball(lang));
    Drop.registerDrop("tnt", new DropTnt(lang));
    Drop.registerDrop("wool", new DropWool(lang));
    Drop.registerDrop("inventory_copier", new DropInventoryCopier(lang));
    Drop.registerDrop("lightning_scroll", new DropLightningScroll(lang));
    Drop.registerDrop("player_launcher", new DropPlayerLauncher(lang));
    Drop.registerDrop("rainbow_wool", new DropRainbowWool(lang));
    Drop.registerDrop("bridge_egg", new DropBridgeEgg(lang));
    Drop.registerDrop("ice_bridge", new DropIceBridge(lang));
    Drop.registerDrop("sleeping_dust", new DropSleepingDust(lang));

    // resources
    add(lang, "iron_ingots", "Iron Ingots", (p, r) -> one(stack("IRON_INGOT", 16 + r.nextInt(33))));
    add(lang, "gold_ingots", "Gold Ingots", (p, r) -> one(stack("GOLD_INGOT", 8 + r.nextInt(17))));
    add(lang, "diamonds", "Diamonds", (p, r) -> one(stack("DIAMOND", 2 + r.nextInt(4))));
    add(lang, "emeralds", "Emeralds", (p, r) -> one(stack("EMERALD", 1 + r.nextInt(3))));

    // blocks
    add(lang, "clay", "Team Clay", (p, r) -> one(named(Items.terracotta(teamColor(p)).stack(16 + r.nextInt(17)), "Team Clay")));
    add(lang, "glass", "Team Glass", (p, r) -> one(named(Items.stainedGlass(teamColor(p)).stack(12 + r.nextInt(13)), "Team Glass")));
    add(lang, "wood_planks", "Wood Planks", (p, r) -> one(stack(16 + r.nextInt(17), "OAK_PLANKS", "WOOD")));
    add(lang, "end_stone", "End Stone", (p, r) -> one(stack(12 + r.nextInt(13), "END_STONE", "ENDER_STONE")));
    add(lang, "obsidian", "Obsidian", (p, r) -> one(stack("OBSIDIAN", 2 + r.nextInt(5))));
    add(lang, "ladders", "Ladders", (p, r) -> one(stack("LADDER", 8 + r.nextInt(9))));
    add(lang, "cobweb", "Cobwebs", (p, r) -> one(stack(2 + r.nextInt(5), "COBWEB", "WEB")));
    add(lang, "slime_block", "Slime Blocks", (p, r) -> one(stack("SLIME_BLOCK", 2 + r.nextInt(4))));
    add(lang, "sponge", "Sponges", (p, r) -> one(stack("SPONGE", 4 + r.nextInt(5))));

    // tools, weapons and armor
    add(lang, "arrows", "Arrows", (p, r) -> one(stack("ARROW", 8 + r.nextInt(9))));
    add(lang, "stone_sword", "Stone Sword", (p, r) -> one(stack("STONE_SWORD", 1)));
    add(lang, "iron_sword", "Iron Sword", (p, r) -> one(stack("IRON_SWORD", 1)));
    add(lang, "diamond_sword", "Diamond Sword", (p, r) -> one(stack("DIAMOND_SWORD", 1)));
    add(lang, "sharp_sword", "Sharp Iron Sword", (p, r) -> one(named(enchant(stack("IRON_SWORD", 1), 2, "DAMAGE_ALL", "SHARPNESS"), "Sharp Iron Sword")));
    add(lang, "knockback_stick", "Knockback Stick", (p, r) -> one(named(enchant(stack("STICK", 1), 2, "KNOCKBACK"), ChatColor.GOLD + "Knockback Stick")));
    add(lang, "shears", "Shears", (p, r) -> one(stack("SHEARS", 1)));
    add(lang, "iron_pickaxe", "Iron Pickaxe", (p, r) -> one(stack("IRON_PICKAXE", 1)));
    add(lang, "iron_axe", "Iron Axe", (p, r) -> one(stack("IRON_AXE", 1)));
    add(lang, "diamond_pickaxe", "Diamond Pickaxe", (p, r) -> one(stack("DIAMOND_PICKAXE", 1)));
    add(lang, "bow_set", "Bow and Arrows", (p, r) -> new ItemStack[] { stack("BOW", 1), stack("ARROW", 12 + r.nextInt(9)) });
    // armor goes straight onto the player: leggings and boots only, and only while they live
    Drop.registerDrop("chainmail_armor", new ArmorDrop(lang, "Chainmail Leggings & Boots", "CHAINMAIL", 0));
    Drop.registerDrop("iron_armor", new ArmorDrop(lang, "Iron Leggings & Boots", "IRON", 0));
    Drop.registerDrop("diamond_armor", new ArmorDrop(lang, "Diamond Leggings & Boots", "DIAMOND", 0));
    Drop.registerDrop("diamond_armor_prot", new ArmorDrop(lang, "Protected Diamond Leggings & Boots", "DIAMOND", 2));

    // overpowered weapons
    add(lang, "super_punch_bow", "Super Punch Bow", (p, r) -> new ItemStack[] {
        named(enchant(enchant(stack("BOW", 1), 5, "ARROW_KNOCKBACK", "PUNCH"), 1, "ARROW_INFINITE", "INFINITY"), ChatColor.RED + "Super Punch Bow"),
        stack("ARROW", 1) });
    add(lang, "fire_bow", "Flame Bow", (p, r) -> new ItemStack[] {
        named(enchant(enchant(stack("BOW", 1), 1, "ARROW_FIRE", "FLAME"), 2, "ARROW_DAMAGE", "POWER"), ChatColor.GOLD + "Flame Bow"),
        stack("ARROW", 16) });
    add(lang, "op_sword", "Overpowered Sword", (p, r) -> one(named(enchant(enchant(stack("DIAMOND_SWORD", 1), 4, "DAMAGE_ALL", "SHARPNESS"), 2, "FIRE_ASPECT"),
        ChatColor.RED + "Overpowered Sword")));
    add(lang, "lava_bucket", "Lava Bucket", (p, r) -> one(stack("LAVA_BUCKET", 1)));
    add(lang, "op_cookie", "Overpowered Cookie", (p, r) -> one(tagged(stack("COOKIE", 1), "opCookie", ChatColor.GOLD + "Overpowered Cookie",
        ChatColor.GRAY + "Eat it for Strength, Speed,", ChatColor.GRAY + "Regeneration and Absorption.")));
    add(lang, "ghast_spawn", "Ghast Summoner", (p, r) -> one(tagged(stack("GHAST_TEAR", 1), "ghastSummon", ChatColor.WHITE + "Ghast Summoner",
        ChatColor.GRAY + "Right click to spawn a ghast", ChatColor.GRAY + "over the nearest enemy.")));
    add(lang, "creeper_spawn", "Creeper Summoner", (p, r) -> one(tagged(stack(1, "GUNPOWDER", "SULPHUR"), "creeperSummon", ChatColor.GREEN + "Creeper Summoner",
        ChatColor.GRAY + "Right click to spawn a creeper", ChatColor.GRAY + "next to the nearest enemy.")));

    Drop.registerDrop("tnt_rain", new ActionDrop(Mobs::tntRain));
    Drop.registerDrop("wolf_pack", new ActionDrop(Mobs::wolves));

    // food, buckets and utility
    add(lang, "golden_apple", "Golden Apples", (p, r) -> one(stack("GOLDEN_APPLE", 1 + r.nextInt(3))));
    add(lang, "enchanted_golden_apple", "Enchanted Golden Apple", (p, r) -> one(enchantedApple()));
    add(lang, "water_bucket", "Water Bucket", (p, r) -> one(stack("WATER_BUCKET", 1)));
    add(lang, "milk_bucket", "Magic Milk", (p, r) -> one(named(stack("MILK_BUCKET", 1), ChatColor.WHITE + "Magic Milk")));
    add(lang, "ender_pearl", "Ender Pearl", (p, r) -> one(stack("ENDER_PEARL", 1)));

    // potions
    add(lang, "speed_potion", "Speed Potion", (p, r) -> one(potion("Speed Potion", 45, 1, "SPEED")));
    add(lang, "jump_potion", "Jump Boost Potion", (p, r) -> one(potion("Jump Boost Potion", 45, 4, "JUMP", "JUMP_BOOST")));
    add(lang, "strength_potion", "Strength Potion", (p, r) -> one(potion("Strength Potion", 30, 0, "INCREASE_DAMAGE", "STRENGTH")));
    add(lang, "regen_potion", "Regeneration Potion", (p, r) -> one(potion("Regeneration Potion", 20, 1, "REGENERATION")));
    add(lang, "invisibility_potion", "Invisibility Potion", (p, r) -> one(potion("Invisibility Potion", 30, 0, "INVISIBILITY")));

    // novel items (their effects are in PlayerInteractListener)
    add(lang, "swap_scroll", "Swap Scroll", (p, r) -> one(tagged(stack("PAPER", 1), "swapScroll", ChatColor.LIGHT_PURPLE + "Swap Scroll",
        ChatColor.GRAY + "Right click to swap places", ChatColor.GRAY + "with a random enemy.")));
    add(lang, "jump_feather", "Jump Feather", (p, r) -> one(tagged(stack("FEATHER", 1 + r.nextInt(2)), "jumpFeather", ChatColor.WHITE + "Jump Feather",
        ChatColor.GRAY + "Right click to leap into the air.")));
    add(lang, "healing_totem", "Healing Totem", (p, r) -> one(tagged(stack("NETHER_STAR", 1), "healingTotem", ChatColor.GREEN + "Healing Totem",
        ChatColor.GRAY + "Right click to heal yourself", ChatColor.GRAY + "and nearby teammates.")));
  }

  private static void add(Language lang, String id, String label, SimpleDrop.Maker maker) {
    Drop.registerDrop(id, new SimpleDrop(lang, label, maker));
  }

  private static ItemStack[] one(ItemStack item) {
    return new ItemStack[] { item };
  }

  private static ItemStack stack(String name, int amount) {
    return new ItemStack(Items.material(name), amount);
  }

  private static ItemStack stack(int amount, String... names) {
    return new ItemStack(Items.material(names), amount);
  }

  private static ItemStack named(ItemStack item, String name) {
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.setDisplayName(name);
      item.setItemMeta(meta);
    }
    return item;
  }

  private static ItemStack tagged(ItemStack item, String tag, String name, String... lore) {
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(name);
    meta.setLore(Arrays.asList(lore));
    item.setItemMeta(meta);
    return NBTUtils.applyNBTTags(item, tag);
  }

  private static ItemStack enchant(ItemStack item, int level, String... names) {
    for (String name : names) {
      Enchantment enchantment = Enchantment.getByName(name);
      if (enchantment != null) {
        item.addUnsafeEnchantment(enchantment, level);
        break;
      }
    }
    return item;
  }

  private static ItemStack enchantedApple() {
    Material modern = Material.getMaterial("ENCHANTED_GOLDEN_APPLE");
    if (modern != null)
      return new ItemStack(modern, 1);
    return new ItemStack(Items.material("GOLDEN_APPLE"), 1, (short) 1);
  }

  private static ItemStack potion(String name, int seconds, int amplifier, String... effectNames) {
    ItemStack item = new ItemStack(Items.material("POTION"), 1);
    ItemMeta meta = item.getItemMeta();
    if (meta instanceof PotionMeta) {
      for (String effectName : effectNames) {
        PotionEffectType type = PotionEffectType.getByName(effectName);
        if (type != null) {
          ((PotionMeta) meta).addCustomEffect(new PotionEffect(type, seconds * 20, amplifier), true);
          break;
        }
      }
    }
    meta.setDisplayName(ChatColor.AQUA + name);
    item.setItemMeta(meta);
    return item;
  }

  /** The wool/clay/glass color of the player's BedWars team (white if they have none). */
  private static GlassColor teamColor(Player player) {
    try {
      IArena arena = LuckyBlocks.getBedWars().getArenaUtil().getArenaByPlayer(player);
      ITeam team = (arena == null) ? null : arena.getTeam(player);
      if (team != null)
        return GlassColor.fromDye(team.getColor().dye());
    } catch (RuntimeException | LinkageError e) {
      // fall through
    }
    return GlassColor.WHITE;
  }
}
