package com.astraor.state;

import com.astraor.AstraORPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

public class ClassManager {

    private final AstraORPlugin plugin;
    private final NamespacedKey classIdKey;
    private final NamespacedKey choosingClassKey;
    private final NamespacedKey classChosenTimeKey;
    // Создаём один раз, а не при каждом вызове clearClass / validateEquipment
    private final NamespacedKey innateElytraKey;

    public ClassManager(AstraORPlugin plugin) {
        this.plugin = plugin;
        this.classIdKey        = new NamespacedKey(plugin, "class_id");
        this.choosingClassKey  = new NamespacedKey(plugin, "choosing_class");
        this.classChosenTimeKey = new NamespacedKey(plugin, "class_chosen_time");
        this.innateElytraKey   = new NamespacedKey(plugin, "innate_elytra");
    }

    /** Возвращает ключ встроенной элитры (для использования в других менеджерах). */
    public NamespacedKey getInnateElytraKey() {
        return innateElytraKey;
    }

    public void setChoosingClass(Player player, boolean choosing) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (choosing) {
            pdc.set(choosingClassKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            pdc.remove(choosingClassKey);
        }
    }

    public boolean isChoosingClass(Player player) {
        return player.getPersistentDataContainer().has(choosingClassKey, PersistentDataType.BYTE);
    }

    public void setClass(Player player, AstraClass astraClass) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (astraClass != null) {
            pdc.set(classIdKey, PersistentDataType.STRING, astraClass.getId());
            pdc.set(classChosenTimeKey, PersistentDataType.LONG, System.currentTimeMillis());
        } else {
            pdc.remove(classIdKey);
            pdc.remove(classChosenTimeKey);
        }
    }

    public long getClassChosenTime(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        return pdc.getOrDefault(classChosenTimeKey, PersistentDataType.LONG, 0L);
    }

    public AstraClass getPlayerClass(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        String id = pdc.get(classIdKey, PersistentDataType.STRING);
        return AstraClass.fromId(id);
    }

    public boolean hasClass(Player player) {
        return player.getPersistentDataContainer().has(classIdKey, PersistentDataType.STRING);
    }

    public void clearClass(Player player) {
        // 1. Снять атрибуты плагина
        Attribute[] attributesToCheck = {
                Attribute.GENERIC_MAX_HEALTH,
                Attribute.GENERIC_MOVEMENT_SPEED,
                Attribute.GENERIC_ARMOR,
                Attribute.GENERIC_ARMOR_TOUGHNESS,
                Attribute.GENERIC_ATTACK_DAMAGE,
                Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                Attribute.PLAYER_BLOCK_INTERACTION_RANGE
        };

        for (Attribute attribute : attributesToCheck) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                // Буферный список — избегаем ConcurrentModificationException
                List<AttributeModifier> toRemove = new ArrayList<>();
                for (AttributeModifier modifier : instance.getModifiers()) {
                    if (modifier.getKey().getNamespace().equals(plugin.getName().toLowerCase())) {
                        toRemove.add(modifier);
                    }
                }
                for (AttributeModifier modifier : toRemove) {
                    instance.removeModifier(modifier);
                }
            }
        }

        // 2. Снять зелья
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Убедиться, что хп не превышает новый максимум
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null && player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }

        // 3. Сброс полёта / элитры
        if (player.getGameMode() == org.bukkit.GameMode.SURVIVAL || player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
            player.setAllowFlight(false);
            player.setGliding(false);
        }

        // Убрать встроенную элитру (используем переиспользуемый ключ)
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        org.bukkit.inventory.ItemStack chest = inv.getChestplate();
        if (chest != null && chest.hasItemMeta()) {
            if (chest.getItemMeta().getPersistentDataContainer().has(innateElytraKey, PersistentDataType.BYTE)) {
                inv.setChestplate(null);
            }
        }
    }
}
