package com.astraor.config;

import com.astraor.AstraORPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class Settings {
    private final AstraORPlugin plugin;

    private String webhookUrl;

    public Settings(AstraORPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.webhookUrl = config.getString("webhook.url", "");
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }
}
