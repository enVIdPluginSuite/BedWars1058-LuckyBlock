package dev.etran.luckyblock.drops

import dev.etran.luckyblock.LuckyBlocks
import dev.etran.luckyblock.compat.Items
import dev.etran.luckyblock.data.Language
import dev.etran.luckyblock.luckyblock.texture.GlassColor
import com.andrei1058.bedwars.api.arena.team.TeamColor
import org.bukkit.Location
import org.bukkit.entity.Player
import java.util.Locale

class DropWool(private val language: Language) : Drop() {

    override fun drop(location: Location, player: Player) {
        val arena = LuckyBlocks.getBedWars().arenaUtil.getArenaByPlayer(player) ?: return
        val team = arena.getTeam(player) ?: return
        val color = team.color
        // the team's wool color on every version (RED_WOOL on 1.13+, WOOL with data before)
        val wool = Items.wool(GlassColor.fromDye(color.dye())).stack(64)
        val meta = wool.itemMeta
        val name = prettyName(color) + " Wool"
        meta.displayName = name
        wool.itemMeta = meta
        location.world.dropItem(location, wool)
        language.sendReceivedItemMessage(player, name, 64)
    }

    // DARK_GREEN -> "Dark Green"
    private fun prettyName(color: TeamColor): String =
        color.name.split("_").joinToString(" ") { it.substring(0, 1).uppercase(Locale.ROOT) + it.substring(1).lowercase(Locale.ROOT) }
}
