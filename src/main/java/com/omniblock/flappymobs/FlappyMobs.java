package com.omniblock.flappymobs;

import com.omniblock.flappymobs.commands.FlappyCommand;
import com.omniblock.flappymobs.config.ConfigManager;
import com.omniblock.flappymobs.messages.MessagesManager;
import com.omniblock.flappymobs.economy.EconomyManager;
import com.omniblock.flappymobs.flight.FlightManager;
import com.omniblock.flappymobs.listeners.PlayerListener;
import com.omniblock.flappymobs.listeners.SignManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlappyMobs extends JavaPlugin {

    private static FlappyMobs instance;
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private FlightManager flightManager;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.messagesManager = new MessagesManager(this);
        this.flightManager = new FlightManager(this);
        this.economyManager = new EconomyManager(this);

        // Register commands
        getCommand("fp").setExecutor(new FlappyCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);

        getLogger().info("FlappyMobs has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cleanup active flights
        if (flightManager != null) {
            flightManager.cleanup();
        }

        getLogger().info("FlappyMobs has been disabled!");
    }

    public static FlappyMobs getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public FlightManager getFlightManager() {
        return flightManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public void reload() {
        configManager.reload();
        messagesManager.reload();
        flightManager.reload();
    }
}