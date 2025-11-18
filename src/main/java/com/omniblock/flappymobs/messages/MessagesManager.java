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

    public String getMessage(String key) {
        // Check if key exists in YAML (not commented)
        if (!messages.contains(key)) {
            return null;
        }

        String message = messages.getString(key);

        // Check if message is null or empty
        if (message == null || message.trim().isEmpty()) {
            return null;
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefixedMessage(String key) {
        String message = getMessage(key);

        // Message not found or commented
        if (message == null) {
            return null;
        }

        String prefix = getMessage("prefix");

        // If prefix doesn't exist, return message without prefix
        if (prefix == null) {
            return message;
        }

        return prefix + message;
    }

    public String getPrefixedMessage(String key, String... replacements) {
        String message = getMessage(key);

        // Message not found or commented
        if (message == null) {
            return null;
        }

        // Apply replacements
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }

        String prefix = getMessage("prefix");

        // If prefix doesn't exist, return message without prefix
        if (prefix == null) {
            return message;
        }

        return prefix + message;
    }

    public void sendMessage(Player player, String key) {
        String message = getPrefixedMessage(key);
        if (message != null) {
            player.sendMessage(message);
        }
    }

    public void sendMessage(Player player, String key, String... replacements) {
        String message = getPrefixedMessage(key, replacements);
        if (message != null) {
            player.sendMessage(message);
        }
    }

    public void reload() {
        this.currentLanguage = plugin.getConfig().getString("general.language", "es");
        loadMessages();
    }
}