package dev.etran.luckyblock.drops

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.luckyblock.texture.GlassColor
import dev.etran.luckyblock.utils.NBTUtils
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Every drop a lucky block can give. The ids are the names used under "loot:" in config.yml.
 * Material, enchantment and potion names differ between Minecraft versions, so each is looked up with
 * the old and the new name.
 */
object DropCatalog {

    @JvmStatic
    fun registerAll(lang: Language) {
        // the drops with their own behavior
        Drop.registerDrop("fireball", DropFireball(lang))
        Drop.registerDrop("tnt", DropTnt(lang))
        Drop.registerDrop("wool", DropWool(lang))
        Drop.registerDrop("inventory_copier", DropInventoryCopier(lang))
        Drop.registerDrop("lightning_scroll", DropLightningScroll(lang))
        Drop.registerDrop("player_launcher", DropPlayerLauncher(lang))
        Drop.registerDrop("rainbow_wool", DropRainbowWool(lang))
        Drop.registerDrop("bridge_egg", DropBridgeEgg(lang))
        Drop.registerDrop("ice_bridge", DropIceBridge(lang))
        Drop.registerDrop("sleeping_dust", DropSleepingDust(lang))

        // resources
        add(lang, "iron_ingots", "Iron Ingots") { _, r -> one(stack("IRON_INGOT", 16 + r.nextInt(33))) }
        add(lang, "gold_ingots", "Gold Ingots") { _, r -> one(stack("GOLD_INGOT", 8 + r.nextInt(17))) }
        add(lang, "diamonds", "Diamonds") { _, r -> one(stack("DIAMOND", 2 + r.nextInt(4))) }
        add(lang, "emeralds", "Emeralds") { _, r -> one(stack("EMERALD", 1 + r.nextInt(3))) }

        // blocks
        add(lang, "clay", "Team Clay") { p, r -> one(named(Items.terracotta(teamColor(p)).stack(16 + r.nextInt(17)), "Team Clay")) }
        add(lang, "glass", "Team Glass") { p, r -> one(named(Items.stainedGlass(teamColor(p)).stack(12 + r.nextInt(13)), "Team Glass")) }
        add(lang, "wood_planks", "Wood Planks") { _, r -> one(stack(16 + r.nextInt(17), "OAK_PLANKS", "WOOD")) }
        add(lang, "end_stone", "End Stone") { _, r -> one(stack(12 + r.nextInt(13), "END_STONE", "ENDER_STONE")) }
        add(lang, "obsidian", "Obsidian") { _, r -> one(stack("OBSIDIAN", 2 + r.nextInt(5))) }
        add(lang, "ladders", "Ladders") { _, r -> one(stack("LADDER", 8 + r.nextInt(9))) }
        add(lang, "cobweb", "Cobwebs") { _, r -> one(stack(2 + r.nextInt(5), "COBWEB", "WEB")) }
        add(lang, "slime_block", "Slime Blocks") { _, r -> one(stack("SLIME_BLOCK", 2 + r.nextInt(4))) }
        add(lang, "sponge", "Sponges") { _, r -> one(stack("SPONGE", 4 + r.nextInt(5))) }

        // tools, weapons and armor
        add(lang, "arrows", "Arrows") { _, r -> one(stack("ARROW", 8 + r.nextInt(9))) }
        add(lang, "stone_sword", "Stone Sword") { _, _ -> one(stack("STONE_SWORD", 1)) }
        add(lang, "iron_sword", "Iron Sword") { _, _ -> one(stack("IRON_SWORD", 1)) }
        add(lang, "diamond_sword", "Diamond Sword") { _, _ -> one(stack("DIAMOND_SWORD", 1)) }
        add(lang, "sharp_sword", "Sharp Iron Sword") { _, _ ->
            one(named(enchant(stack("IRON_SWORD", 1), 2, "DAMAGE_ALL", "SHARPNESS"), "Sharp Iron Sword"))
        }
        add(lang, "knockback_stick", "Knockback Stick") { _, _ ->
            one(named(enchant(stack("STICK", 1), 2, "KNOCKBACK"), ChatColor.GOLD.toString() + "Knockback Stick"))
        }
        add(lang, "shears", "Shears") { _, _ -> one(stack("SHEARS", 1)) }
        add(lang, "iron_pickaxe", "Iron Pickaxe") { _, _ -> one(stack("IRON_PICKAXE", 1)) }
        add(lang, "iron_axe", "Iron Axe") { _, _ -> one(stack("IRON_AXE", 1)) }
        add(lang, "diamond_pickaxe", "Diamond Pickaxe") { _, _ -> one(stack("DIAMOND_PICKAXE", 1)) }
        add(lang, "bow_set", "Bow and Arrows") { _, r -> arrayOf(stack("BOW", 1), stack("ARROW", 12 + r.nextInt(9))) }
        // armor goes straight onto the player: leggings and boots only, and only while they live
        Drop.registerDrop("chainmail_armor", ArmorDrop(lang, "Chainmail Leggings & Boots", "CHAINMAIL", 0))
        Drop.registerDrop("iron_armor", ArmorDrop(lang, "Iron Leggings & Boots", "IRON", 0))
        Drop.registerDrop("diamond_armor", ArmorDrop(lang, "Diamond Leggings & Boots", "DIAMOND", 0))
        Drop.registerDrop("diamond_armor_prot", ArmorDrop(lang, "Protected Diamond Leggings & Boots", "DIAMOND", 2))

        // overpowered weapons
        add(lang, "super_punch_bow", "Super Punch Bow") { _, _ ->
            arrayOf(
                named(enchant(enchant(stack("BOW", 1), 5, "ARROW_KNOCKBACK", "PUNCH"), 1, "ARROW_INFINITE", "INFINITY"), ChatColor.RED.toString() + "Super Punch Bow"),
                stack("ARROW", 1)
            )
        }
        add(lang, "fire_bow", "Flame Bow") { _, _ ->
            arrayOf(
                named(enchant(enchant(stack("BOW", 1), 1, "ARROW_FIRE", "FLAME"), 2, "ARROW_DAMAGE", "POWER"), ChatColor.GOLD.toString() + "Flame Bow"),
                stack("ARROW", 16)
            )
        }
        add(lang, "op_sword", "Overpowered Sword") { _, _ ->
            one(named(enchant(enchant(stack("DIAMOND_SWORD", 1), 4, "DAMAGE_ALL", "SHARPNESS"), 2, "FIRE_ASPECT"), ChatColor.RED.toString() + "Overpowered Sword"))
        }
        add(lang, "lava_bucket", "Lava Bucket") { _, _ -> one(stack("LAVA_BUCKET", 1)) }
        add(lang, "op_cookie", "Overpowered Cookie") { _, _ ->
            one(tagged(stack("COOKIE", 1), "opCookie", ChatColor.GOLD.toString() + "Overpowered Cookie",
                ChatColor.GRAY.toString() + "Eat it for Strength, Speed,", ChatColor.GRAY.toString() + "Regeneration and Absorption."))
        }
        add(lang, "ghast_spawn", "Ghast Summoner") { _, _ ->
            one(tagged(stack("GHAST_TEAR", 1), "ghastSummon", ChatColor.WHITE.toString() + "Ghast Summoner",
                ChatColor.GRAY.toString() + "Right click to spawn a ghast", ChatColor.GRAY.toString() + "over the nearest enemy."))
        }
        add(lang, "creeper_spawn", "Creeper Summoner") { _, _ ->
            one(tagged(stack(1, "GUNPOWDER", "SULPHUR"), "creeperSummon", ChatColor.GREEN.toString() + "Creeper Summoner",
                ChatColor.GRAY.toString() + "Right click to spawn a creeper", ChatColor.GRAY.toString() + "next to the nearest enemy."))
        }

        Drop.registerDrop("tnt_rain", ActionDrop(Mobs::tntRain))
        Drop.registerDrop("wolf_pack", ActionDrop(Mobs::wolves))

        // food, buckets and utility
        add(lang, "golden_apple", "Golden Apples") { _, r -> one(stack("GOLDEN_APPLE", 1 + r.nextInt(3))) }
        add(lang, "enchanted_golden_apple", "Enchanted Golden Apple") { _, _ -> one(enchantedApple()) }
        add(lang, "water_bucket", "Water Bucket") { _, _ -> one(stack("WATER_BUCKET", 1)) }
        add(lang, "milk_bucket", "Magic Milk") { _, _ -> one(named(stack("MILK_BUCKET", 1), ChatColor.WHITE.toString() + "Magic Milk")) }
        add(lang, "ender_pearl", "Ender Pearl") { _, _ -> one(stack("ENDER_PEARL", 1)) }

        // potions
        add(lang, "speed_potion", "Speed Potion") { _, _ -> one(potion("Speed Potion", 45, 1, "SPEED")) }
        add(lang, "jump_potion", "Jump Boost Potion") { _, _ -> one(potion("Jump Boost Potion", 45, 4, "JUMP", "JUMP_BOOST")) }
        add(lang, "strength_potion", "Strength Potion") { _, _ -> one(potion("Strength Potion", 30, 0, "INCREASE_DAMAGE", "STRENGTH")) }
        add(lang, "regen_potion", "Regeneration Potion") { _, _ -> one(potion("Regeneration Potion", 20, 1, "REGENERATION")) }
        add(lang, "invisibility_potion", "Invisibility Potion") { _, _ -> one(potion("Invisibility Potion", 30, 0, "INVISIBILITY")) }

        // novel items (their effects are in PlayerInteractListener)
        add(lang, "swap_scroll", "Swap Scroll") { _, _ ->
            one(tagged(stack("PAPER", 1), "swapScroll", ChatColor.LIGHT_PURPLE.toString() + "Swap Scroll",
                ChatColor.GRAY.toString() + "Right click to swap places", ChatColor.GRAY.toString() + "with a random enemy."))
        }
        add(lang, "jump_feather", "Jump Feather") { _, r ->
            one(tagged(stack("FEATHER", 1 + r.nextInt(2)), "jumpFeather", ChatColor.WHITE.toString() + "Jump Feather",
                ChatColor.GRAY.toString() + "Right click to leap into the air."))
        }
        add(lang, "healing_totem", "Healing Totem") { _, _ ->
            one(tagged(stack("NETHER_STAR", 1), "healingTotem", ChatColor.GREEN.toString() + "Healing Totem",
                ChatColor.GRAY.toString() + "Right click to heal yourself", ChatColor.GRAY.toString() + "and nearby teammates."))
        }
    }

    private fun add(lang: Language, id: String, label: String, maker: SimpleDrop.Maker) {
        Drop.registerDrop(id, SimpleDrop(lang, label, maker))
    }

    private fun one(item: ItemStack): Array<ItemStack> = arrayOf(item)

    private fun stack(name: String, amount: Int): ItemStack = ItemStack(Items.material(name), amount)

    private fun stack(amount: Int, vararg names: String): ItemStack = ItemStack(Items.material(*names), amount)

    private fun named(item: ItemStack, name: String): ItemStack {
        val meta = item.itemMeta
        if (meta != null) {
            meta.displayName = name
            item.itemMeta = meta
        }
        return item
    }

    private fun tagged(item: ItemStack, tag: String, name: String, vararg lore: String): ItemStack {
        val meta = item.itemMeta
        meta.displayName = name
        meta.lore = lore.toList()
        item.itemMeta = meta
        return NBTUtils.applyNBTTags(item, tag)
    }

    private fun enchant(item: ItemStack, level: Int, vararg names: String): ItemStack {
        for (name in names) {
            val enchantment = Enchantment.getByName(name)
            if (enchantment != null) {
                item.addUnsafeEnchantment(enchantment, level)
                break
            }
        }
        return item
    }

    private fun enchantedApple(): ItemStack {
        val modern = Material.getMaterial("ENCHANTED_GOLDEN_APPLE")
        if (modern != null) return ItemStack(modern, 1)
        return ItemStack(Items.material("GOLDEN_APPLE"), 1, 1.toShort())
    }

    private fun potion(name: String, seconds: Int, amplifier: Int, vararg effectNames: String): ItemStack {
        val item = ItemStack(Items.material("POTION"), 1)
        val meta = item.itemMeta
        if (meta is PotionMeta) {
            for (effectName in effectNames) {
                val type = PotionEffectType.getByName(effectName)
                if (type != null) {
                    meta.addCustomEffect(PotionEffect(type, seconds * 20, amplifier), true)
                    break
                }
            }
        }
        meta.displayName = ChatColor.AQUA.toString() + name
        item.itemMeta = meta
        return item
    }

    /** The wool/clay/glass color of the player's BedWars team (white if they have none). */
    private fun teamColor(player: Player): GlassColor {
        try {
            val arena = LuckyBlocks.getBedWars().arenaUtil.getArenaByPlayer(player)
            val team = arena?.getTeam(player)
            if (team != null) return GlassColor.fromDye(team.color.dye())
        } catch (e: RuntimeException) {
            // fall through
        } catch (e: LinkageError) {
        }
        return GlassColor.WHITE
    }
}
