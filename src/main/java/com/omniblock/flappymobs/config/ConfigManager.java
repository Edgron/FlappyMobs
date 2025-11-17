package com.omniblock.flappymobs.config;

import com.omniblock.flappymobs.FlappyMobs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final FlappyMobs plugin;
    private FileConfiguration config;
    private FileConfiguration flightsConfig;
    private File flightsFile;
    private Map<EntityType, CreatureConfig> creatureConfigs;

    public ConfigManager(FlappyMobs plugin) {
        this.plugin = plugin;
        this.creatureConfigs = new HashMap<>();
        loadConfigs();
    }

    public void loadConfigs() {
        // Save default config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Load flights.yml
        flightsFile = new File(plugin.getDataFolder(), "flights.yml");
        if (!flightsFile.exists()) {
            try {
                flightsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create flights.yml!");
                e.printStackTrace();
            }
        }
        flightsConfig = YamlConfiguration.loadConfiguration(flightsFile);

        // Load creature configs
        loadCreatureConfigs();
    }

    private void loadCreatureConfigs() {
        creatureConfigs.clear();

        for (String key : config.getConfigurationSection("creatures").getKeys(false)) {
            try {
                EntityType type = EntityType.valueOf(key);
                String path = "creatures." + key;

                boolean enabled = config.getBoolean(path + ".enabled", true);
                double scale = config.getDouble(path + ".scale", 1.0);
                double speed = config.getDouble(path + ".speed", 1.0);
                double health = config.getDouble(path + ".health", 20.0);
                boolean vulnerable = config.getBoolean(path + ".vulnerable", true);

                creatureConfigs.put(type, new CreatureConfig(enabled, scale, speed, health, vulnerable));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid creature type: " + key);
            }
        }
    }

    public void reload() {
        loadConfigs();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getFlightsConfig() {
        return flightsConfig;
    }

    public void saveFlightsConfig() {
        try {
            flightsConfig.save(flightsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save flights.yml!");
            e.printStackTrace();
        }
    }

    public CreatureConfig getCreatureConfig(EntityType type) {
        return creatureConfigs.get(type);
    }

    public boolean isCreatureEnabled(EntityType type) {
        CreatureConfig config = creatureConfigs.get(type);
        return config != null && config.isEnabled();
    }

    public int getDefaultParachuteTime() {
        return config.getInt("general.parachute_time", 5);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("general.debug", false);
    }

    public boolean isEconomyEnabled() {
        return config.getBoolean("economy.enabled", true);
    }

    public String getCurrencySymbol() {
        return config.getString("economy.currency_symbol", "$");
    }

    public static class CreatureConfig {
        private final boolean enabled;
        private final double scale;
        private final double speed;
        private final double health;
        private final boolean vulnerable;

        public CreatureConfig(boolean enabled, double scale, double speed, double health, boolean vulnerable) {
            this.enabled = enabled;
            this.scale = scale;
            this.speed = speed;
            this.health = health;
            this.vulnerable = vulnerable;
        }

        public boolean isEnabled() { return enabled; }
        public double getScale() { return scale; }
        public double getSpeed() { return speed; }
        public double getHealth() { return health; }
        public boolean isVulnerable() { return vulnerable; }
    }
}