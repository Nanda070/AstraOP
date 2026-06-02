package com.astraor.mechanics;

import com.astraor.AstraORPlugin;
import com.astraor.state.AstraClass;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MechanicsManager {

    private final AstraORPlugin plugin;
    // Track cooldowns (UUID -> Timestamp)
    public final Map<UUID, Long> vanguardAdrenalineCooldowns = new HashMap<>();
    public final Map<UUID, Long> windwalkerUpdraftCooldowns = new HashMap<>();

    public MechanicsManager(AstraORPlugin plugin) {
        this.plugin = plugin;
        startTickingTask();
    }

    public void applyClass(Player player, AstraClass astraClass) {
        // First ensure clean state
        plugin.getClassManager().clearClass(player);
        if (astraClass == null) return;

        // Apply new modifiers
        switch (astraClass) {
            case ARCHITECT -> applyModifier(player, Attribute.PLAYER_BLOCK_INTERACTION_RANGE, 2.0, AttributeModifier.Operation.ADD_NUMBER);
            case JUGGERNAUT -> {
                applyModifier(player, Attribute.GENERIC_MAX_HEALTH, 10.0, AttributeModifier.Operation.ADD_NUMBER);
                applyModifier(player, Attribute.GENERIC_ARMOR, 4.0, AttributeModifier.Operation.ADD_NUMBER);
                applyModifier(player, Attribute.GENERIC_ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_NUMBER);
                applyModifier(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1.0, AttributeModifier.Operation.ADD_NUMBER);
            }
            case SHADOW -> applyModifier(player, Attribute.GENERIC_MAX_HEALTH, -6.0, AttributeModifier.Operation.ADD_NUMBER);
            case RANGER -> {
                // Modifiers applied via damage events
            }
            case VANGUARD -> {
                // Modifiers applied via damage events
            }
            case BERSERKER -> {
                // Modifiers applied via damage events
            }
            case WINDWALKER -> {
                equipInnateElytra(player);
            }
            default -> {}
        }
        
        // Initial equipment validation check (drop incompatible)
        validateEquipment(player, astraClass);
    }

    private void applyModifier(Player player, Attribute attribute, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            NamespacedKey key = new NamespacedKey(plugin, attribute.getKey().getKey() + "_mod");
            AttributeModifier modifier = new AttributeModifier(key, amount, operation, EquipmentSlotGroup.ANY);
            // Remove existing with same key if any
            for (AttributeModifier mod : instance.getModifiers()) {
                if (mod.getKey().equals(key)) {
                    instance.removeModifier(mod);
                }
            }
            instance.addModifier(modifier);
        }
    }

    public void equipInnateElytra(Player player) {
        NamespacedKey innateKey = new NamespacedKey(plugin, "innate_elytra");
        ItemStack elytra = new ItemStack(org.bukkit.Material.ELYTRA);
        ItemMeta meta = elytra.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§7§oВстроенные Элитры"));
            meta.getPersistentDataContainer().set(innateKey, PersistentDataType.BYTE, (byte) 1);
            elytra.setItemMeta(meta);
        }

        PlayerInventory inv = player.getInventory();
        // Return old chestplate to inventory if it is not our innate elytra
        ItemStack current = inv.getChestplate();
        if (current != null && current.hasItemMeta()) {
            if (!current.getItemMeta().getPersistentDataContainer().has(innateKey, PersistentDataType.BYTE)) {
                inv.addItem(current);
            }
        }
        inv.setChestplate(elytra);
    }

    public boolean isInnateElytra(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey innateKey = new NamespacedKey(plugin, "innate_elytra");
        return item.getItemMeta().getPersistentDataContainer().has(innateKey, PersistentDataType.BYTE);
    }

    private void startTickingTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                AstraClass clazz = plugin.getClassManager().getPlayerClass(player);
                if (clazz == null) continue;

                switch (clazz) {
                    case ARCHITECT -> {
                        addEffect(player, PotionEffectType.HASTE, 1, 100);
                        addEffect(player, PotionEffectType.SPEED, 0, 100);
                    }
                    case JUGGERNAUT -> addEffect(player, PotionEffectType.SLOWNESS, 0, 100);
                    case SHADOW -> {
                        addEffect(player, PotionEffectType.SPEED, 1, 100);
                        // Sunlight check
                        if (player.getWorld().hasStorm() || player.getLocation().getBlock().getLightFromSky() == 15) {
                            if (player.getWorld().getTime() < 13000 || player.getWorld().getTime() > 23000) {
                                // Direct sunlight
                                int highestY = player.getWorld().getHighestBlockYAt(player.getLocation());
                                if (player.getLocation().getY() >= highestY) {
                                    addEffect(player, PotionEffectType.WEAKNESS, 0, 60);
                                }
                            }
                        }
                    }
                    case WINDWALKER -> {
                        if (player.getGameMode() == org.bukkit.GameMode.SURVIVAL || player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                            if (!player.getAllowFlight() && !player.isGliding() && ((org.bukkit.entity.Entity)player).isOnGround()) {
                                player.setAllowFlight(true);
                            }
                        }
                        if (player.getLocation().getY() < 50) {
                            addEffect(player, PotionEffectType.MINING_FATIGUE, 0, 60);
                            addEffect(player, PotionEffectType.WEAKNESS, 0, 60);
                        }
                    }
                    default -> {}
                }
            }
        }, 20L, 40L); // Run every 2 seconds
    }

    private void addEffect(Player player, PotionEffectType type, int amplifier, int durationTicks) {
        player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, false, true));
    }
    
    public void validateEquipment(Player player, AstraClass astraClass) {
        PlayerInventory inv = player.getInventory();
        
        if (astraClass == AstraClass.RANGER) {
            ItemStack offhand = inv.getItemInOffHand();
            if (offhand.getType().name().contains("SHIELD")) {
                inv.setItemInOffHand(null);
                player.getWorld().dropItemNaturally(player.getLocation(), offhand);
            }
        }
        
        if (astraClass == AstraClass.JUGGERNAUT) {
            ItemStack chest = inv.getChestplate();
            if (chest != null && chest.getType().name().contains("ELYTRA")) {
                inv.setChestplate(null);
                player.getWorld().dropItemNaturally(player.getLocation(), chest);
            }
        }
        
        if (astraClass == AstraClass.BERSERKER) {
            ItemStack[] contents = inv.getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null) {
                    String name = item.getType().name();
                    if (name.contains("BOW") || name.contains("CROSSBOW") || name.contains("TRIDENT")) {
                        inv.setItem(i, null);
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
            }
        }
        
        if (astraClass == AstraClass.WINDWALKER) {
            // Restore innate elytra if player managed to remove it
            ItemStack chestplate = inv.getChestplate();
            if (chestplate == null || !isInnateElytra(chestplate)) {
                equipInnateElytra(player);
            }

            // Remove diamond/netherite armor from other slots
            ItemStack[] armor = inv.getArmorContents();
            for (int i = 0; i < armor.length - 1; i++) { // skip index 2 (chestplate)
                ItemStack item = armor[i];
                if (item != null) {
                    String name = item.getType().name();
                    if (name.contains("DIAMOND") || name.contains("NETHERITE")) {
                        armor[i] = null;
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                    }
                }
            }
            inv.setArmorContents(armor);
        }
    }
}
