package com.astraor.gui;

import com.astraor.AstraORPlugin;
import com.astraor.state.AstraClass;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class OnboardingGUI implements Listener {

    private final AstraORPlugin plugin;
    private static final String MAIN_TITLE = "§6§lВыбор класса AstraOR";
    private static final String CONFIRM_TITLE = "§c§lПодтвердите: ";

    // Store pending selections for confirmation
    private final Map<Player, AstraClass> pendingSelections = new HashMap<>();

    public OnboardingGUI(AstraORPlugin plugin) {
        this.plugin = plugin;
    }

    public static void openMainGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(MAIN_TITLE));

        // Background
        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, glass);
        }

        // Slot 4: Info Book
        inv.setItem(4, createItem(Material.BOOK, "§6§lАрхивы Астры: Мутации", 
                "§7Добро пожаловать в Архивы.",
                "§7Пожалуйста, выберите мутацию,",
                "§7чтобы адаптироваться к этому миру."));

        // Slots 19-25: Classes
        inv.setItem(19, createItem(Material.BRICK, "§b§lАрхитектор", 
                "§7Специалист по инфраструктуре", "", "§a+ Вечная Спешка II и Скорость I", "§a+ Иммунитет к урону от падения", "§a+ Дальность ломания блоков +2", "", "§c- Урон от взрывов +50%", "§c- Урон в ближнем бою -30%"));
        
        inv.setItem(20, createItem(Material.IRON_SWORD, "§c§lАвангард", 
                "§7Мастер ближнего боя", "", "§a+ Базовый урон +20%", "§a+ Адреналин (<30% Здоровья)", "", "§c- Голод уходит в 1.5x быстрее", "§c- Урон из лука -50%"));
        
        inv.setItem(21, createItem(Material.DIAMOND_CHESTPLATE, "§9§lДжаггернаут", 
                "§7Тяжелый танк", "", "§a+ 10 Макс. Здоровья (5 сердец)", "§a+ 4 Брони и 2 Твердости", "§a+ 100% Сопротивление отбрасыванию", "", "§c- Вечная Медлительность I", "§c- Запрет на Элитры"));
        
        inv.setItem(22, createItem(Material.COAL, "§8§lТень", 
                "§7Разведчик-Ассасин", "", "§a+ Невидимость в темноте (Shift)", "§a+ 1.5x Крит. урон из невидимости", "§a+ Вечная Скорость II", "", "§c- Макс. Здоровье -6 (3 сердца)", "§c- Слабость I на солнце"));
        
        inv.setItem(23, createItem(Material.BOW, "§2§lРейнджер", 
                "§7Стрелок дальнего боя", "", "§a+ 50% Урона из лука", "§a+ 20% Скорость стрелы", "§a+ 30% Шанс сохранить стрелу", "", "§c- Урон в ближнем бою -40%", "§c- Запрет на щит во второй руке"));
        
        inv.setItem(24, createItem(Material.GOLDEN_AXE, "§4§lБерсерк", 
                "§7Мастер серии убийств", "", "§a+ Сила и Скорость за убийство", "§a+ Игнорирует 20% брони цели", "", "§c- Запрет на лук/арбалет/трезубец", "§c- Броня ломается на 30% быстрее"));
        
        inv.setItem(25, createItem(Material.FEATHER, "§f§lИдущий-с-ветром", 
                "§7Мастер мобильности", "", "§a+ Встроенные элитры", "§a+ Навык 'Восходящий поток' (Shift+ПКМ)", "§a+ Нет урона от удара в полете", "", "§c- Дебаффы ниже высоты 50", "§c- Получаемый урон от стрел +50%", "§c- Максимум железная броня"));

        // Slot 49: Guide
        inv.setItem(49, createItem(Material.COMPASS, "§e§lРуководство по смене класса", 
                "§7Вы можете сменить свой класс позже",
                "§7посетив Хранителя Храма.",
                "§7Это потребует особый предмет."));

        player.openInventory(inv);
    }

    public void openConfirmGUI(Player player, AstraClass astraClass) {
        pendingSelections.put(player, astraClass);
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(CONFIRM_TITLE + astraClass.getId().toUpperCase()));

        ItemStack glass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glass);
        }

        inv.setItem(11, createItem(Material.LIME_STAINED_GLASS_PANE, "§a§lПРИНЯТЬ", "§7Нажмите для выбора: " + astraClass.getId()));
        inv.setItem(15, createItem(Material.RED_STAINED_GLASS_PANE, "§c§lОТМЕНА", "§7Вернуться к списку"));

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
            if (lore.length > 0) {
                meta.lore(Arrays.stream(lore).map(Component::text).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.contains("Выбор класса AstraOR")) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            AstraClass selected = null;
            int slot = event.getRawSlot();
            switch (slot) {
                case 19 -> selected = AstraClass.ARCHITECT;
                case 20 -> selected = AstraClass.VANGUARD;
                case 21 -> selected = AstraClass.JUGGERNAUT;
                case 22 -> selected = AstraClass.SHADOW;
                case 23 -> selected = AstraClass.RANGER;
                case 24 -> selected = AstraClass.BERSERKER;
                case 25 -> selected = AstraClass.WINDWALKER;
            }

            if (selected != null) {
                openConfirmGUI(player, selected);
            }
        } else if (title.contains("Подтвердите:")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            int slot = event.getRawSlot();
            if (slot == 11) { // ACCEPT
                AstraClass selected = pendingSelections.get(player);
                if (selected != null) {
                    confirmClass(player, selected);
                }
            } else if (slot == 15) { // CANCEL
                pendingSelections.remove(player);
                openMainGUI(player);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        // If the inventory was closed because another one opened, don't interfere.
        // This handles the case of: main GUI -> click class -> confirm GUI opens
        if (event.getReason() == InventoryCloseEvent.Reason.OPEN_NEW) return;
        
        // Force them to pick if they are choosing and closed the inventory themselves
        if (plugin.getClassManager().isChoosingClass(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && plugin.getClassManager().isChoosingClass(player)) {
                    openMainGUI(player);
                }
            }, 2L);
        }
    }

    private void confirmClass(Player player, AstraClass astraClass) {
        pendingSelections.remove(player);
        plugin.getClassManager().setChoosingClass(player, false);
        
        // This is where MechanicsManager should apply the modifiers and we log the selection.
        // For now, we will just set the ID and do the titles. We will implement MechanicsManager next.
        plugin.getClassManager().setClass(player, astraClass);
        
        // Dispatch to log
        plugin.getDiscordWebhook().logClassSelection(player.getName(), astraClass.getId());

        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.showTitle(net.kyori.adventure.title.Title.title(
            Component.text("§aКласс выбран!"), 
            Component.text("§7Теперь вы " + astraClass.getId()), 
            net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(3500), java.time.Duration.ofMillis(1000))
        ));

        // Notify mechanics to apply initial modifiers and validate equipment
        plugin.getMechanicsManager().applyClass(player, astraClass);
    }
}
