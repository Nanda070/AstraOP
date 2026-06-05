package com.astraor.listeners;

import com.astraor.AstraORPlugin;
import com.astraor.gui.OnboardingGUI;
import com.astraor.state.AstraClass;
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
        // Задержка: гарантируем, что игрок полностью загружен
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            if (!plugin.getClassManager().hasClass(player)) {
                // Новый игрок — показываем экран выбора класса
                plugin.getClassManager().setChoosingClass(player, true);
                OnboardingGUI.openMainGUI(player);
            } else {
                // FIX #15: Атрибуты и эффекты сохраняются в PDC, но AttributeModifier
                // не персистентен — нужно заново применить модификаторы класса при каждом входе
                AstraClass playerClass = plugin.getClassManager().getPlayerClass(player);
                if (playerClass != null) {
                    plugin.getMechanicsManager().applyClass(player, playerClass);
                }
            }
        }, 10L);
    }
}
