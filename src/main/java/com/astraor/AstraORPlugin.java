package com.astraor;

import com.astraor.config.Settings;
import com.astraor.gui.OnboardingGUI;
import com.astraor.listeners.AuthListener;
import com.astraor.listeners.FreezeListener;
import com.astraor.listeners.NPCInteractListener;
import com.astraor.mechanics.BossManager;
import com.astraor.mechanics.CatalystManager;
import com.astraor.mechanics.MechanicsListener;
import com.astraor.mechanics.MechanicsManager;
import com.astraor.state.ClassManager;
import com.astraor.state.CooldownManager;
import com.astraor.state.KeybindManager;
import com.astraor.util.DiscordWebhook;
import org.bukkit.plugin.java.JavaPlugin;

public class AstraORPlugin extends JavaPlugin {

    private Settings settings;
    private DiscordWebhook discordWebhook;
    private ClassManager classManager;
    private KeybindManager keybindManager;
    private CooldownManager cooldownManager;
    private MechanicsManager mechanicsManager;
    private CatalystManager catalystManager;
    private BossManager bossManager;

    @Override
    public void onEnable() {
        getLogger().info("AstraOR is initializing...");

        // Config & Utils
        this.settings        = new Settings(this);
        this.settings.load();

        this.discordWebhook  = new DiscordWebhook(this);
        this.classManager    = new ClassManager(this);
        this.keybindManager  = new KeybindManager(this);
        this.cooldownManager = new CooldownManager();
        this.mechanicsManager = new MechanicsManager(this);
        this.catalystManager  = new CatalystManager(this);
        this.bossManager      = new BossManager(this);

        this.catalystManager.registerRecipe();

        // Listeners
        getServer().getPluginManager().registerEvents(new AuthListener(this),            this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this),          this);
        getServer().getPluginManager().registerEvents(new OnboardingGUI(this),           this);
        getServer().getPluginManager().registerEvents(new NPCInteractListener(this),     this);
        getServer().getPluginManager().registerEvents(new MechanicsListener(this, mechanicsManager), this);

        // Commands
        com.astraor.commands.AstraORCommand.register(this);
        com.astraor.commands.AstraKeybindCommand.register(this);

        getLogger().info("AstraOR successfully enabled.");
    }

    @Override
    public void onDisable() {
        if (this.bossManager != null) {
            this.bossManager.onDisable();
        }
        getLogger().info("AstraOR disabled.");
    }

    public Settings getSettings()               { return settings; }
    public DiscordWebhook getDiscordWebhook()   { return discordWebhook; }
    public ClassManager getClassManager()       { return classManager; }
    public KeybindManager getKeybindManager()   { return keybindManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public MechanicsManager getMechanicsManager() { return mechanicsManager; }
    public CatalystManager getCatalystManager() { return catalystManager; }
    public BossManager getBossManager()         { return bossManager; }
}
