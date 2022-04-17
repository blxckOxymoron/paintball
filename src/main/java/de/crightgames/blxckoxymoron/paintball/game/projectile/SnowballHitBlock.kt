package de.crightgames.blxckoxymoron.paintball.game.projectile

import de.crightgames.blxckoxymoron.paintball.Paintball
import de.crightgames.blxckoxymoron.paintball.game.Game
import de.crightgames.blxckoxymoron.paintball.game.IncMaterial
import de.crightgames.blxckoxymoron.paintball.game.Scores
import de.crightgames.blxckoxymoron.paintball.game.Scores.plusAssign
import de.crightgames.blxckoxymoron.paintball.util.VectorUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Container
import org.bukkit.block.data.BlockData
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import kotlin.random.Random

class SnowballHitBlock : Listener {

    companion object {
        val showWithGlass: (Block) -> Unit = {
            it.type = when (it.type) {
                Material.LIME_STAINED_GLASS     -> Material.YELLOW_STAINED_GLASS
                Material.YELLOW_STAINED_GLASS   -> Material.ORANGE_STAINED_GLASS
                Material.ORANGE_STAINED_GLASS   -> Material.RED_STAINED_GLASS
                Material.RED_STAINED_GLASS      -> Material.MAGENTA_STAINED_GLASS
                Material.MAGENTA_STAINED_GLASS  -> Material.MAGENTA_STAINED_GLASS
                else -> Material.LIME_STAINED_GLASS
            }
        }

        private val noCopyData = listOf("lit")

        fun copyBlockDataJava(from: BlockData, to: Block) {

            try {
                val prevDataMap = from::class.java.methods.map { method ->
                    val name = method.name

                    return@map name.replace(Regex("^(get|is)"), "") to
                        if ((name.startsWith("get") || name.startsWith("is")) &&
                            method.parameterCount == 0
                        ) method.invoke(from)
                        else null
                }.filter { it.second != null }

                Bukkit.broadcastMessage(from.material.name)

                Bukkit.broadcastMessage(prevDataMap.joinToString("\n") {
                    it.first + " > " + if (it.second != null) it.second!!::class.simpleName else "null"
                })

                val newData = to.blockData

                newData::class.java.methods.forEach { method ->
                    val name = method.name
                    if (!name.startsWith("set")) return@forEach
                    val newDataVal = prevDataMap.find {
                        it.first == name.replace(Regex("^set"), "")
                    }?.second

                    if (newDataVal == null) Bukkit.broadcastMessage("no value for: $name")

                    if (
                        newDataVal != null &&
                        method.parameterCount == 1 &&
                        method.parameters.first().type.name == newDataVal::class.java.name
                    ) {
                        Bukkit.broadcastMessage("invoked: $name")
                        method.invoke(newData, newDataVal)
                    }
                }

                to.setBlockData(newData, false)

            } catch (e: IllegalArgumentException) {
                Bukkit.broadcastMessage(e.message ?: "ERROR")
            }

        }

        private fun copyBlockDataSting(from: BlockData, to: Block) {
            val fromBlockString = from.asString
                .replace(Regex("^[\\w:]*"), "")
                .replace(Regex("(,?${noCopyData.joinToString("|")})=[^,\\s\\]]*"), "")
                .replace(Regex("(?<=\\[),"), "")
                .replace(Regex("\\[]"), "")

            if (fromBlockString.isEmpty()) return

            try {
                // try a simple conversion
                val toBlockData = Bukkit.createBlockData("minecraft:" + to.type.name.lowercase() + fromBlockString)
                to.setBlockData(toBlockData, false)
            } catch (_: IllegalArgumentException) { }

        }

        fun replaceWithColor(color: IncMaterial): (Block) -> Pair<Boolean, IncMaterial?> {
            // returns true if block was replaced
            return replacer@{ block: Block ->
                if (
                    Paintball.gameConfig.noReplace.contains(block.type) ||
                    block.state is Container ||
                    block.isLiquid ||
                    block.isEmpty
                ) return@replacer false to null
                val prevData = block.blockData.clone()

                // to copy block data:
                // search for every get method in previous data
                // if a set method exists on the new data, use it
                // apply the new data
                val (replacement, prevTeam) = color.findReplacement(block.type)
                val sameBlock = replacement == prevData.material
                block.setType(replacement, false)


                copyBlockDataSting(prevData, block)

                return@replacer !sameBlock to prevTeam
            }
        }
    }


    @EventHandler
    fun onSnowballHit(e: ProjectileHitEvent) {

        val (player, team) = Game.checkProjectileEvent(e) ?: return

        val block = e.hitBlock ?: return

        //DEBATABLE: only replace blocks with line of sight
        val blocksAround = VectorUtils.vectorsInRadius(Paintball.gameConfig.colorRadius)
            .filter { Random.Default.nextDouble() < ((Paintball.gameConfig.colorRadius.toDouble() * .6) / it.length()) }
            .map { block.location.clone().add(it).block }

        val replacedColors = blocksAround.map(replaceWithColor(team.material)).filter { it.first }.map { it.second }

        val individualColoredScore = Scores.coloredIndividualObj?.getScore(player.name)
        if (individualColoredScore != null)
            individualColoredScore.score = individualColoredScore.score + replacedColors.size

        replacedColors.filterNotNull().forEach { inc ->
            Scores.coloredObj?.getScore(inc.name)?.plusAssign(-1)
        }
        Scores.coloredObj?.getScore(team.name)?.plusAssign(replacedColors.size)

    }
}