package com.astraor.gui;

import com.astraor.AstraORPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class GuideGUI implements Listener {

    private final AstraORPlugin plugin;

    public GuideGUI(AstraORPlugin plugin) {
        this.plugin = plugin;
    }

    public static void openGUI(Player player, AstraORPlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§8Путеводитель по Астре"));

        inv.setItem(10, createItem(Material.WRITTEN_BOOK, "§6§lТайны Астры", Arrays.asList(
                "§7Узнайте о происхождении силы,",
                "§7о классах и о том, как изменить",
                "§7свою судьбу."
        )));

        inv.setItem(12, createItem(Material.EMERALD, "§a§lПуть Монет", Arrays.asList(
                "§7Переместиться к Хранителю,",
                "§7который разорвёт ваши связи",
                "§7за звонкие монеты."
        )));

        inv.setItem(14, createItem(Material.AMETHYST_SHARD, "§d§lПуть Катализатора", Arrays.asList(
                "§7Переместиться к Хранителю,",
                "§7который разорвёт ваши связи",
                "§7за Катализатор Астры."
        )));

        inv.setItem(16, createItem(Material.WITHER_SKELETON_SKULL, "§4§lВызов Хранителю", Arrays.asList(
                "§7Отправиться в логово Босса,",
                "§7чтобы испытать свою силу",
                "§7и добыть Катализатор."
        )));

        // Заполнение пустоты
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            if (lore != null) {
                meta.lore(lore.stream().map(Component::text).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text("§8Путеводитель по Астре"))) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();

        if (slot == 10) {
            // Информация
            player.closeInventory();
            player.sendMessage(Component.text(" "));
            player.sendMessage(Component.text("§6§l=== Тайны Астры ==="));
            player.sendMessage(Component.text("§fДавным-давно мир был разделен на созвездия, дарующие смертным уникальные силы..."));
            player.sendMessage(Component.text("§fНо сила — это не только дар, но и бремя. Если вы хотите изменить свою суть, вам придется найти Хранителей Храма."));
            player.sendMessage(Component.text("§fОдни потребуют от вас мирского богатства (Монеты), другие — редчайшую реликвию (Катализатор Астры)."));
            player.sendMessage(Component.text("§fСам же Катализатор охраняется древним Хранителем. Одолейте его, если осмелитесь!"));
            player.sendMessage(Component.text(" "));
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
        } else if (slot == 12) {
            // ТП к монетному НПС
            Location loc = plugin.getLocationManager().getLocation("money_npc");
            if (loc != null) {
                player.teleport(loc);
                player.sendMessage(Component.text("§aВы были перемещены к Хранителю Монет."));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            } else {
                player.sendMessage(Component.text("§cТочка перемещения не установлена администратором."));
            }
            player.closeInventory();
        } else if (slot == 14) {
            // ТП к айтемному НПС
            Location loc = plugin.getLocationManager().getLocation("item_npc");
            if (loc != null) {
                player.teleport(loc);
                player.sendMessage(Component.text("§aВы были перемещены к Хранителю Катализатора."));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            } else {
                player.sendMessage(Component.text("§cТочка перемещения не установлена администратором."));
            }
            player.closeInventory();
        } else if (slot == 16) {
            // ТП к боссу
            List<Location> bossSpawns = plugin.getBossManager().getBossSpawns();
            if (bossSpawns != null && !bossSpawns.isEmpty()) {
                // ТП к первому спавну босса
                player.teleport(bossSpawns.get(0));
                player.sendMessage(Component.text("§4Вы были перемещены в логово Хранителя Катализатора!"));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 0.5f, 1f);
            } else {
                player.sendMessage(Component.text("§cТочка появления босса не найдена."));
            }
            player.closeInventory();
        }
    }
}
