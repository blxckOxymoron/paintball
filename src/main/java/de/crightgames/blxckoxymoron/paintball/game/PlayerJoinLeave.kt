package de.crightgames.blxckoxymoron.paintball.game

import de.crightgames.blxckoxymoron.paintball.Paintball
import de.crightgames.blxckoxymoron.paintball.util.ThemeBuilder
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerJoinLeave : Listener{

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        if (!e.player.isOp) e.player.gameMode = GameMode.SPECTATOR
        e.player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(ThemeBuilder.themed(
            "Willkommen zu *Paintball*, *${e.player.name}*!"
        )))

        val wasPlaying = Paintball.gameConfig.teams.any { tm -> tm.players.any { it.uniqueId == e.player.uniqueId } }

        if (!wasPlaying) Game.arenaWorld?.spawnLocation?.let { e.player.teleport(it) }

        if (Game.state == Game.GameState.RUNNING && !wasPlaying) Game.addSpectator(e.player)

        e.joinMessage = Game.getPlayerJoinMessage(e.player)

        Countdown.checkAndStart()
    }

    @EventHandler
    fun onPlayerLeave(e: PlayerQuitEvent) {
        e.quitMessage = Game.getPlayerLeaveMessage(e.player)
    }
}