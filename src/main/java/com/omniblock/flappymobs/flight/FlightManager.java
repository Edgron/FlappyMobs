package com.omniblock.flappymobs.flight;

import com.omniblock.flappymobs.FlappyMobs;
import com.omniblock.flappymobs.config.ConfigManager;
import com.omniblock.flappymobs.messages.MessagesManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class FlightManager implements Listener {

    private final FlappyMobs plugin;
    private final MessagesManager messagesManager;
    private final Map<String, Flight> flights;
    private final Map<UUID, FlightSession> activeSessions;
    private final Map<UUID, Flight> creatingFlights;
    private final Map<UUID, ParachuteData> activeParachutes;
    private final Map<UUID, Long> parachuteStartTime;
    private final Set<UUID> maceProtection;

    public FlightManager(FlappyMobs plugin) {
        this.plugin = plugin;
        this.messagesManager = plugin.getMessagesManager();
        this.flights = new HashMap<>();
        this.activeSessions = new HashMap<>();
        this.creatingFlights = new HashMap<>();
        this.activeParachutes = new HashMap<>();
        this.parachuteStartTime = new HashMap<>();
        this.maceProtection = new HashSet<>();
        loadFlights();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private static class ParachuteData {
        private final Player player;
        private final Chicken chicken;
        private final BukkitTask soundTask;
        private final Flight flight;
        private int duration;
        private int ticksAlive;

        public ParachuteData(Player player, Chicken chicken, BukkitTask soundTask, Flight flight, int duration) {
            this.player = player;
            this.chicken = chicken;
            this.soundTask = soundTask;
            this.flight = flight;
            this.duration = duration;
            this.ticksAlive = 0;
        }

        public Player getPlayer() { return player; }
        public Chicken getChicken() { return chicken; }
        public BukkitTask getSoundTask() { return soundTask; }
        public Flight getFlight() { return flight; }
        public int getDuration() { return duration; }
        public void decrementDuration() { this.duration--; }
        public int getTicksAlive() { return ticksAlive; }
        public void incrementTicksAlive() { this.ticksAlive++; }
    }

    private String stripAllColors(String text) {
        if (text == null) return null;
        return text.replaceAll("[&§][0-9a-fk-or]", "")
            .replaceAll("&#[0-9A-Fa-f]{6}", "")
            .replaceAll("#[0-9A-Fa-f]{6}", "");
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
                EntityType creature = EntityType.valueOf(config.getString(path + ".creature", "PHANTOM"));
                double cost = config.getDouble(path + ".cost", 0.0);

                Flight flight = new Flight(flightName, creature, cost);
                flight.setInvulnerable(config.getBoolean(path + ".invulnerable", true));
                flight.setParachuteTime(config.getInt(path + ".parachute", 5));
                flight.setAllowEnderpearlInFlight(config.getBoolean(path + ".allow_enderpearl_in_flight", false));
                flight.setAllowEnderpearlInParachute(config.getBoolean(path + ".allow_enderpearl_in_parachute", false));
                flight.setAllowShiftDismount(config.getBoolean(path + ".allow_shift_dismount", true));

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
        updateAllSigns();
    }

    private void updateAllSigns() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String signKey = plugin.getConfigManager().getSignKey();

            String signKeyStripped = stripAllColors(
                    ChatColor.translateAlternateColorCodes('&', signKey))
                    .replace("[", "").replace("]", "").trim();

            int updatedSigns = 0;

            for (World world : plugin.getServer().getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    for (BlockState state : chunk.getTileEntities()) {
                        if (!(state instanceof Sign)) continue;

                        Sign sign = (Sign) state;
                        String line0 = sign.getLine(0);

                        if (line0 == null || line0.isEmpty()) continue;

                        String line0Stripped = stripAllColors(line0)
                                .replace("[", "").replace("]", "").trim();

                        if (!line0Stripped.equalsIgnoreCase(signKeyStripped)) continue;

                        String flightName = stripAllColors(sign.getLine(1)).trim();

                        if (flightName.isEmpty()) continue;

                        Flight flight = getFlight(flightName);

                        if (flight == null) {
                            if (plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().warning("[DEBUG] Sign found with flight name '" + flightName + "' but flight doesn't exist");
                            }
                            continue;
                        }

                        sign.setLine(0, plugin.getConfigManager().getSignLine0Color() + signKey);
                        sign.setLine(1, plugin.getConfigManager().getSignLine1Color() + flightName);
                        sign.setLine(2, plugin.getConfigManager().getSignLine2Color() + flight.getCreature().name());
                        sign.setLine(3, plugin.getConfigManager().getSignLine3Color() + plugin.getEconomyManager().formatAmount(flight.getCost()));
                        sign.update();
                        updatedSigns++;

                        if (plugin.getConfigManager().isDebugEnabled()) {
                            plugin.getLogger().info("[DEBUG] Updated sign for flight: " + flightName);
                        }
                    }
                }
            }

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Updated " + updatedSigns + " signs");
            }
        });
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String signKey = plugin.getConfigManager().getSignKey();

        String signKeyStripped = stripAllColors(
                ChatColor.translateAlternateColorCodes('&', signKey))
                .replace("[", "").replace("]", "").trim();

        String line0 = event.getLine(0);
        if (line0 == null || line0.isEmpty()) return;

        String line0Stripped = stripAllColors(line0)
                .replace("[", "").replace("]", "").trim();

        if (!line0Stripped.equalsIgnoreCase(signKeyStripped)) return;

        String flightName = event.getLine(1);
        if (flightName == null || flightName.isEmpty()) {
            messagesManager.sendMessage(player, "sign_invalid");
            return;
        }

        String flightNameStripped = stripAllColors(flightName).trim();

        Flight flight = getFlight(flightNameStripped);

        if (flight == null) {
            messagesManager.sendMessage(player, "flight_not_found", "flight", flightNameStripped);
            return;
        }

        event.setLine(0, plugin.getConfigManager().getSignLine0Color() + signKey);
        event.setLine(1, plugin.getConfigManager().getSignLine1Color() + flightNameStripped);
        event.setLine(2, plugin.getConfigManager().getSignLine2Color() + flight.getCreature().name());
        event.setLine(3, plugin.getConfigManager().getSignLine3Color() + plugin.getEconomyManager().formatAmount(flight.getCost()));
        event.getBlock().getState().update();

        messagesManager.sendMessage(player, "sign_created");

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Sign created for flight: " + flightNameStripped);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        if (!type.name().contains("SIGN")) return;

        if (!(block.getState() instanceof Sign)) return;

        Sign sign = (Sign) block.getState();
        Player player = event.getPlayer();

        String signKey = plugin.getConfigManager().getSignKey();

        String signKeyStripped = stripAllColors(
                ChatColor.translateAlternateColorCodes('&', signKey))
                .replace("[", "").replace("]", "").trim();

        String line0 = sign.getLine(0);
        if (line0 == null || line0.isEmpty()) return;

        String line0Stripped = stripAllColors(line0)
                .replace("[", "").replace("]", "").trim();

        if (!line0Stripped.equalsIgnoreCase(signKeyStripped)) return;

        String flightName = sign.getLine(1);
        if (flightName == null || flightName.isEmpty()) return;

        String flightNameStripped = stripAllColors(flightName).trim();

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] FlappyMobs sign clicked:");
            plugin.getLogger().info("[DEBUG]   Line 0 raw: '" + line0 + "'");
            plugin.getLogger().info("[DEBUG]   Line 0 stripped: '" + line0Stripped + "'");
            plugin.getLogger().info("[DEBUG]   Line 1 raw: '" + flightName + "'");
            plugin.getLogger().info("[DEBUG]   Line 1 stripped: '" + flightNameStripped + "'");
        }

        Flight flight = getFlight(flightNameStripped);

        if (flight == null) {
            messagesManager.sendMessage(player, "flight_not_found", "flight", flightNameStripped);
            return;
        }

        event.setCancelled(true);
        startFlight(player, flight);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Starting flight '" + flight.getName() + "' from sign click");
        }
    }

    public void saveFlight(Flight flight) {
        FileConfiguration config = plugin.getConfigManager().getFlightsConfig();
        String path = "flights." + flight.getName();

        config.set(path + ".creature", flight.getCreature().name());
        config.set(path + ".cost", flight.getCost());
        config.set(path + ".invulnerable", flight.isInvulnerable());
        config.set(path + ".parachute", flight.getParachuteTime());
        config.set(path + ".allow_enderpearl_in_flight", flight.isAllowEnderpearlInFlight());
        config.set(path + ".allow_enderpearl_in_parachute", flight.isAllowEnderpearlInParachute());
        config.set(path + ".allow_shift_dismount", flight.isAllowShiftDismount());

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
            messagesManager.sendMessage(player, "already_riding");
            return;
        }

        if (flight.getWaypoints().isEmpty()) {
            messagesManager.sendMessage(player, "error_generic");
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("[DEBUG] Flight '" + flight.getName() + "' has no waypoints!");
            }
            return;
        }

        if (flight.getCost() > 0 && !player.hasPermission("fp.nocost")) {
            if (!plugin.getEconomyManager().isEnabled()) {
                messagesManager.sendMessage(player, "error_economy");
                plugin.getLogger().warning("Economy is not enabled but flight has cost!");
                return;
            }

            if (!plugin.getEconomyManager().hasBalance(player, flight.getCost())) {
                messagesManager.sendMessage(player, "insufficient_funds", "cost", plugin.getEconomyManager().formatAmount(flight.getCost()));
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Player " + player.getName() + " has insufficient funds. Has: " + 
                        plugin.getEconomyManager().getBalance(player) + " Needs: " + flight.getCost());
                }
                return;
            }

            if (!plugin.getEconomyManager().withdraw(player, flight.getCost())) {
                messagesManager.sendMessage(player, "error_economy");
                return;
            }

            messagesManager.sendMessage(player, "payment_success", "cost", plugin.getEconomyManager().formatAmount(flight.getCost()));

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Charged " + flight.getCost() + " to player " + player.getName());
            }
        }

        playSound(player, player.getLocation(), "start");
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation(), 1);

        Waypoint firstWP = flight.getWaypoints().get(0);
        Location startLoc = firstWP.getAsLocation();
        player.teleport(startLoc);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Teleported player to first waypoint: " + startLoc);
        }

        Entity entity = player.getWorld().spawnEntity(startLoc, flight.getCreature());

        if (!(entity instanceof LivingEntity)) {
            entity.remove();
            messagesManager.sendMessage(player, "error_generic");
            return;
        }

        LivingEntity creature = (LivingEntity) entity;

        if (creature instanceof Phantom) {
            Phantom phantom = (Phantom) creature;
            phantom.setFireTicks(0);
            phantom.setShouldBurnInDay(false);
        }

        ConfigManager.CreatureConfig config = plugin.getConfigManager().getCreatureConfig(flight.getCreature());
        if (config != null) {
            creature.setMaxHealth(config.getHealth());
            creature.setHealth(config.getHealth());
            creature.setAI(false);
            creature.setGravity(false);
            creature.setInvulnerable(flight.isInvulnerable());
            creature.setSilent(config.isSilent());
            creature.setCollidable(false);

            AttributeInstance scaleAttr = creature.getAttribute(Attribute.SCALE);
            if (scaleAttr != null) {
                scaleAttr.setBaseValue(config.getScale());
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Set creature scale to: " + config.getScale());
                }
            }

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Creature config - Health: " + config.getHealth() + 
                    ", Speed: " + config.getSpeed() + ", Scale: " + config.getScale() + ", Silent: " + config.isSilent());
            }
        }

        creature.addPassenger(player);

        FlightSession session = new FlightSession(player, flight, creature);
        activeSessions.put(player.getUniqueId(), session);

        maceProtection.add(player.getUniqueId());

        session.setCurrentWaypointIndex(1);
        calculateMovement(session);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            moveCreature(session);
        }, 1L, 1L);

        session.setMovementTask(task);

        messagesManager.sendMessage(player, "flight_started");

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Flight started successfully. Total waypoints: " + flight.getWaypoints().size());
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

            if (!completed) {
                int parachuteTime = session.getFlight().getParachuteTime();
                if (parachuteTime >= 0) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        deployParachute(player, session.getFlight(), parachuteTime);
                    }, 5L);
                }
            } else {
                maceProtection.remove(player.getUniqueId());
            }

            creature.remove();
        }

        if (completed) {
            messagesManager.sendMessage(player, "dismount_success");
        }

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Flight ended for " + player.getName() + " (completed=" + completed + ")");
        }
    }

    public boolean isInFlight(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public void reload() {
        loadFlights();
    }

    public void cleanup() {
        for (FlightSession session : new ArrayList<>(activeSessions.values())) {
            endFlight(session.getPlayer(), false);
        }
        activeSessions.clear();
        creatingFlights.clear();

        for (UUID uuid : new ArrayList<>(activeParachutes.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeParachute(player);
            }
        }
        activeParachutes.clear();
        parachuteStartTime.clear();
        maceProtection.clear();
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

    // Aquí los métodos auxiliares como playSound, calculateMovement, moveCreature, deployParachute, removeParachute
    // son requeridos para que el plugin funcione correctamente, si deseas puedo agregarlos también.
}
