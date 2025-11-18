package com.omniblock.flappymobs.config;

import com.omniblock.flappymobs.FlappyMobs;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessagesManager {

    private final FlappyMobs plugin;
    private FileConfiguration messages;
    private final Map<String, String> cache;

    public MessagesManager(FlappyMobs plugin) {
        this.plugin = plugin;
        this.cache = new HashMap<>();
        loadMessages();
    }

    private void loadMessages() {
        String lang = plugin.getConfig().getString("general.language", "es");
        File langFile = new File(plugin.getDataFolder() + "/lang", lang + ".yml");

        if (!langFile.exists()) {
            plugin.saveResource("lang/" + lang + ".yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(langFile);
        cache.clear();

        plugin.getLogger().info("Loaded language: " + lang);
    }

    public String getMessage(String key, String... replacements) {
        String message = cache.computeIfAbsent(key, k -> 
            ChatColor.translateAlternateColorCodes('&', 
                messages.getString(k, "&cMessage not found: " + k)));

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }

        return message;
    }

    public String getPrefixedMessage(String key, String... replacements) {
        return getMessage("prefix") + getMessage(key, replacements);
    }

    public void reload() {
        loadMessages();
    }
}