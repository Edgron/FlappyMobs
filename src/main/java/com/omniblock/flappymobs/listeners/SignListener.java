package com.omniblock.flappymobs.listeners;

import com.omniblock.flappymobs.FlappyMobs;
import com.omniblock.flappymobs.flight.Flight;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.SignChangeEvent;

public class SignListener implements Listener {
    private final FlappyMobs plugin;
    public SignListener(FlappyMobs plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onSignCreate(SignChangeEvent e) {
        if (!e.getPlayer().hasPermission("fp.sign.create")) return;
        if (e.getLine(0).equalsIgnoreCase("[FlappyMobs]")) {
            String name = e.getLine(1);
            Flight f = plugin.getFlightManager().getFlight(name);
            if (f == null) {
                e.getPlayer().sendMessage(plugin.getMessagesManager().getPrefixedMessage("flight_not_found", "flight", name));
                return;
            }
            e.setLine(0, ChatColor.translateAlternateColorCodes('&', plugin.getMessagesManager().getMessage("sign_header")));
            e.setLine(1, name);
            e.setLine(2, f.getCreature().name());
            e.setLine(3, plugin.getEconomyManager().formatAmount(f.getCost()));
            e.getPlayer().sendMessage(plugin.getMessagesManager().getPrefixedMessage("sign_created"));
        }
    }
    @EventHandler
    public void onSignInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Sign)) return;
        Sign sign = (Sign) block.getState();
        String line0 = ChatColor.stripColor(sign.getLine(0));
        if (line0 == null || !line0.equalsIgnoreCase("[FlappyMobs]")) return;
        String name = sign.getLine(1);
        Flight f = plugin.getFlightManager().getFlight(name);
        if (f == null) {
            e.getPlayer().sendMessage(plugin.getMessagesManager().getPrefixedMessage("flight_not_found", "flight", name));
            return;
        }
        e.getPlayer().performCommand("fp flight " + name);
    }
}
