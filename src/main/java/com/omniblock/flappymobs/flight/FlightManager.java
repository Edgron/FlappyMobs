package com.omniblock.flappymobs.flight;

import com.omniblock.flappymobs.FlappyMobs;
import com.omniblock.flappymobs.config.ConfigManager;
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
    private final Map<String, Flight> flights;
    private final Map<UUID, FlightSession> activeSessions;
    private final Map<UUID, Flight> creatingFlights;
    private final Map<UUID, ParachuteData> activeParachutes;
    private final Map<UUID, Long> parachuteStartTime;
    private final Set<UUID> maceProtection;

    public FlightManager(FlappyMobs plugin) {
        this.plugin = plugin;
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
            String message = plugin.getMessagesManager().getPrefixedMessage("sign_invalid");
            if (message != null) player.sendMessage(message);
            return;
        }

        String flightNameStripped = stripAllColors(flightName).trim();

        Flight flight = getFlight(flightNameStripped);

        if (flight == null) {
            String message = plugin.getMessagesManager().getPrefixedMessage("flight_not_found", 
                "flight", flightNameStripped);
            if (message != null) player.sendMessage(message);
            return;
        }

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

        // IMPORTANTE: Solo procesar si ES un cartel de FlappyMobs
        if (!line0Stripped.equalsIgnoreCase(signKeyStripped)) return;

        // A partir de aquí, es un cartel de FlappyMobs
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
            // Solo mostrar error si ES cartel de FlappyMobs pero flight no existe
            String message = plugin.getMessagesManager().getPrefixedMessage("flight_not_found", 
                "flight", flightNameStripped);
            if (message != null) player.sendMessage(message);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("[DEBUG] Flight '" + flightNameStripped + "' not found in FlappyMobs sign!");
            }
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

    // Verificar si el jugador ya está en vuelo
    if (activeSessions.containsKey(player.getUniqueId())) {
        String message = plugin.getMessagesManager().getPrefixedMessage("already_riding");
        if (message != null) player.sendMessage(message);
        return;
    }

    // Validar waypoints
    if (flight.getWaypoints().isEmpty()) {
        String message = plugin.getMessagesManager().getPrefixedMessage("error_generic");
        if (message != null) player.sendMessage(message);
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().warning("[DEBUG] Flight '" + flight.getName() + "' has no waypoints!");
        }
        return;
    }

    // Crear sesión TEMPORAL para bloquear clics múltiples (sin entidad aún)
    FlightSession tempSession = new FlightSession(player, flight, null);
    activeSessions.put(player.getUniqueId(), tempSession);

    // Validar economía
    if (flight.getCost() > 0 && !player.hasPermission("fp.nocost")) {
        if (!plugin.getEconomyManager().isEnabled()) {
            activeSessions.remove(player.getUniqueId()); // Remover sesión temporal
            String message = plugin.getMessagesManager().getPrefixedMessage("error_economy");
            if (message != null) player.sendMessage(message);
            plugin.getLogger().warning("Economy is not enabled but flight has cost!");
            return;
        }

        if (!plugin.getEconomyManager().hasBalance(player, flight.getCost())) {
            activeSessions.remove(player.getUniqueId()); // Remover sesión temporal
            String message = plugin.getMessagesManager().getPrefixedMessage("insufficient_funds", 
                "cost", plugin.getEconomyManager().formatAmount(flight.getCost()));
            if (message != null) player.sendMessage(message);
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Player " + player.getName() + " has insufficient funds. Has: " + 
                    plugin.getEconomyManager().getBalance(player) + " Needs: " + flight.getCost());
            }
            return;
        }

        if (!plugin.getEconomyManager().withdraw(player, flight.getCost())) {
            activeSessions.remove(player.getUniqueId()); // Remover sesión temporal
            String message = plugin.getMessagesManager().getPrefixedMessage("error_economy");
            if (message != null) player.sendMessage(message);
            return;
        }

        String message = plugin.getMessagesManager().getPrefixedMessage("payment_success", 
            "cost", plugin.getEconomyManager().formatAmount(flight.getCost()));
        if (message != null) player.sendMessage(message);

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
        activeSessions.remove(player.getUniqueId()); // Remover sesión temporal
        String message = plugin.getMessagesManager().getPrefixedMessage("error_generic");
        if (message != null) player.sendMessage(message);
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

    // Actualizar sesión temporal con la entidad real
    FlightSession session = new FlightSession(player, flight, creature);
    activeSessions.put(player.getUniqueId(), session);

    maceProtection.add(player.getUniqueId());

    session.setCurrentWaypointIndex(1);
    calculateMovement(session);

    BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
        moveCreature(session);
    }, 1L, 1L);

    session.setMovementTask(task);

    String message = plugin.getMessagesManager().getPrefixedMessage("flight_started");
    if (message != null) player.sendMessage(message);

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

        if (creature instanceof Phantom) {
            creature.setFireTicks(0);
        }

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

        boolean isLastWaypoint = (wpIndex == flight.getWaypoints().size() - 1);

        double newX = currentLoc.getX() + session.getXPerTick();
        double newY = currentLoc.getY() + session.getYPerTick();
        double newZ = currentLoc.getZ() + session.getZPerTick();

        if (isLastWaypoint) {
            double distToTarget = currentLoc.distance(targetLoc);
            if (distToTarget < 1.5) {
                newY = targetLoc.getY();
            }
        }

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

        double tolerance = isLastWaypoint ? 1.0 : 2.5;
        double toleranceY = isLastWaypoint ? 1.0 : 3.5;

        if (distX <= tolerance && distY <= toleranceY && distZ <= tolerance) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Reached WP" + wpIndex + (isLastWaypoint ? " (LAST)" : "") + " - Moving to next waypoint");
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
            String message = plugin.getMessagesManager().getPrefixedMessage("dismount_success");
            if (message != null) player.sendMessage(message);
        }

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Flight ended for " + player.getName() + " (completed=" + completed + ")");
        }
    }

    private void deployParachute(Player player, Flight flight, int duration) {
        removeParachute(player);

        playSound(player, player.getLocation(), "parachute_deploy");

        Location chickenLoc = player.getLocation().add(0, 2, 0);
        Chicken chicken = (Chicken) player.getWorld().spawnEntity(chickenLoc, EntityType.CHICKEN);

        chicken.setAI(false);
        chicken.setGravity(false);
        chicken.setSilent(true);
        chicken.setInvulnerable(false);

        double chickenHealth = plugin.getConfigManager().getParachuteChickenHealth();
        double chickenScale = plugin.getConfigManager().getParachuteChickenScale();

        chicken.setMaxHealth(chickenHealth);
        chicken.setHealth(chickenHealth);
        chicken.setCollidable(true);

        AttributeInstance scaleAttr = chicken.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(chickenScale);
        }

        player.addPassenger(chicken);

        int effectDuration = duration == 0 ? Integer.MAX_VALUE : duration * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, effectDuration, 0, false, false));

        BukkitTask soundTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnGround() && !isInLiquid(player) && activeParachutes.containsKey(player.getUniqueId())) {
                playSound(player, player.getLocation(), "parachute_descent");
            }
        }, 0L, 20L);

        ParachuteData data = new ParachuteData(player, chicken, soundTask, flight, duration);
        activeParachutes.put(player.getUniqueId(), data);

        parachuteStartTime.put(player.getUniqueId(), System.currentTimeMillis());

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkParachute(player);
        }, 1L, 1L);

        String message = plugin.getMessagesManager().getPrefixedMessage("parachute_activated");
        if (message != null) player.sendMessage(message);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Parachute deployed for " + player.getName() + 
                " Duration: " + (duration == 0 ? "unlimited" : duration + "s"));
        }
    }

    private boolean isInLiquid(Player player) {
        Material blockType = player.getLocation().getBlock().getType();
        return blockType == Material.WATER || 
               blockType == Material.BUBBLE_COLUMN || 
               blockType == Material.LAVA;
    }

    private void checkParachute(Player player) {
        ParachuteData data = activeParachutes.get(player.getUniqueId());
        if (data == null) return;

        data.incrementTicksAlive();

        if (player.isGliding()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Player " + player.getName() + " deployed elytra - removing parachute");
            }
            removeParachute(player);
            maceProtection.remove(player.getUniqueId());
            String message = plugin.getMessagesManager().getPrefixedMessage("parachute_cancelled");
            if (message != null) player.sendMessage(message);
            return;
        }

        if (player.isOnGround() || isInLiquid(player)) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Player " + player.getName() + " touched ground/liquid - removing parachute");
            }

            Block groundBlock = player.getLocation().subtract(0, 1, 0).getBlock();
            playLandingSound(player, groundBlock);

            removeParachute(player);
            maceProtection.remove(player.getUniqueId());
            return;
        }

        if (!data.getChicken().isValid() || data.getChicken().isDead()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Parachute chicken destroyed for " + player.getName());
            }
            removeParachute(player);
            return;
        }

        if (!player.getPassengers().contains(data.getChicken())) {
            player.addPassenger(data.getChicken());
        }

        if (data.getDuration() > 0 && data.getTicksAlive() >= 20) {
            if (data.getTicksAlive() % 20 == 0) {
                data.decrementDuration();
                if (data.getDuration() <= 0) {
                    if (plugin.getConfigManager().isDebugEnabled()) {
                        plugin.getLogger().info("[DEBUG] Parachute duration expired for " + player.getName());
                    }
                    removeParachute(player);
                }
            }
        }
    }

    private void playLandingSound(Player player, Block block) {
        Material blockType = block.getType();
        String soundName = getBlockFallSound(blockType);

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, SoundCategory.BLOCKS, 0.3f, 1.0f);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Played landing sound: " + soundName);
            }
        } catch (IllegalArgumentException e) {
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_SMALL_FALL, SoundCategory.BLOCKS, 0.3f, 1.0f);
        }
    }

    private String getBlockFallSound(Material material) {
        switch (material) {
            case ANCIENT_DEBRIS: return "BLOCK_ANCIENT_DEBRIS_FALL";
            case ANVIL: case CHIPPED_ANVIL: case DAMAGED_ANVIL: return "BLOCK_ANVIL_FALL";
            case BAMBOO: case BAMBOO_SAPLING: return "BLOCK_BAMBOO_FALL";
            case BASALT: case SMOOTH_BASALT: case POLISHED_BASALT: return "BLOCK_BASALT_FALL";
            case BONE_BLOCK: return "BLOCK_BONE_BLOCK_FALL";
            case CHAIN: return "BLOCK_CHAIN_FALL";
            case GLASS: case GLASS_PANE: case TINTED_GLASS: return "BLOCK_GLASS_FALL";
            case GRASS_BLOCK: case SHORT_GRASS: case TALL_GRASS: return "BLOCK_GRASS_FALL";
            case GRAVEL: return "BLOCK_GRAVEL_FALL";
            case HONEY_BLOCK: return "BLOCK_HONEY_BLOCK_FALL";
            case LADDER: return "BLOCK_LADDER_FALL";
            case LANTERN: case SOUL_LANTERN: return "BLOCK_LANTERN_FALL";
            case LODESTONE: return "BLOCK_LODESTONE_FALL";
            case IRON_BLOCK: case GOLD_BLOCK: return "BLOCK_METAL_FALL";
            case NETHER_BRICKS: case CHISELED_NETHER_BRICKS: case CRACKED_NETHER_BRICKS: return "BLOCK_NETHER_BRICKS_FALL";
            case NETHER_GOLD_ORE: return "BLOCK_NETHER_GOLD_ORE_FALL";
            case NETHERITE_BLOCK: return "BLOCK_NETHERITE_BLOCK_FALL";
            case NETHERRACK: return "BLOCK_NETHERRACK_FALL";
            case CRIMSON_NYLIUM: case WARPED_NYLIUM: return "BLOCK_NYLIUM_FALL";
            case SAND: case RED_SAND: return "BLOCK_SAND_FALL";
            case SCAFFOLDING: return "BLOCK_SCAFFOLDING_FALL";
            case SHROOMLIGHT: return "BLOCK_SHROOMLIGHT_FALL";
            case SLIME_BLOCK: return "BLOCK_SLIME_BLOCK_FALL";
            case SNOW: case SNOW_BLOCK: return "BLOCK_SNOW_FALL";
            case SOUL_SAND: return "BLOCK_SOUL_SAND_FALL";
            case SOUL_SOIL: return "BLOCK_SOUL_SOIL_FALL";
            case STONE: case COBBLESTONE: case STONE_BRICKS: return "BLOCK_STONE_FALL";
            case NETHER_WART_BLOCK: case WARPED_WART_BLOCK: return "BLOCK_WART_BLOCK_FALL";
            case WEEPING_VINES: case WEEPING_VINES_PLANT: return "BLOCK_WEEPING_VINES_FALL";
            case OAK_WOOD: case SPRUCE_WOOD: case BIRCH_WOOD: case JUNGLE_WOOD: 
            case ACACIA_WOOD: case DARK_OAK_WOOD: case MANGROVE_WOOD: case CHERRY_WOOD:
            case OAK_PLANKS: case SPRUCE_PLANKS: case BIRCH_PLANKS: case JUNGLE_PLANKS:
            case ACACIA_PLANKS: case DARK_OAK_PLANKS: case MANGROVE_PLANKS: case CHERRY_PLANKS:
                return "BLOCK_WOOD_FALL";
            case WHITE_WOOL: case ORANGE_WOOL: case MAGENTA_WOOL: case LIGHT_BLUE_WOOL:
            case YELLOW_WOOL: case LIME_WOOL: case PINK_WOOL: case GRAY_WOOL:
            case LIGHT_GRAY_WOOL: case CYAN_WOOL: case PURPLE_WOOL: case BLUE_WOOL:
            case BROWN_WOOL: case GREEN_WOOL: case RED_WOOL: case BLACK_WOOL:
                return "BLOCK_WOOL_FALL";
            default:
                return "ENTITY_GENERIC_SMALL_FALL";
        }
    }

    private void removeParachute(Player player) {
        ParachuteData data = activeParachutes.remove(player.getUniqueId());
        if (data == null) return;

        player.stopSound(Sound.ITEM_ELYTRA_FLYING, SoundCategory.MASTER);

        if (data.getSoundTask() != null) {
            data.getSoundTask().cancel();
        }

        if (data.getChicken().isValid()) {
            data.getChicken().getWorld().spawnParticle(Particle.POOF, data.getChicken().getLocation(), 5, 0.3, 0.3, 0.3, 0.02);
            data.getChicken().remove();
        }

        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        parachuteStartTime.remove(player.getUniqueId());
    }

    @EventHandler
    public void onParachuteChickenDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Chicken)) return;

        Chicken chicken = (Chicken) event.getEntity();

        for (Map.Entry<UUID, ParachuteData> entry : activeParachutes.entrySet()) {
            if (entry.getValue().getChicken().equals(chicken)) {
                Player player = entry.getValue().getPlayer();
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Parachute chicken damaged for " + player.getName() + 
                        " - Damage: " + event.getDamage() + " - Health: " + chicken.getHealth());
                }

                if (chicken.getHealth() - event.getDamage() <= 0) {
                    removeParachute(player);
                    String message = plugin.getMessagesManager().getPrefixedMessage("parachute_destroyed");
                    if (message != null) player.sendMessage(message);
                }
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!event.isSneaking()) return;

        if (activeParachutes.containsKey(player.getUniqueId())) {
            Long startTime = parachuteStartTime.get(player.getUniqueId());
            if (startTime != null) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed < 1000) {
                    return;
                }
            }

            removeParachute(player);
            maceProtection.remove(player.getUniqueId());
            String message = plugin.getMessagesManager().getPrefixedMessage("parachute_cancelled");
            if (message != null) player.sendMessage(message);
            return;
        }

        FlightSession session = activeSessions.get(player.getUniqueId());
        if (session != null) {
            if (session.getFlight().isAllowShiftDismount()) {
                endFlight(player, false);
                String message = plugin.getMessagesManager().getPrefixedMessage("flight_dismounted");
                if (message != null) player.sendMessage(message);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        FlightSession session = activeSessions.get(player.getUniqueId());

        if (session == null) return;

        Entity dismounted = event.getDismounted();

        if (!dismounted.equals(session.getCreature())) return;

        if (!session.getFlight().isAllowShiftDismount()) {
            event.setCancelled(true);

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Prevented dismount for player " + player.getName() + 
                    " - allow_shift_dismount is false");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMaceAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();

        if (!maceProtection.contains(player.getUniqueId())) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.MACE) return;

        double originalDamage = event.getDamage();
        double baseDamage = 5.0;

        event.setDamage(baseDamage);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Mace protection: reduced damage from " + originalDamage + " to " + baseDamage);
        }
    }

    private void playSound(Player player, Location location, String type) {
        ConfigManager.SoundConfig soundConfig = plugin.getConfigManager().getSoundConfig(type);
        if (soundConfig == null || !soundConfig.isEnabled()) return;

        try {
            Sound sound = Sound.valueOf(soundConfig.getSound());
            SoundCategory category = SoundCategory.valueOf(soundConfig.getCategory());
            player.playSound(location, sound, category, soundConfig.getVolume(), soundConfig.getPitch());
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound configuration for: " + type);
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
