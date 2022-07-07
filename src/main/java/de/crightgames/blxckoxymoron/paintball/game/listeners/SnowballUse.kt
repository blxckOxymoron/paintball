package de.crightgames.blxckoxymoron.paintball.game.listeners

import de.crightgames.blxckoxymoron.paintball.Paintball
import de.crightgames.blxckoxymoron.paintball.Paintball.Companion.inWholeTicks
import de.crightgames.blxckoxymoron.paintball.game.Game
import de.crightgames.blxckoxymoron.paintball.game.Scores
import de.crightgames.blxckoxymoron.paintball.game.Scores.plusAssign
import de.crightgames.blxckoxymoron.paintball.config.ConfigTeam.Companion.team
import org.bukkit.Bukkit
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.entity.ThrowableProjectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.scheduler.BukkitTask
import java.util.*

class SnowballUse : Listener {

    companion object {
        fun refillSnowballsFor(player: Player, cancel: () -> Unit): Runnable {
            return Runnable {
                val itemsOnCursor = player.itemOnCursor.takeIf { it.isSimilar(Game.projectileItem) }?.amount ?: 0
                if (!player.inventory.containsAtLeast(Game.projectileItem, 16 - itemsOnCursor)) {
                    player.inventory.addItem(Game.projectileItem)
                } else {
                    cancel()
                }
            }
        }
        val playersWithRefill = mutableListOf<UUID>()
    }

    @EventHandler
    fun onSnowballSpawn(e: ProjectileLaunchEvent) {

        val snowball = e.entity as? ThrowableProjectile ?: return
        if (!snowball.item.isSimilar(Game.projectileItem)) return

        val player = snowball.shooter as? Player ?: return

        if (Game.state != Game.GameState.RUNNING) {
            player.spawnParticle(Particle.FIREWORKS_SPARK, player.eyeLocation.clone().add(player.location.direction.clone().multiply(2)), 1, 0.0, 0.0, 0.0, 0.05)
            e.isCancelled = true
            return
        }

        if (!player.inventory.containsAtLeast(Game.projectileItem, 2)) {
            e.isCancelled = true
            return
        }

        val now = System.currentTimeMillis()

        val isOnCooldown = (
            now - (Paintball.lastShot[player.uniqueId]?: -Paintball.gameConfig.durations["shot"]!!.inWholeMilliseconds)
                < Paintball.gameConfig.durations["shot"]!!.inWholeMilliseconds
        )

        if (isOnCooldown) {
            e.isCancelled = true
            return
        }

        player.playSound(player.location, Sound.ENTITY_TURTLE_EGG_CRACK, SoundCategory.MASTER, 100F, 1.5F)
        Scores.shotsObj?.getScore(player.name)?.plusAssign(1)

        val refillSpeed = (
            Paintball.gameConfig.durations["refill"]!!.inWholeTicks *
                ((player.team?.players?.size ?: Game.currentBiggestTeamSize).toDouble() / Game.currentBiggestTeamSize)
            ).toLong()

        if (!playersWithRefill.contains(player.uniqueId)) {
            playersWithRefill.add(player.uniqueId)
            var task: BukkitTask? = null

            task = Bukkit.getScheduler().runTaskTimer(
                Paintball.INSTANCE,
                refillSnowballsFor(player) {
                    playersWithRefill.remove(player.uniqueId)
                    task?.cancel()
                },
                refillSpeed,
                refillSpeed
            )
        }

        Paintball.lastShot[player.uniqueId] = now
    }
}