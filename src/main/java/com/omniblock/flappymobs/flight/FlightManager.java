package com.omniblock.flappymobs.flight;

import com.omniblock.flappymobs.FlappyMobs;
import com.omniblock.flappymobs.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class FlightManager {

    private final FlappyMobs plugin;
    private final Map<String, Flight> flights;
    private final Map<UUID, FlightSession> activeSessions;
    private final Map<UUID, Flight> creatingFlights;

    public FlightManager(FlappyMobs plugin) {
        this.plugin = plugin;
        this.flights = new HashMap<>();
        this.activeSessions = new HashMap<>();
        this.creatingFlights = new HashMap<>();
        loadFlights();
    }

    public void loadFlights() {
        flights.clear();
        FileConfiguration config = plugin.getConfigManager().getFlightsConfig();

        if (config.getConfigurationSection("flights") == null) {
            plugin.getLogger().info("No flights configuration found.");
            return;
        }

        for (String flightName : config.getConfigurationSection("flights").getKeys(false)) {
            String path = "flights." + flightName;

            try {
                EntityType creature = EntityType.valueOf(config.getString(path + ".creature", "ENDER_DRAGON"));
                double cost = config.getDouble(path + ".cost", 0.0);

                Flight flight = new Flight(flightName, creature, cost);
                flight.setInvulnerable(config.getBoolean(path + ".invulnerable", true));
                flight.setParachuteTime(config.getInt(path + ".parachute", 5));

                List<String> waypointStrings = config.getStringList(path + ".waypoints");
                for (String wpString : waypointStrings) {
                    Waypoint wp = Waypoint.loadFromString(wpString);
                    if (wp != null) {
                        flight.addWaypoint(wp);
                    }
                }

                flights.put(flightName.toLowerCase(), flight);

                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Loaded flight: " + flightName + " with " + flight.getWaypoints().size() + " waypoints");
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading flight: " + flightName);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + flights.size() + " flights.");
    }

    public void saveFlight(Flight flight) {
        FileConfiguration config = plugin.getConfigManager().getFlightsConfig();
        String path = "flights." + flight.getName();

        config.set(path + ".creature", flight.getCreature().name());
        config.set(path + ".cost", flight.getCost());
        config.set(path + ".invulnerable", flight.isInvulnerable());
        config.set(path + ".parachute", flight.getParachuteTime());

        List<String> waypointStrings = new ArrayList<>();
        for (Waypoint wp : flight.getWaypoints()) {
            waypointStrings.add(wp.saveToString());
        }
        config.set(path + ".waypoints", waypointStrings);

        plugin.getConfigManager().saveFlightsConfig();
        flights.put(flight.getName().toLowerCase(), flight);
    }

    public void deleteFlight(String name) {
        FileConfiguration config = plugin.getConfigManager().getFlightsConfig();
        config.set("flights." + name, null);
        plugin.getConfigManager().saveFlightsConfig();
        flights.remove(name.toLowerCase());
    }

    public Flight getFlight(String name) {
        return flights.get(name.toLowerCase());
    }

    public Collection<Flight> getAllFlights() {
        return flights.values();
    }

    public void startCreating(Player player, Flight flight) {
        creatingFlights.put(player.getUniqueId(), flight);
    }

    public void stopCreating(Player player) {
        creatingFlights.remove(player.getUniqueId());
    }

    public Flight getCreatingFlight(Player player) {
        return creatingFlights.get(player.getUniqueId());
    }

    public boolean isCreating(Player player) {
        return creatingFlights.containsKey(player.getUniqueId());
    }

    public void startFlight(Player player, Flight flight) {
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Starting flight '" + flight.getName() + "' for player " + player.getName());
        }

        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("already_riding"));
            return;
        }

        if (flight.getWaypoints().isEmpty()) {
            player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("error_generic"));
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("[DEBUG] Flight '" + flight.getName() + "' has no waypoints!");
            }
            return;
        }

        // Check and charge cost
        if (flight.getCost() > 0 && !player.hasPermission("fp.nocost")) {
            if (!plugin.getEconomyManager().hasBalance(player, flight.getCost())) {
                player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("insufficient_funds", 
                    "cost", plugin.getEconomyManager().formatAmount(flight.getCost())));
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Player " + player.getName() + " has insufficient funds for flight");
                }
                return;
            }

            if (!plugin.getEconomyManager().withdraw(player, flight.getCost())) {
                player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("error_economy"));
                return;
            }

            player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("payment_success", 
                "cost", plugin.getEconomyManager().formatAmount(flight.getCost())));

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Charged " + flight.getCost() + " to player " + player.getName());
            }
        }

        // Teleport player to first waypoint
        Waypoint firstWP = flight.getWaypoints().get(0);
        Location startLoc = firstWP.getAsLocation();
        player.teleport(startLoc);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Teleported player to first waypoint: " + startLoc);
        }

        // Spawn creature at first waypoint
        Entity entity = player.getWorld().spawnEntity(startLoc, flight.getCreature());

        if (!(entity instanceof LivingEntity)) {
            entity.remove();
            player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("error_generic"));
            return;
        }

        LivingEntity creature = (LivingEntity) entity;

        // Apply creature configuration
        ConfigManager.CreatureConfig config = plugin.getConfigManager().getCreatureConfig(flight.getCreature());
        if (config != null) {
            creature.setMaxHealth(config.getHealth());
            creature.setHealth(config.getHealth());
            creature.setAI(false);
            creature.setGravity(false);
            creature.setInvulnerable(flight.isInvulnerable());
            creature.setSilent(true);
            creature.setCollidable(false);

            // Apply scale using GENERIC_SCALE attribute (Paper 1.21+)
            AttributeInstance scaleAttr = creature.getAttribute(Attribute.GENERIC_SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(config.getScale());
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Set creature scale to: " + config.getScale());
                }
            }

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Creature config - Health: " + config.getHealth() + 
                    ", Speed: " + config.getSpeed() + ", Scale: " + config.getScale());
            }
        }

        // Mount player
        creature.addPassenger(player);

        // Create session
        FlightSession session = new FlightSession(player, flight, creature);
        activeSessions.put(player.getUniqueId(), session);

        // Start from waypoint 1 (we're already at 0)
        session.setCurrentWaypointIndex(1);
        calculateMovement(session);

        // Start movement task (every tick = 50ms)
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            moveCreature(session);
        }, 1L, 1L);

        session.setMovementTask(task);

        player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("flight_started"));

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Flight started successfully. Total waypoints: " + flight.getWaypoints().size());
        }
    }

    private void calculateMovement(FlightSession session) {
        Flight flight = session.getFlight();
        int wpIndex = session.getCurrentWaypointIndex();

        if (wpIndex >= flight.getWaypoints().size()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] No more waypoints. Ending flight.");
            }
            return;
        }

        Location from = session.getCreature().getLocation();
        Location to = flight.getWaypoints().get(wpIndex).getAsLocation();

        double distX = to.getX() - from.getX();
        double distY = to.getY() - from.getY();
        double distZ = to.getZ() - from.getZ();

        ConfigManager.CreatureConfig config = plugin.getConfigManager().getCreatureConfig(flight.getCreature());
        double speed = config != null ? config.getSpeed() : 1.0;

        double distance = Math.sqrt(distX * distX + distY * distY + distZ * distZ);
        double blocksPerTick = speed * 0.15;
        double ticks = distance / blocksPerTick;

        if (ticks > 0) {
            session.setXPerTick(distX / ticks);
            session.setYPerTick(distY / ticks);
            session.setZPerTick(distZ / ticks);
        } else {
            session.setXPerTick(0);
            session.setYPerTick(0);
            session.setZPerTick(0);
        }

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] WP" + wpIndex + ": Distance=" + String.format("%.2f", distance) + 
                " Ticks=" + String.format("%.0f", ticks) + 
                " Speed=" + speed + 
                " From=" + from.getBlockX() + "," + from.getBlockY() + "," + from.getBlockZ() +
                " To=" + to.getBlockX() + "," + to.getBlockY() + "," + to.getBlockZ());
        }
    }

    private void moveCreature(FlightSession session) {
        Entity creature = session.getCreature();

        if (!creature.isValid() || creature.getPassengers().isEmpty()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Creature invalid or no passengers. Ending flight.");
            }
            endFlight(session.getPlayer(), false);
            return;
        }

        Flight flight = session.getFlight();
        int wpIndex = session.getCurrentWaypointIndex();

        if (wpIndex >= flight.getWaypoints().size()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Reached final waypoint. Completing flight.");
            }
            endFlight(session.getPlayer(), true);
            return;
        }

        Waypoint target = flight.getWaypoints().get(wpIndex);
        Location currentLoc = creature.getLocation();
        Location targetLoc = target.getAsLocation();

        double newX = currentLoc.getX() + session.getXPerTick();
        double newY = currentLoc.getY() + session.getYPerTick();
        double newZ = currentLoc.getZ() + session.getZPerTick();

        Location newLoc = new Location(currentLoc.getWorld(), newX, newY, newZ);

        Vector direction = targetLoc.toVector().subtract(currentLoc.toVector());
        if (direction.length() > 0) {
            direction.normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
            float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));
            newLoc.setYaw(yaw);
            newLoc.setPitch(pitch);
        }

        creature.teleport(newLoc);

        double distX = Math.abs(currentLoc.getX() - targetLoc.getX());
        double distY = Math.abs(currentLoc.getY() - targetLoc.getY());
        double distZ = Math.abs(currentLoc.getZ() - targetLoc.getZ());

        if (distX <= 2.5 && distY <= 3.5 && distZ <= 2.5) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Reached WP" + wpIndex + " - Moving to next waypoint");
            }
            session.setCurrentWaypointIndex(wpIndex + 1);
            calculateMovement(session);
        }
    }

    public void endFlight(Player player, boolean completed) {
        FlightSession session = activeSessions.remove(player.getUniqueId());

        if (session == null) {
            return;
        }

        session.cancel();
        Entity creature = session.getCreature();

        if (creature.isValid()) {
            creature.eject();

            if (session.getFlight().getParachuteTime() > 0) {
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW_FALLING,
                    session.getFlight().getParachuteTime() * 20,
                    0,
                    false,
                    false
                ));
                player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("parachute_activated"));
            }

            creature.remove();
        }

        if (completed) {
            player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("dismount_success"));
        }

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Flight ended for " + player.getName() + " (completed=" + completed + ")");
        }
    }

    public boolean isInFlight(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public void cleanup() {
        for (FlightSession session : new ArrayList<>(activeSessions.values())) {
            endFlight(session.getPlayer(), false);
        }
        activeSessions.clear();
        creatingFlights.clear();
    }

    public void reload() {
        loadFlights();
    }

    public int removeRiderlessCreatures() {
        int count = 0;
        for (FlightSession session : new ArrayList<>(activeSessions.values())) {
            Entity creature = session.getCreature();
            if (creature.getPassengers().isEmpty()) {
                endFlight(session.getPlayer(), false);
                count++;
            }
        }
        return count;
    }
}