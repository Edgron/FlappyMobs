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
        if (messages.isConfigurationSection(key)) {
            return messages.getBoolean(key + ".enabled", true);
        }
        return true;
    }
    public String getMessage(String key) {
        if (!isMessageEnabled(key)) {
            return null;
        }
        String message;
        if (messages.isConfigurationSection(key)) {
            message = messages.getString(key + ".message", "&cMessage not found: " + key);
        } else {
            message = messages.getString(key, "&cMessage not found: " + key);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    public String getPrefixedMessage(String key) {
        String message = getMessage(key);
        if (message == null) {
            return null;
        }
        String prefix = getMessage("prefix");
        if (prefix == null) {
            return message;
        }
        return prefix + message;
    }
    public String getPrefixedMessage(String key, String... replacements) {
        String message = getMessage(key);
        if (message == null) {
            return null;
        }
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        String prefix = getMessage("prefix");
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
