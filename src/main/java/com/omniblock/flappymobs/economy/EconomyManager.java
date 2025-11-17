package com.omniblock.flappymobs.economy;

import com.omniblock.flappymobs.FlappyMobs;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private final FlappyMobs plugin;
    private Economy economy;
    private boolean enabled;

    public EconomyManager(FlappyMobs plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfigManager().isEconomyEnabled();

        if (enabled) {
            setupEconomy();
        }
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found. Economy features disabled.");
            enabled = false;
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No economy provider found. Economy features disabled.");
            enabled = false;
            return false;
        }

        economy = rsp.getProvider();
        plugin.getLogger().info("Economy system enabled with " + economy.getName());
        return true;
    }

    public boolean isEnabled() {
        return enabled && economy != null;
    }

    public boolean hasBalance(Player player, double amount) {
        if (!isEnabled()) {
            return true;
        }

        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) {
            return true;
        }

        if (!hasBalance(player, amount)) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public String formatAmount(double amount) {
        if (!isEnabled()) {
            return plugin.getConfigManager().getCurrencySymbol() + String.format("%.2f", amount);
        }

        return economy.format(amount);
    }

    public Economy getEconomy() {
        return economy;
    }
}