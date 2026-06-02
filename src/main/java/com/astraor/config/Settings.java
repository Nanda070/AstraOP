package com.astraor.config;

import com.astraor.AstraORPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class Settings {
    private final AstraORPlugin plugin;

    private String webhookUrl;
    private long classChangeCost;
    private long bossRespawnTimeTicks;

    public Settings(AstraORPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.webhookUrl = config.getString("webhook.url", "");
        this.classChangeCost = config.getLong("class_change_cost", 1000L);
        this.bossRespawnTimeTicks = config.getLong("boss_respawn_time_ticks", 12000L); // 10 minutes default
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public long getClassChangeCost() {
        return classChangeCost;
    }

    public long getBossRespawnTimeTicks() {
        return bossRespawnTimeTicks;
    }
}
