package com.omniblock.flappymobs.economy;

import com.omniblock.flappymobs.FlappyMobs;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final FlappyMobs plugin;
    private Economy economy = null;
    private boolean enabled = false;

    public EconomyManager(FlappyMobs plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (!plugin.getConfigManager().isEconomyEnabled()) {
            plugin.getLogger().info("Economy is disabled in config.");
            return false;
        }

        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found! Economy features will be disabled.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No economy provider found! Make sure you have an economy plugin installed.");
            return false;
        }

        economy = rsp.getProvider();
        enabled = economy != null;

        if (enabled) {
            plugin.getLogger().info("Economy hooked successfully with " + economy.getName());
        } else {
            plugin.getLogger().warning("Failed to hook into economy provider!");
        }

        return enabled;
    }

    public boolean isEnabled() {
        return enabled && economy != null;
    }

    public boolean hasBalance(Player player, double amount) {
        if (!isEnabled()) return true; // Si no hay economía, permitir todo
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) return true;

        net.milkbowl.vault.economy.EconomyResponse response = economy.withdrawPlayer(player, amount);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] Withdraw: " + player.getName() + " - Amount: " + amount + 
                " - Success: " + response.transactionSuccess() + 
                " - Balance: " + response.balance);
        }

        return response.transactionSuccess();
    }

    public double getBalance(Player player) {
        if (!isEnabled()) return 0;
        return economy.getBalance(player);
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