package com.astraor.listeners;

import com.astraor.AstraORPlugin;
import com.astraor.gui.OnboardingGUI;
import com.astrasmp.AstraSMPPlugin;
import com.astrasmp.service.EconomyService;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NPCInteractListener implements Listener {

    private final AstraORPlugin plugin;
    private final NamespacedKey npcKey;

    // FIX: UUID — value type, WeakHashMap не работает для него.
    // Используем ConcurrentHashMap + очистку в PlayerQuitEvent.
    private final Set<UUID> processingPlayers = ConcurrentHashMap.newKeySet();

    public NPCInteractListener(AstraORPlugin plugin) {
        this.plugin = plugin;
        this.npcKey = new NamespacedKey(plugin, "temple_npc");
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Player player  = event.getPlayer();
        Entity clicked = event.getRightClicked();

        if (!clicked.getPersistentDataContainer().has(npcKey, PersistentDataType.STRING)
                && !clicked.getPersistentDataContainer().has(npcKey, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);

        // Уже выбирает класс — игнорируем
        if (plugin.getClassManager().isChoosingClass(player)) return;

        // Кулдаун 15 минут
        long chosenTime = plugin.getClassManager().getClassChosenTime(player);
        if (chosenTime > 0 && System.currentTimeMillis() - chosenTime < 15L * 60L * 1000L) {
            long remainingMillis = (15L * 60L * 1000L) - (System.currentTimeMillis() - chosenTime);
            long remainingMins   = Math.max(1, remainingMillis / 60000L);
            player.sendMessage(Component.text("§cВы должны поиграть за свой класс ещё " + remainingMins + " минут, прежде чем менять его."));
            return;
        }

        String type = clicked.getPersistentDataContainer().getOrDefault(npcKey, PersistentDataType.STRING, "item");

        if (type.equalsIgnoreCase("money")) {
            handleMoneyNPC(player);
        } else {
            handleItemNPC(player);
        }
    }

    // ----------------------------------------------------------------
    //  Оплата монетами — FIX #6: атомарная защита от двойного клика
    // ----------------------------------------------------------------

    private void handleMoneyNPC(Player player) {
        player.sendMessage(Component.text("§6[Хранитель Храма] §fПриветствую... За " + plugin.getSettings().getClassChangeCost() + " монет я могу разорвать твои связи."));

        UUID uuid = player.getUniqueId();
        // Если транзакция уже в процессе — отклоняем
        if (!processingPlayers.add(uuid)) {
            player.sendMessage(Component.text("§cПожалуйста, подождите..."));
            return;
        }

        try {
            EconomyService eco = AstraSMPPlugin.getInstance().getServices().economy();
            if (eco == null) {
                player.sendMessage(Component.text("§cОшибка: система экономики недоступна."));
                return;
            }

            long cost    = plugin.getSettings().getClassChangeCost();
            long balance = eco.getBalance(uuid);
            if (balance >= cost) {
                eco.setBalance(uuid, player.getName(), balance - cost);
                player.sendMessage(Component.text("§aОплата принята. Архивы открыты."));
                openClassGUI(player);
            } else {
                player.sendMessage(Component.text("§cУ вас недостаточно монет. Нужно " + cost + "."));
            }
        } finally {
            // Всегда снимаем блокировку
            processingPlayers.remove(uuid);
        }
    }

    // ----------------------------------------------------------------
    //  Оплата катализатором
    // ----------------------------------------------------------------

    private void handleItemNPC(Player player) {
        player.sendMessage(Component.text("§6[Хранитель Храма] §fПриветствую... За один Катализатор Астры я могу разорвать твои связи."));

        boolean hasCatalyst = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (plugin.getCatalystManager().isCatalyst(item)) {
                hasCatalyst = true;
                item.setAmount(item.getAmount() - 1);
                break;
            }
        }

        if (hasCatalyst) {
            player.sendMessage(Component.text("§aКатализатор принят. Архивы открыты."));
            openClassGUI(player);
        } else {
            player.sendMessage(Component.text("§cУ вас нет Катализатора Астры."));
        }
    }

    private void openClassGUI(Player player) {
        plugin.getClassManager().clearClass(player);
        plugin.getClassManager().setClass(player, null);
        plugin.getClassManager().setChoosingClass(player, true);
        OnboardingGUI.openMainGUI(player);
    }

    // Очищаем блокировку при дисконнекте (чтобы не завис навсегда)
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        processingPlayers.remove(event.getPlayer().getUniqueId());
    }
}
