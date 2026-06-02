package com.astraor.listeners;

import com.astraor.AstraORPlugin;
import com.astraor.gui.OnboardingGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class NPCInteractListener implements Listener {

    private final AstraORPlugin plugin;
    private final NamespacedKey npcKey;

    public NPCInteractListener(AstraORPlugin plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "temple_npc");
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();
        
        // Check if the clicked entity is our custom NPC
        if (clicked.getPersistentDataContainer().has(npcKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);

            // Don't interact if already choosing
            if (plugin.getClassManager().isChoosingClass(player)) {
                return;
            }

            // NPC Dialog
            player.sendMessage(Component.text("§6[Хранитель Храма] §fПриветствую, путник... Всего за один Astra Catalyst я могу разорвать твои связи и позволить тебе выбрать новый путь."));

            // Check for Catalyst in inventory
            boolean hasCatalyst = false;
            for (ItemStack item : player.getInventory().getContents()) {
                if (plugin.getCatalystManager().isCatalyst(item)) {
                    hasCatalyst = true;
                    // Deduct exactly 1 catalyst
                    item.setAmount(item.getAmount() - 1);
                    break;
                }
            }

            if (hasCatalyst) {
                player.sendMessage(Component.text("§aОплата принята. Архивы открыты."));
                
                // Clear their current class and tag them
                plugin.getClassManager().clearClass(player);
                plugin.getClassManager().setClass(player, null);
                plugin.getClassManager().setChoosingClass(player, true);
                
                // Open GUI
                OnboardingGUI.openMainGUI(player);
            } else {
                player.sendMessage(Component.text("§cУ вас нет Astra Catalyst."));
            }
        }
    }
}
