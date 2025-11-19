package com.omniblock.flappymobs.commands;

import com.omniblock.flappymobs.FlappyMobs;
import com.omniblock.flappymobs.messages.MessagesManager;
import com.omniblock.flappymobs.economy.EconomyManager;
import com.omniblock.flappymobs.flight.Flight;
import com.omniblock.flappymobs.flight.FlightManager;
import com.omniblock.flappymobs.flight.Waypoint;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class FlappyCommand implements CommandExecutor {
    private final FlappyMobs plugin;
    private final FlightManager flightManager;
    private final EconomyManager economyManager;
    private final MessagesManager messagesManager;

    public FlappyCommand(FlappyMobs plugin) {
        this.plugin = plugin;
        this.flightManager = plugin.getFlightManager();
        this.economyManager = plugin.getEconomyManager();
        this.messagesManager = plugin.getMessagesManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e/" + label + " help");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("flight")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messagesManager.getPrefixedMessage("player_only"));
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 2) {
                player.sendMessage(messagesManager.getPrefixedMessage("flight_not_found"));
                return true;
            }

            String name = args[1];
            Flight flight = flightManager.getFlight(name);

            if (flight == null) {
                player.sendMessage(messagesManager.getPrefixedMessage("flight_not_found", "flight", name));
                return true;
            }

            // FlightManager now handles payment internally
            flightManager.startFlight(player, flight);
            return true;
        }
		
        if (sub.equals("send")) {
            // Subcomando para enviar a otro jugador a vuelo específico (permiso flappymobs.send)
            if (!sender.hasPermission("flappymobs.send")) {
                sender.sendMessage(messagesManager.getPrefixedMessage("no_permission_send"));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(messagesManager.getPrefixedMessage("usage_fp_send"));
                return true;
            }
            String flightName = args[1];
            String targetName = args[2];
        
            Flight flight = flightManager.getFlight(flightName);
            if (flight == null) {
                sender.sendMessage(messagesManager.getPrefixedMessage("flight_not_found", "flight", flightName));
                return true;
            }
        
            Player target = plugin.getServer().getPlayerExact(targetName);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(messagesManager.getPrefixedMessage("player_not_found", "player", targetName));
                return true;
            }
        
            flightManager.startFlight(target, flight);
            sender.sendMessage(messagesManager.getPrefixedMessage("send_success", "player", targetName, "flight", flightName));
            target.sendMessage(messagesManager.getPrefixedMessage("send_received", "flight", flightName));
        
            return true;
        }
		
        if (sub.equals("dismount")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messagesManager.getPrefixedMessage("player_only"));
                return true;
            }

            Player player = (Player) sender;

            if (!flightManager.isInFlight(player)) {
                player.sendMessage(messagesManager.getPrefixedMessage("not_riding"));
                return true;
            }

            flightManager.endFlight(player, false);
            player.sendMessage(messagesManager.getPrefixedMessage("dismount_success"));
            return true;
        }

        if (sub.equals("list")) {
            sender.sendMessage(messagesManager.getPrefixedMessage("list_header"));

            if (flightManager.getAllFlights().isEmpty()) {
                sender.sendMessage(messagesManager.getPrefixedMessage("list_empty"));
            } else {
                for (Flight flight : flightManager.getAllFlights()) {
                    sender.sendMessage(messagesManager.getPrefixedMessage("list_entry", 
                        "flight", flight.getName(), 
                        "creature", flight.getCreature().name(), 
                        "cost", economyManager.formatAmount(flight.getCost())));
                }
            }

            sender.sendMessage(messagesManager.getPrefixedMessage("list_footer"));
            return true;
        }

        if (sub.equals("info")) {
            if (args.length < 2) {
                sender.sendMessage(messagesManager.getPrefixedMessage("info_usage"));
                return true;
            }

            String name = args[1];
            Flight flight = flightManager.getFlight(name);

            if (flight == null) {
                sender.sendMessage(messagesManager.getPrefixedMessage("flight_not_found", "flight", name));
                return true;
            }

            sender.sendMessage(messagesManager.getPrefixedMessage("info_header"));
            sender.sendMessage(messagesManager.getPrefixedMessage("info_name", "flight", flight.getName()));
            sender.sendMessage(messagesManager.getPrefixedMessage("info_creature", "creature", flight.getCreature().name()));
            sender.sendMessage(messagesManager.getPrefixedMessage("info_cost", "cost", economyManager.formatAmount(flight.getCost())));
            sender.sendMessage(messagesManager.getPrefixedMessage("info_waypoints", "waypoints", String.valueOf(flight.getWaypoints().size())));
            sender.sendMessage(messagesManager.getPrefixedMessage("info_invulnerable", "invulnerable", String.valueOf(flight.isInvulnerable())));
            sender.sendMessage(messagesManager.getPrefixedMessage("info_parachute", "parachute", String.valueOf(flight.getParachuteTime())));
            sender.sendMessage(messagesManager.getPrefixedMessage("info_footer"));
            return true;
        }

        // Admin commands
        if (sub.equals("create")) {
            if (!sender.hasPermission("fp.create")) {
                sender.sendMessage(messagesManager.getPrefixedMessage("no_permission"));
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(messagesManager.getPrefixedMessage("player_only"));
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 3) {
                player.sendMessage(messagesManager.getPrefixedMessage("create_usage"));
                return true;
            }

            String name = args[1];
            String mob = args[2];
            double cost = 0;

            if (args.length > 3) {
                try {
                    cost = Double.parseDouble(args[3]);
                } catch (Exception ignored) {
                    cost = 0;
                }
            }

            EntityType type;
            try {
                type = EntityType.valueOf(mob.toUpperCase());
            } catch (Exception e) {
                player.sendMessage(messagesManager.getPrefixedMessage("invalid_creature"));
                return true;
            }

            if (!plugin.getConfigManager().isCreatureEnabled(type)) {
                player.sendMessage(messagesManager.getPrefixedMessage("creature_disabled"));
                return true;
            }

            if (flightManager.getFlight(name) != null) {
                player.sendMessage(messagesManager.getPrefixedMessage("create_exists"));
                return true;
            }

            flightManager.startCreating(player, new Flight(name, type, cost));
            player.sendMessage(messagesManager.getPrefixedMessage("create_started", "flight", name));
            return true;
        }

        if (sub.equals("setwp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messagesManager.getPrefixedMessage("player_only"));
                return true;
            }

            Player player = (Player) sender;
            Flight creating = flightManager.getCreatingFlight(player);

            if (creating == null) {
                player.sendMessage(messagesManager.getPrefixedMessage("save_no_flight"));
                return true;
            }

            Location loc = player.getLocation();
            creating.addWaypoint(new Waypoint(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            player.sendMessage(messagesManager.getPrefixedMessage("waypoint_added", "number", String.valueOf(creating.getWaypoints().size())));
            return true;
        }

        if (sub.equals("remlastwp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messagesManager.getPrefixedMessage("player_only"));
                return true;
            }

            Player player = (Player) sender;
            Flight creating = flightManager.getCreatingFlight(player);

            if (creating == null) {
                player.sendMessage(messagesManager.getPrefixedMessage("save_no_flight"));
                return true;
            }

            if (creating.getWaypoints().isEmpty()) {
                player.sendMessage(messagesManager.getPrefixedMessage("waypoint_none_to_remove"));
                return true;
            }

            creating.removeLastWaypoint();
            player.sendMessage(messagesManager.getPrefixedMessage("waypoint_removed"));
            return true;
        }

        if (sub.equals("save")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messagesManager.getPrefixedMessage("player_only"));
                return true;
            }

            Player player = (Player) sender;
            Flight creating = flightManager.getCreatingFlight(player);

            if (creating == null) {
                player.sendMessage(messagesManager.getPrefixedMessage("save_no_flight"));
                return true;
            }

            if (creating.getWaypoints().size() < 2) {
                player.sendMessage(messagesManager.getPrefixedMessage("save_no_waypoints"));
                return true;
            }

            flightManager.saveFlight(creating);
            flightManager.stopCreating(player);
            player.sendMessage(messagesManager.getPrefixedMessage("save_success", 
                "flight", creating.getName(), 
                "waypoints", String.valueOf(creating.getWaypoints().size())));
            return true;
        }

        if (sub.equals("cancel")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messagesManager.getPrefixedMessage("player_only"));
                return true;
            }

            Player player = (Player) sender;
            flightManager.stopCreating(player);
            player.sendMessage(messagesManager.getPrefixedMessage("cancel_success"));
            return true;
        }

        if (sub.equals("delete")) {
            if (!sender.hasPermission("fp.delete")) {
                sender.sendMessage(messagesManager.getPrefixedMessage("no_permission"));
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(messagesManager.getPrefixedMessage("delete_usage"));
                return true;
            }

            String name = args[1];
            Flight f = flightManager.getFlight(name);

            if (f == null) {
                sender.sendMessage(messagesManager.getPrefixedMessage("flight_not_found", "flight", name));
                return true;
            }

            flightManager.deleteFlight(name);
            sender.sendMessage(messagesManager.getPrefixedMessage("delete_success", "flight", name));
            return true;
        }

        if (sub.equals("edit")) {
            if (!sender.hasPermission("fp.edit")) {
                sender.sendMessage(messagesManager.getPrefixedMessage("no_permission"));
                return true;
            }

            if (args.length < 4) {
                sender.sendMessage(messagesManager.getPrefixedMessage("edit_usage"));
                return true;
            }

            String name = args[1];
            String property = args[2];
            String value = args[3];

            Flight f = flightManager.getFlight(name);

            if (f == null) {
                sender.sendMessage(messagesManager.getPrefixedMessage("flight_not_found", "flight", name));
                return true;
            }

            switch (property.toLowerCase()) {
                case "creature":
                    try {
                        f.setCreature(EntityType.valueOf(value.toUpperCase()));
                    } catch (Exception e) {
                        sender.sendMessage(messagesManager.getPrefixedMessage("edit_invalid_value", "property", property));
                        return true;
                    }
                    break;
                case "cost":
                    try {
                        f.setCost(Double.parseDouble(value));
                    } catch (Exception e) {
                        sender.sendMessage(messagesManager.getPrefixedMessage("edit_invalid_value", "property", property));
                        return true;
                    }
                    break;
                case "invulnerable":
                    f.setInvulnerable(Boolean.parseBoolean(value));
                    break;
                case "parachute":
                    try {
                        f.setParachuteTime(Integer.parseInt(value));
                    } catch (Exception e) {
                        sender.sendMessage(messagesManager.getPrefixedMessage("edit_invalid_value", "property", property));
                        return true;
                    }
                    break;
                default:
                    sender.sendMessage(messagesManager.getPrefixedMessage("edit_invalid_property"));
                    return true;
            }

            flightManager.saveFlight(f);
            sender.sendMessage(messagesManager.getPrefixedMessage("edit_success", 
                "property", property, 
                "flight", name, 
                "value", value));
            return true;
        }

        if (sub.equals("removemobs")) {
            if (!sender.hasPermission("fp.removemobs")) {
                sender.sendMessage(messagesManager.getPrefixedMessage("no_permission"));
                return true;
            }

            int removed = flightManager.removeRiderlessCreatures();
            sender.sendMessage(messagesManager.getPrefixedMessage("removemobs_success", "count", String.valueOf(removed)));
            return true;
        }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("fp.reload")) {
                sender.sendMessage(messagesManager.getPrefixedMessage("no_permission"));
                return true;
            }

            plugin.reload();
            sender.sendMessage(messagesManager.getPrefixedMessage("reload_success"));
            return true;
        }

        sender.sendMessage("§eComando inválido. Usa /fp help");
        return true;
    }
}