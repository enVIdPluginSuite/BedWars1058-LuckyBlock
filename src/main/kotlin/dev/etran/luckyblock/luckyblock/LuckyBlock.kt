package dev.etran.luckyblock.luckyblock

import com.andrei1058.bedwars.api.arena.IArena
import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.drops.Drop
import dev.etran.luckyblock.luckyblock.texture.LuckyBlockTexture
import dev.etran.luckyblock.tasks.CreateLuckyBlockTask
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class LuckyBlock(val block: Block, val texture: LuckyBlockTexture, @Suppress("UNUSED_PARAMETER") arena: IArena?) {
    val head: LuckyBlockHead = LuckyBlockHead(block.location.add(0.5, -1.25, 0.5), texture)

    companion object {
        private val blocks: MutableMap<Block, LuckyBlock> = HashMap()

        @JvmStatic
        fun createLuckyBlock(block: Block, itemInHand: ItemStack, replaced: BlockState, placer: Player?, before: Int) {
            val arena = LuckyBlocks.getBedWars().arenaUtil.getArenaByIdentifier(block.world.name)
            val texture = LuckyBlockTexture.byBukkitItem(itemInHand)
            // lucky blocks only exist inside games, and only on free space (never on map blocks like grass)
            if (arena == null || texture == null || !CreateLuckyBlockTask.isFreeSpace(replaced)) return
            val luckyBlock = LuckyBlock(block, texture, arena)
            blocks[block] = luckyBlock
            CreateLuckyBlockTask.create(luckyBlock, arena, replaced, placer, itemInHand, before)
        }

        /** Drops a lucky block that never got created (its head block disappeared). */
        @JvmStatic
        fun forget(block: Block) {
            blocks.remove(block)
        }

        @JvmStatic
        fun destroyLuckyBlock(block: Block, player: Player) {
            val luckyBlock = blocks.remove(block) ?: return
            luckyBlock.head.destroy()
            // every rarity has its own loot table (see "loot:" in config.yml)
            val drop = Drop.randomDrop(luckyBlock.texture.rarity)
            drop?.drop(block.location, player)
        }

        /** Forgets the lucky blocks placed in one world (used when a game ends and the map is reset). */
        @JvmStatic
        fun destroyAllIn(worldName: String) {
            val iterator = blocks.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (entry.key.world.name == worldName) {
                    entry.value.head.destroy()
                    iterator.remove()
                }
            }
        }

        /** Removes every lucky block head (used when the plugin is disabled). */
        @JvmStatic
        fun destroyAll() {
            for (luckyBlock in blocks.values) luckyBlock.head.destroy()
            blocks.clear()
        }

        @JvmStatic
        fun isLuckyBlock(block: Block): Boolean = blocks.containsKey(block)
    }
}
