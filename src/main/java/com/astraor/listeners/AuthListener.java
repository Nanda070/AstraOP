package com.astraor.listeners;

import com.astraor.AstraORPlugin;
import com.astraor.gui.OnboardingGUI;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RegisterEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AuthListener implements Listener {

    private final AstraORPlugin plugin;

    public AuthListener(AstraORPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        handleAuthSuccess(event.getPlayer());
    }

    @EventHandler
    public void onRegister(RegisterEvent event) {
        handleAuthSuccess(event.getPlayer());
    }

    private void handleAuthSuccess(Player player) {
        if (!plugin.getClassManager().hasClass(player)) {
            plugin.getClassManager().setChoosingClass(player, true);
            
            // Delay slightly to ensure player is fully logged in before opening GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    OnboardingGUI.openMainGUI(player);
                }
            }, 10L);
        }
    }
}
