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

    // Константы заголовков GUI — единый источник правды, не дублируем строки
    private static final String GUI_MAIN_TITLE    = "Выбор класса AstraOR";
    private static final String GUI_CONFIRM_TITLE = "Подтвердите:";

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
            // Блокируем горизонтальное перемещение, разрешаем поворот головы
            if (event.getFrom().getX() != event.getTo().getX()
                    || event.getFrom().getZ() != event.getTo().getZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        // FIX: современный instanceof pattern вместо cast
        if (event.getEntity() instanceof Player player && isFrozen(player)) {
            event.setCancelled(true);
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
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isFrozen(player)) return;

        // FIX: сравниваем по plain-text заголовку, но через константы —
        // если название GUI изменится, достаточно поменять здесь и в OnboardingGUI.
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.getView().title());

        if (title.contains(GUI_MAIN_TITLE) || title.contains(GUI_CONFIRM_TITLE)) {
            // Наши GUI — OnboardingGUI сам обработает клик
            return;
        }

        // Любой другой инвентарь — блокируем
        event.setCancelled(true);
    }
}
