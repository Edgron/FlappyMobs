package com.omniblock.flappymobs.messages;

import com.omniblock.flappymobs.FlappyMobs;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;

public class MessagesManager {
    private final FlappyMobs plugin;
    private FileConfiguration messages;
    private File messagesFile;
    private String currentLanguage;

    public MessagesManager(FlappyMobs plugin) {
        this.plugin = plugin;
        this.currentLanguage = plugin.getConfig().getString("general.language", "es");
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder() + "/lang", currentLanguage + ".yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("lang/" + currentLanguage + ".yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        plugin.getLogger().info("Loaded language: " + currentLanguage);
    }

    private boolean isMessageEnabled(String key) {
        plugin.reloadConfig();
        boolean enabled = plugin.getConfig().getBoolean("messages." + key + ".enabled", true);
        if (plugin.getConfig().getBoolean("general.debug", false)) {
            plugin.getLogger().info("[DEBUG] Message 'messages." + key + ".enabled' = " + enabled);
        }
        return enabled;
    }

    public String getMessage(String key) {
        String message = messages.getString(key, "&cMessage not found: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefixedMessage(String key) {
        if (!isMessageEnabled(key)) {
            if (plugin.getConfig().getBoolean("general.debug", false)) {
                plugin.getLogger().info("[DEBUG] Message '" + key + "' is DISABLED, not sending");
            }
            return null;
        }
        String message = getMessage(key);
        boolean prefixEnabled = isMessageEnabled("prefix");
        if (!prefixEnabled) {
            return message;
        }
        String prefix = getMessage("prefix");
        return prefix + message;
    }

    public String getPrefixedMessage(String key, String... replacements) {
        if (!isMessageEnabled(key)) {
            if (plugin.getConfig().getBoolean("general.debug", false)) {
                plugin.getLogger().info("[DEBUG] Message '" + key + "' is DISABLED, not sending");
            }
            return null;
        }
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        boolean prefixEnabled = isMessageEnabled("prefix");
        if (!prefixEnabled) {
            return message;
        }
        String prefix = getMessage("prefix");
        return prefix + message;
    }

    public void sendMessage(Player player, String key) {
        String message = getPrefixedMessage(key);
        if (message != null) {
            player.sendMessage(message);
        } else {
            if (plugin.getConfig().getBoolean("general.debug", false)) {
                plugin.getLogger().info("[DEBUG] Not sending message '" + key + "' - disabled in config");
            }
        }
    }

    public void sendMessage(Player player, String key, String... replacements) {
        String message = getPrefixedMessage(key, replacements);
        if (message != null) {
            player.sendMessage(message);
        } else {
            if (plugin.getConfig().getBoolean("general.debug", false)) {
                plugin.getLogger().info("[DEBUG] Not sending message '" + key + "' - disabled in config");
            }
        }
    }

    public void reload() {
        this.currentLanguage = plugin.getConfig().getString("general.language", "es");
        loadMessages();
    }
}