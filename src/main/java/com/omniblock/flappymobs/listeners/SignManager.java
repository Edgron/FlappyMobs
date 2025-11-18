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
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignManager implements Listener {

    private final FlappyMobs plugin;

    public SignManager(FlappyMobs plugin) {
        this.plugin = plugin;
    }

    /**
     * Strip ALL color codes from text including:
     * - Legacy codes (&a, §c, &0-9, &a-f, &k-o, &r)
     * - Hex colors with & (&#FF5733)
     * - Hex colors without & (#FF5733)
     * 
     * @param text Text to strip colors from
     * @return Text without any color codes
     */
    private String stripAllColors(String text) {
        if (text == null) return null;

        return text
            .replaceAll("[&§][0-9a-fk-or]", "")           // Legacy codes (&a, §c, etc.)
            .replaceAll("&#[0-9A-Fa-f]{6}", "")            // Hex with & (&#FF5733)
            .replaceAll("#[0-9A-Fa-f]{6}", "");            // Hex without & (#FF5733)
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String signKey = plugin.getConfigManager().getSignKey();

        // Strip colors from sign key config
        String signKeyStripped = stripAllColors(
            ChatColor.translateAlternateColorCodes('&', signKey))
            .replace("[", "").replace("]", "").trim();

        // Strip colors from line 0
        String line0 = event.getLine(0);
        if (line0 == null || line0.isEmpty()) return;

        String line0Stripped = stripAllColors(line0)
            .replace("[", "").replace("]", "").trim();

        if (!line0Stripped.equalsIgnoreCase(signKeyStripped)) return;

        // Strip colors from flight name (line 1)
        String flightName = event.getLine(1);
        if (flightName == null || flightName.isEmpty()) {
            String message = plugin.getMessagesManager().getPrefixedMessage("sign_invalid");
            if (message != null) player.sendMessage(message);
            return;
        }

        String flightNameStripped = stripAllColors(flightName).trim();

        Flight flight = plugin.getFlightManager().getFlight(flightNameStripped);

        if (flight == null) {
            String message = plugin.getMessagesManager().getPrefixedMessage("flight_not_found", 
                "flight", flightNameStripped);
            if (message != null) player.sendMessage(message);
            return;
        }

        // Update sign with colors
        event.setLine(0, plugin.getConfigManager().getSignLine0Color() + signKey);
        event.setLine(1, plugin.getConfigManager().getSignLine1Color() + flightNameStripped);
        event.setLine(2, plugin.getConfigManager().getSignLine2Color() + flight.getCreature().name());
        event.setLine(3, plugin.getConfigManager().getSignLine3Color() + plugin.getEconomyManager().formatAmount(flight.getCost()));

        String message = plugin.getMessagesManager().getPrefixedMessage("sign_created");
        if (message != null) player.sendMessage(message);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Sign created for flight: " + flightNameStripped);
        }
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        if (!type.name().contains("SIGN")) return;

        if (!(block.getState() instanceof Sign)) return;

        Sign sign = (Sign) block.getState();
        Player player = event.getPlayer();

        String signKey = plugin.getConfigManager().getSignKey();

        // Strip colors from sign key config
        String signKeyStripped = stripAllColors(
            ChatColor.translateAlternateColorCodes('&', signKey))
            .replace("[", "").replace("]", "").trim();

        // Strip colors from line 0
        String line0 = sign.getLine(0);
        if (line0 == null || line0.isEmpty()) return;

        String line0Stripped = stripAllColors(line0)
            .replace("[", "").replace("]", "").trim();

        if (!line0Stripped.equalsIgnoreCase(signKeyStripped)) return;

        // Strip colors from flight name (line 1)
        String flightName = sign.getLine(1);
        if (flightName == null || flightName.isEmpty()) return;

        String flightNameStripped = stripAllColors(flightName).trim();

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Sign clicked:");
            plugin.getLogger().info("[DEBUG]   Line 0 raw: '" + line0 + "'");
            plugin.getLogger().info("[DEBUG]   Line 0 stripped: '" + line0Stripped + "'");
            plugin.getLogger().info("[DEBUG]   Line 1 raw: '" + flightName + "'");
            plugin.getLogger().info("[DEBUG]   Line 1 stripped: '" + flightNameStripped + "'");
            plugin.getLogger().info("[DEBUG]   Sign key config: '" + signKey + "'");
            plugin.getLogger().info("[DEBUG]   Sign key stripped: '" + signKeyStripped + "'");
        }

        Flight flight = plugin.getFlightManager().getFlight(flightNameStripped);

        if (flight == null) {
            String message = plugin.getMessagesManager().getPrefixedMessage("flight_not_found", 
                "flight", flightNameStripped);
            if (message != null) player.sendMessage(message);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("[DEBUG] Flight '" + flightNameStripped + "' not found!");
            }
            return;
        }

        event.setCancelled(true);
        plugin.getFlightManager().startFlight(player, flight);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Starting flight '" + flight.getName() + "' from sign click");
        }
    }
}