package com.omniblock.flappymobs.config;

import com.omniblock.flappymobs.FlappyMobs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final FlappyMobs plugin;
    private FileConfiguration flightsConfig;
    private File flightsFile;

    public ConfigManager(FlappyMobs plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        loadFlightsConfig();
    }

    private void loadFlightsConfig() {
        flightsFile = new File(plugin.getDataFolder(), "flights.yml");
        if (!flightsFile.exists()) {
            plugin.saveResource("flights.yml", false);
        }
        flightsConfig = YamlConfiguration.loadConfiguration(flightsFile);
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

    public void reloadFlightsConfig() {
        flightsConfig = YamlConfiguration.loadConfiguration(flightsFile);
    }

    public void reload() {
        plugin.reloadConfig();
        reloadFlightsConfig();
    }

    public boolean isDebugEnabled() {
        return plugin.getConfig().getBoolean("general.debug", false);
    }

    public boolean isEconomyEnabled() {
        return plugin.getConfig().getBoolean("economy.enabled", true);
    }

    public String getCurrencySymbol() {
        return plugin.getConfig().getString("economy.currency_symbol", "$");
    }

    public boolean isCreatureEnabled(EntityType type) {
        return plugin.getConfig().getBoolean("creatures." + type.name() + ".enabled", false);
    }

    public CreatureConfig getCreatureConfig(EntityType type) {
        String path = "creatures." + type.name();
        if (!plugin.getConfig().getBoolean(path + ".enabled", false)) {
            return null;
        }

        double scale = plugin.getConfig().getDouble(path + ".scale", 1.0);
        double speed = plugin.getConfig().getDouble(path + ".speed", 1.0);
        double health = plugin.getConfig().getDouble(path + ".health", 20.0);
        boolean vulnerable = plugin.getConfig().getBoolean(path + ".vulnerable", true);

        return new CreatureConfig(scale, speed, health, vulnerable);
    }

    public SoundConfig getSoundConfig(String type) {
        String path = "sounds." + type;
        if (!plugin.getConfig().contains(path)) {
            return null;
        }

        boolean enabled = plugin.getConfig().getBoolean(path + ".enabled", true);
        String sound = plugin.getConfig().getString(path + ".sound", "ENTITY_ENDER_DRAGON_FLAP");
        float volume = (float) plugin.getConfig().getDouble(path + ".volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble(path + ".pitch", 1.0);
        String category = plugin.getConfig().getString(path + ".category", "MASTER");

        return new SoundConfig(enabled, sound, volume, pitch, category);
    }

    public static class CreatureConfig {
        private final double scale;
        private final double speed;
        private final double health;
        private final boolean vulnerable;

        public CreatureConfig(double scale, double speed, double health, boolean vulnerable) {
            this.scale = scale;
            this.speed = speed;
            this.health = health;
            this.vulnerable = vulnerable;
        }

        public double getScale() { return scale; }
        public double getSpeed() { return speed; }
        public double getHealth() { return health; }
        public boolean isVulnerable() { return vulnerable; }
    }

    public static class SoundConfig {
        private final boolean enabled;
        private final String sound;
        private final float volume;
        private final float pitch;
        private final String category;

        public SoundConfig(boolean enabled, String sound, float volume, float pitch, String category) {
            this.enabled = enabled;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.category = category;
        }

        public boolean isEnabled() { return enabled; }
        public String getSound() { return sound; }
        public float getVolume() { return volume; }
        public float getPitch() { return pitch; }
        public String getCategory() { return category; }
    }
}