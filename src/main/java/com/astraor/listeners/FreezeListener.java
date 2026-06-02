package com.astraor.listeners;

import com.astraor.AstraORPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class FreezeListener implements Listener {

    private final AstraORPlugin plugin;

    public FreezeListener(AstraORPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isFrozen(Player player) {
        return plugin.getClassManager().isChoosingClass(player);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (isFrozen(event.getPlayer())) {
            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (isFrozen(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (isFrozen(player)) {
                // If it's our custom GUI, OnboardingGUI will handle it. We just don't want them messing with their own inv.
                // We should cancel the event generally unless handled by OnboardingGUI logic
                // For safety, cancel all clicks here, and let the GUI listener override or we check GUI titles.
                String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
                if (title.contains("Class Selection") || title.contains("Confirm Class")) {
                    return; // Let OnboardingGUI handle it
                }
                event.setCancelled(true);
            }
        }
    }
}
