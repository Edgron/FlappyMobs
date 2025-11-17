package com.omniblock.flappymobs.config;

import com.omniblock.flappymobs.FlappyMobs;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class MessagesManager {

    private final FlappyMobs plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public MessagesManager(FlappyMobs plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        String language = plugin.getConfig().getString("general.language", "es");

        // Create lang folder
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // Copy default messages file
        messagesFile = new File(langFolder, "messages_" + language + ".yml");
        if (!messagesFile.exists()) {
            try {
                InputStream defaultMessages = plugin.getResource("lang/messages_" + language + ".yml");
                if (defaultMessages != null) {
                    Files.copy(defaultMessages, messagesFile.toPath());
                } else {
                    plugin.getLogger().warning("Language file not found for: " + language);
                    // Fallback to Spanish
                    defaultMessages = plugin.getResource("lang/messages_es.yml");
                    if (defaultMessages != null) {
                        Files.copy(defaultMessages, messagesFile.toPath());
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create messages file!");
                e.printStackTrace();
            }
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reload() {
        loadMessages();
    }

    public String getMessage(String key) {
        String message = messages.getString(key, "&cMessage not found: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }

        return message;
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    public String getPrefixedMessage(String key) {
        return getPrefix() + getMessage(key);
    }

    public String getPrefixedMessage(String key, String... replacements) {
        return getPrefix() + getMessage(key, replacements);
    }
}