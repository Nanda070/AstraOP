package com.astraor.mechanics;

import com.astraor.AstraORPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class CatalystManager {

    private final AstraORPlugin plugin;
    private final NamespacedKey catalystKey;

    public CatalystManager(AstraORPlugin plugin) {
        this.plugin = plugin;
        this.catalystKey = new NamespacedKey(plugin, "is_catalyst");
    }

    public void registerRecipe() {
        ItemStack catalyst = getCatalystItem();

        NamespacedKey recipeKey = new NamespacedKey(plugin, "astra_catalyst");
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, catalyst);
        
        recipe.shape("ADA", "DND", "ADA");
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('N', Material.NETHER_STAR);

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info("Registered Astra Catalyst recipe.");
    }

    public ItemStack getCatalystItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§d§lКатализатор Астры"));
            meta.lore(List.of(
                    Component.text("§7Редкий артефакт, пульсирующий"),
                    Component.text("§7пространственной энергией. Его можно"),
                    Component.text("§7отдать Хранителю Храма, чтобы"),
                    Component.text("§7разорвать ваши текущие связи.")
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            meta.getPersistentDataContainer().set(catalystKey, PersistentDataType.BYTE, (byte) 1);
            
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isCatalyst(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(catalystKey, PersistentDataType.BYTE);
    }
}
