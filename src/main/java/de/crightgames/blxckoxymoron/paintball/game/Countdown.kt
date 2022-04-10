package de.crightgames.blxckoxymoron.paintball.game

import de.crightgames.blxckoxymoron.paintball.Paintball
import de.crightgames.blxckoxymoron.paintball.Paintball.Companion.inWholeTicks
import de.crightgames.blxckoxymoron.paintball.util.ThemeBuilder
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object Countdown {
    private var TIMER_SPEED = 1.seconds

    private var currentTime = Duration.ZERO
    private var decreaseTask: BukkitTask? = null

    fun checkAndStart() {
        val onlinePlayerCount = Bukkit.getOnlinePlayers().size
        if (onlinePlayerCount >= Paintball.gameConfig.minimumPlayers && Paintball.gameConfig.autostart)
            start()

    }

    fun start() {
        if (decreaseTask?.isCancelled == false) return // already counting down
        currentTime = Paintball.gameConfig.durations["timer"]!!
        decreaseTask = Bukkit.getScheduler().runTaskTimer(Paintball.INSTANCE, decrease, 0, TIMER_SPEED.inWholeTicks)
    }

    private val decrease = Runnable {

        val allPlayers = Bukkit.getOnlinePlayers()

        val enoughPlayers = allPlayers.size >= Paintball.gameConfig.minimumPlayers
        if (!enoughPlayers) return@Runnable cancelStart()

        allPlayers.forEach { notifyPlayer(it) }

        currentTime -= TIMER_SPEED
        if (currentTime < Duration.ZERO) {
            startGame()
        }
    }

    private fun startGame() {
        decreaseTask?.cancel()
        Game.start()
    }

    private fun cancelStart() {
        decreaseTask?.cancel()

        Bukkit.broadcastMessage(ThemeBuilder.themed(":RED:Zu wenig Spieler - Start abgebrochen::"))
    }

    private fun notifyPlayer(player: Player) {
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, SoundCategory.MASTER, 100F, 0.5F)
        player.sendTitle(
            ThemeBuilder.themed("*${if (currentTime <= Duration.ZERO) "GO" else currentTime.inWholeSeconds}*"),
            ThemeBuilder.themed("`Paintball`"),
            0,
            21,
            2,
        )
    }
}