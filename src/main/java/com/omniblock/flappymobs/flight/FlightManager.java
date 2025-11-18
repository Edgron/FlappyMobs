package com.omniblock.flappymobs.flight;

import com.omniblock.flappymobs.FlappyMobs;
import com.omniblock.flappymobs.config.ConfigManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
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
    private final Map<UUID, Long> dismountConfirmations;

    public FlightManager(FlappyMobs plugin) {
        this.plugin = plugin;
        this.flights = new HashMap<>();
        this.activeSessions = new HashMap<>();
        this.creatingFlights = new HashMap<>();
        this.activeParachutes = new HashMap<>();
        this.dismountConfirmations = new HashMap<>();
        loadFlights();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private static class ParachuteData {
        private final Player player;
        private final Chicken chicken;
        private final BukkitTask soundTask;
        private int duration;

        public ParachuteData(Player player, Chicken chicken, BukkitTask soundTask, int duration) {
            this.player = player;
            this.chicken = chicken;
            this.soundTask = soundTask;
            this.duration = duration;
        }

        public Player getPlayer() { return player; }
        public Chicken getChicken() { return chicken; }
        public BukkitTask getSoundTask() { return soundTask; }
        public int getDuration() { return duration; }
        public void decrementDuration() { this.duration--; }
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
        updateAllSigns();
    }

    private void updateAllSigns() {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (World world : plugin.getServer().getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    for (BlockState tile : chunk.getTileEntities()) {
                        if (tile instanceof org.bukkit.block.Sign) {
                            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) tile;
                            String line0 = ChatColor.stripColor(sign.getLine(0));
                            if (line0 != null && line0.replace("[", "").replace("]", "").equalsIgnoreCase("FlappyMobs")) {
                                String flightName = sign.getLine(1);
                                Flight flight = getFlight(flightName);
                                if (flight != null) {
                                    sign.setLine(2, flight.getCreature().name());
                                    sign.setLine(3, plugin.getEconomyManager().formatAmount(flight.getCost()));
                                    sign.update();
                                }
                            }
                        }
                    }
                }
            }
        });
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
            if (!plugin.getEconomyManager().isEnabled()) {
                player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("error_economy"));
                plugin.getLogger().warning("Economy is not enabled but flight has cost!");
                return;
            }

            if (!plugin.getEconomyManager().hasBalance(player, flight.getCost())) {
                player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("insufficient_funds", 
                    "cost", plugin.getEconomyManager().formatAmount(flight.getCost())));
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Player " + player.getName() + " has insufficient funds. Has: " + 
                        plugin.getEconomyManager().getBalance(player) + " Needs: " + flight.getCost());
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

        // Play start sound
        playSound(player, player.getLocation(), "start");

        // Spawn GUST particle at player's feet
        player.getWorld().spawnParticle(Particle.GUST, player.getLocation(), 1);

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
            creature.setSilent(config.isSilent());
            creature.setCollidable(false);

            // Apply scale
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

        session.setCurrentWaypointIndex(1);
        calculateMovement(session);

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

        // Check if it's the last waypoint
        boolean isLastWaypoint = (wpIndex == flight.getWaypoints().size() - 1);

        double newX = currentLoc.getX() + session.getXPerTick();
        double newY = currentLoc.getY() + session.getYPerTick();
        double newZ = currentLoc.getZ() + session.getZPerTick();

        // For last waypoint, respect exact Y coordinate
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

            // Deploy parachute only if NOT completed (not at last waypoint)
            if (!completed) {
                int parachuteTime = session.getFlight().getParachuteTime();
                if (parachuteTime >= 0) {
                    deployParachute(player, parachuteTime);
                }
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

    private void deployParachute(Player player, int duration) {
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
            if (!player.isOnGround() && !isInWater(player) && activeParachutes.containsKey(player.getUniqueId())) {
                playSound(player, player.getLocation(), "parachute_descent");
            }
        }, 0L, 20L);

        ParachuteData data = new ParachuteData(player, chicken, soundTask, duration);
        activeParachutes.put(player.getUniqueId(), data);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            checkParachute(player);
        }, 1L, 1L);

        player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("parachute_activated"));

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Parachute deployed for " + player.getName() + 
                " Duration: " + (duration == 0 ? "unlimited" : duration + "s"));
        }
    }

    private boolean isInWater(Player player) {
        Material blockType = player.getLocation().getBlock().getType();
        return blockType == Material.WATER || blockType == Material.BUBBLE_COLUMN;
    }

    private void checkParachute(Player player) {
        ParachuteData data = activeParachutes.get(player.getUniqueId());
        if (data == null) return;

        // Check if player is on ground OR in water
        if (player.isOnGround() || isInWater(player)) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] Player " + player.getName() + " touched ground/water - removing parachute");
            }
            removeParachute(player);
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

        if (data.getDuration() > 0) {
            data.decrementDuration();
            if (data.getDuration() <= 0) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] Parachute duration expired for " + player.getName());
                }
                removeParachute(player);
            }
        }
    }

    private void removeParachute(Player player) {
        ParachuteData data = activeParachutes.remove(player.getUniqueId());
        if (data == null) return;

        // Stop sound explicitly
        player.stopSound(Sound.ITEM_ELYTRA_FLYING, SoundCategory.MASTER);

        if (data.getSoundTask() != null) {
            data.getSoundTask().cancel();
        }

        if (data.getChicken().isValid()) {
            // Spawn POOF particles
            data.getChicken().getWorld().spawnParticle(Particle.POOF, data.getChicken().getLocation(), 5, 0.3, 0.3, 0.3, 0.02);
            data.getChicken().remove();
        }

        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
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
                    player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("parachute_destroyed"));
                }
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (!event.isSneaking()) return;

        // Check if player is in parachute
        if (activeParachutes.containsKey(player.getUniqueId())) {
            removeParachute(player);
            player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("parachute_cancelled"));
            return;
        }

        // Check if player is in flight
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        long currentTime = System.currentTimeMillis();
        Long lastConfirm = dismountConfirmations.get(player.getUniqueId());

        if (lastConfirm != null && (currentTime - lastConfirm) <= 3000) {
            // Second shift press within 3 seconds - dismount
            dismountConfirmations.remove(player.getUniqueId());
            endFlight(player, false);
            player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("flight_dismounted"));
        } else {
            // First shift press - ask for confirmation
            dismountConfirmations.put(player.getUniqueId(), currentTime);
            player.sendMessage(plugin.getMessagesManager().getPrefixedMessage("dismount_confirm"));
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
        dismountConfirmations.clear();
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