package com.omniblock.flappymobs.economy;

import com.omniblock.flappymobs.FlappyMobs;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final FlappyMobs plugin;
    private Economy economy = null;

    public EconomyManager(FlappyMobs plugin) {
        this.plugin = plugin;
        if (!setupEconomy()) {
            plugin.getLogger().warning("Failed to setup economy! Make sure you have:");
            plugin.getLogger().warning("1. Vault installed");
            plugin.getLogger().warning("2. An economy plugin (EssentialsX, CMI, etc.)");
        }
    }

    private boolean setupEconomy() {
        if (!plugin.getConfigManager().isEconomyEnabled()) {
            plugin.getLogger().info("Economy is disabled in config.");
            return false;
        }

        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault not found! Economy features will be disabled.");
            plugin.getLogger().severe("Download Vault: https://www.spigotmc.org/resources/vault.34315/");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("No economy provider found!");
            plugin.getLogger().severe("Install an economy plugin like EssentialsX, CMI, or CraftConomy.");
            return false;
        }

        economy = rsp.getProvider();

        if (economy != null) {
            plugin.getLogger().info("Economy hooked successfully with: " + economy.getName());
            return true;
        } else {
            plugin.getLogger().severe("Failed to get economy provider from Vault!");
            return false;
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public boolean hasBalance(Player player, double amount) {
        if (!isEnabled()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("[DEBUG] Economy not enabled, allowing transaction");
            }
            return true;
        }

        OfflinePlayer offlinePlayer = player;
        boolean has = economy.has(offlinePlayer, amount);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Economy.has(" + player.getName() + ", " + amount + ") = " + has);
            plugin.getLogger().info("[DEBUG] Player balance: " + economy.getBalance(offlinePlayer));
        }

        return has;
    }

    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("[DEBUG] Economy not enabled, simulating successful withdrawal");
            }
            return true;
        }

        OfflinePlayer offlinePlayer = player;

        // Double check balance before withdrawal
        if (!economy.has(offlinePlayer, amount)) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().warning("[DEBUG] Pre-withdrawal check failed. Balance: " + 
                    economy.getBalance(offlinePlayer) + " Required: " + amount);
            }
            return false;
        }

        net.milkbowl.vault.economy.EconomyResponse response = economy.withdrawPlayer(offlinePlayer, amount);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Withdrawal attempt:");
            plugin.getLogger().info("[DEBUG]   Player: " + player.getName());
            plugin.getLogger().info("[DEBUG]   Amount: " + amount);
            plugin.getLogger().info("[DEBUG]   Success: " + response.transactionSuccess());
            plugin.getLogger().info("[DEBUG]   New Balance: " + response.balance);
            plugin.getLogger().info("[DEBUG]   Error Message: " + response.errorMessage);
        }

        return response.transactionSuccess();
    }

    public double getBalance(Player player) {
        if (!isEnabled()) return 0.0;

        OfflinePlayer offlinePlayer = player;
        return economy.getBalance(offlinePlayer);
    }

    public String formatAmount(double amount) {
        if (!isEnabled()) {
            return plugin.getConfigManager().getCurrencySymbol() + String.format("%.2f", amount);
        }
        return economy.format(amount);
    }

    public String getCurrencyName() {
        if (!isEnabled()) return "Money";
        return economy.currencyNamePlural();
    }
}