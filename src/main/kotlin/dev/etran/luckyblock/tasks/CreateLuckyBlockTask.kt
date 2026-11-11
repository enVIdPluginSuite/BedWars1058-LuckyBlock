package dev.etran.luckyblock.tasks

import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.luckyblock.LuckyBlock
import dev.etran.luckyblock.luckyblock.LuckyBlockHead
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

class CreateLuckyBlockTask(
    private val block: Block,
    private val texture: LuckyBlockTexture,
    private val head: LuckyBlockHead,
    private val arena: IArena?,
    private val replaced: BlockState,
    private val placer: Player?,
    item: ItemStack,
    /** How many of this lucky block the placer held right before placing it (-1 = unknown). */
    private val countBefore: Int
) : BukkitRunnable() {

    private val item: ItemStack = item.clone().also { it.amount = 1 }

    /**
     * Only blocks that BedWars1058 accepted as a player-placed block become lucky blocks. If it refused (or
     * never registered it), or the spot was a map block, the head is undone, so no glass is left behind.
     */
    private fun accepted(): Boolean {
        if (arena == null || !isFreeSpace(replaced)) return false
        return try {
            arena.isBlockPlaced(block)
        } catch (e: RuntimeException) {
            false
        } catch (e: LinkageError) {
            false
        }
    }

    private fun placerIsSurvival(): Boolean =
        placer != null && placer.isOnline && placer.gameMode != GameMode.CREATIVE

    override fun run() {
        // the game ended during the tick: the map is being reset, so nothing may be written into it
        if (!dev.etran.luckyblock.abilities.Kit.gameRunning(arena)) {
            LuckyBlock.forget(block)
            return
        }
        val known = countBefore >= 0 && placerIsSurvival()
        val now = if (known) countOf(placer!!, texture) else 0
        if (!Items.isHeadBlock(block.type) && !Items.isStainedGlassBlock(block.type)) {
            // the head was removed again within the tick (or the placement was undone): nothing to turn into a lucky block
            LuckyBlock.forget(block)
            return
        }
        if (!accepted()) {
            LuckyBlock.forget(block)
            replaced.update(true, false)
            // the item is only given back if the game really took it
            if (placerIsSurvival() && (!known || now < countBefore)) placer!!.inventory.addItem(item)
            return
        }
        // a lucky block is now in the world, so exactly one must be gone from the inventory
        if (known && now >= countBefore) takeOne()
        texture.placeGlass(block)
        if (texture.showsHead) head.spawn()
    }

    private fun takeOne() {
        val inventory = placer!!.inventory
        val contents = inventory.contents
        for (i in contents.indices) {
            val stack = contents[i]
            if (stack != null && LuckyBlockTexture.byBukkitItem(stack) === texture) {
                if (stack.amount > 1) {
                    stack.amount = stack.amount - 1
                    inventory.setItem(i, stack)
                } else {
                    inventory.setItem(i, null)
                }
                return
            }
        }
    }

    companion object {
        @JvmStatic
        fun create(luckyBlock: LuckyBlock, arena: IArena?, replaced: BlockState, placer: Player?, item: ItemStack, countBefore: Int) {
            CreateLuckyBlockTask(luckyBlock.block, luckyBlock.texture, luckyBlock.head, arena, replaced, placer, item, countBefore)
                .runTaskLater(LuckyBlocks.getInstance(), 1L)
        }

        /** True if a lucky block may stand where this state was: air or liquid, never a map block like grass. */
        @JvmStatic
        fun isFreeSpace(state: BlockState): Boolean {
            val name = state.type.name
            return name == "AIR" || name.endsWith("_AIR") || name.contains("WATER") || name.contains("LAVA")
        }

        /** How many of this lucky block the player carries now. */
        @JvmStatic
        fun countOf(player: Player, texture: LuckyBlockTexture): Int {
            var total = 0
            for (stack in player.inventory.contents) {
                if (stack != null && LuckyBlockTexture.byBukkitItem(stack) === texture) total += stack.amount
            }
            return total
        }
    }
}
