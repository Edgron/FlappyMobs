package com.omniblock.flappymobs.listeners;

import com.omniblock.flappymobs.FlappyMobs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.entity.Player;

public class PlayerListener implements Listener {
    private final FlappyMobs plugin;
    public PlayerListener(FlappyMobs plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (plugin.getFlightManager().isInFlight(p)) {
            plugin.getFlightManager().endFlight(p, false);
        }
    }
    @EventHandler
    public void onKick(PlayerKickEvent e) {
        Player p = e.getPlayer();
        if (plugin.getFlightManager().isInFlight(p)) {
            plugin.getFlightManager().endFlight(p, false);
        }
    }
}
