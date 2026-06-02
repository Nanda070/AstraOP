package com.astraor;

import com.astraor.config.Settings;
import com.astraor.util.DiscordWebhook;
import org.bukkit.plugin.java.JavaPlugin;

public class AstraORPlugin extends JavaPlugin {

    private Settings settings;
    private DiscordWebhook discordWebhook;
    private com.astraor.state.ClassManager classManager;
    private com.astraor.mechanics.MechanicsManager mechanicsManager;
    private com.astraor.mechanics.CatalystManager catalystManager;

    @Override
    public void onEnable() {
        getLogger().info("AstraOR is initializing...");

        // Config & Utils
        this.settings = new Settings(this);
        this.settings.load();
        
        this.discordWebhook = new DiscordWebhook(this);
        this.classManager = new com.astraor.state.ClassManager(this);
        this.mechanicsManager = new com.astraor.mechanics.MechanicsManager(this);
        this.catalystManager = new com.astraor.mechanics.CatalystManager(this);

        this.catalystManager.registerRecipe();

        // Initialize Listeners
        getServer().getPluginManager().registerEvents(new com.astraor.listeners.AuthListener(this), this);
        getServer().getPluginManager().registerEvents(new com.astraor.listeners.FreezeListener(this), this);
        getServer().getPluginManager().registerEvents(new com.astraor.gui.OnboardingGUI(this), this);
        getServer().getPluginManager().registerEvents(new com.astraor.listeners.NPCInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new com.astraor.mechanics.MechanicsListener(this, this.mechanicsManager), this);

        // Initialize Commands
        com.astraor.commands.AstraORCommand.register(this);

        getLogger().info("AstraOR successfully enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("AstraOR disabled.");
    }

    public Settings getSettings() {
        return settings;
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }

    public com.astraor.state.ClassManager getClassManager() {
        return classManager;
    }

    public com.astraor.mechanics.MechanicsManager getMechanicsManager() {
        return mechanicsManager;
    }

    public com.astraor.mechanics.CatalystManager getCatalystManager() {
        return catalystManager;
    }
}
