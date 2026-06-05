package com.astraor.mechanics;

import com.astraor.AstraORPlugin;
import com.astraor.state.AstraClass;
import com.astraor.state.CooldownManager;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MechanicsManager {

    // ---- Константы имён способностей (для CooldownManager) ----
    public static final String ABILITY_VANGUARD_ADRENALINE = "vanguard_adrenaline";
    public static final String ABILITY_WINDWALKER_UPDRAFT  = "windwalker_updraft";
    public static final String ABILITY_PHANTOM_PHASE       = "phantom_phase";
    public static final String ABILITY_PHOENIX_SUPERNOVA   = "phoenix_supernova";
    public static final String ABILITY_DRUID_GRASP         = "druid_grasp";
    public static final String ABILITY_VAMPIRE_DASH        = "vampire_dash";
    public static final String ABILITY_GOLIATH_TREMOR      = "goliath_tremor";

    private final AstraORPlugin plugin;
    private final CooldownManager cooldowns;

    public MechanicsManager(AstraORPlugin plugin) {
        this.plugin    = plugin;
        this.cooldowns = plugin.getCooldownManager();
        startTickingTask();
    }

    /** Удобный доступ к CooldownManager для MechanicsListener. */
    public CooldownManager getCooldowns() {
        return cooldowns;
    }

    // ----------------------------------------------------------------
    //  applyClass
    // ----------------------------------------------------------------

    public void applyClass(Player player, AstraClass astraClass) {
        plugin.getClassManager().clearClass(player);
        if (astraClass == null) return;

        switch (astraClass) {
            case ARCHITECT -> applyModifier(player, Attribute.PLAYER_BLOCK_INTERACTION_RANGE, 2.0, AttributeModifier.Operation.ADD_NUMBER);
            case JUGGERNAUT -> {
                applyModifier(player, Attribute.GENERIC_MAX_HEALTH, 10.0, AttributeModifier.Operation.ADD_NUMBER);
                applyModifier(player, Attribute.GENERIC_ARMOR, 4.0, AttributeModifier.Operation.ADD_NUMBER);
                applyModifier(player, Attribute.GENERIC_ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_NUMBER);
                applyModifier(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1.0, AttributeModifier.Operation.ADD_NUMBER);
            }
            case SHADOW   -> applyModifier(player, Attribute.GENERIC_MAX_HEALTH, -6.0, AttributeModifier.Operation.ADD_NUMBER);
            case PHANTOM  -> applyModifier(player, Attribute.GENERIC_MAX_HEALTH, -4.0, AttributeModifier.Operation.ADD_NUMBER);
            case PHOENIX  -> applyModifier(player, Attribute.GENERIC_MAX_HEALTH, -4.0, AttributeModifier.Operation.ADD_NUMBER);
            case WINDWALKER -> equipInnateElytra(player);
            case GOLIATH -> {
                applyModifier(player, Attribute.GENERIC_MAX_HEALTH, 10.0, AttributeModifier.Operation.ADD_NUMBER);
                applyModifier(player, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1.0, AttributeModifier.Operation.ADD_NUMBER);
            }
            // RANGER, VANGUARD, BERSERKER, DRUID, VAMPIRE — только через события
            default -> {}
        }

        validateEquipment(player, astraClass);
    }

    // ----------------------------------------------------------------
    //  applyModifier — FIX #1: без ConcurrentModificationException
    // ----------------------------------------------------------------

    private void applyModifier(Player player, Attribute attribute, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        NamespacedKey key = new NamespacedKey(plugin, attribute.getKey().getKey() + "_mod");

        // Собираем для удаления отдельно — не модифицируем коллекцию во время итерации
        List<AttributeModifier> toRemove = new ArrayList<>();
        for (AttributeModifier mod : instance.getModifiers()) {
            if (mod.getKey().equals(key)) {
                toRemove.add(mod);
            }
        }
        toRemove.forEach(instance::removeModifier);

        instance.addModifier(new AttributeModifier(key, amount, operation, EquipmentSlotGroup.ANY));
    }

    // ----------------------------------------------------------------
    //  Innate Elytra
    // ----------------------------------------------------------------

    public void equipInnateElytra(Player player) {
        NamespacedKey innateKey = plugin.getClassManager().getInnateElytraKey();
        ItemStack elytra = new ItemStack(org.bukkit.Material.ELYTRA);
        ItemMeta meta = elytra.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§7§oВстроенные Элитры"));
            meta.getPersistentDataContainer().set(innateKey, PersistentDataType.BYTE, (byte) 1);
            elytra.setItemMeta(meta);
        }

        PlayerInventory inv = player.getInventory();
        ItemStack current = inv.getChestplate();
        // Возвращаем старый нагрудник в инвентарь, если это не наша элитра
        if (current != null && current.hasItemMeta()) {
            if (!current.getItemMeta().getPersistentDataContainer().has(innateKey, PersistentDataType.BYTE)) {
                inv.addItem(current);
            }
        }
        inv.setChestplate(elytra);
    }

    public boolean isInnateElytra(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(plugin.getClassManager().getInnateElytraKey(), PersistentDataType.BYTE);
    }

    // ----------------------------------------------------------------
    //  Ticking task
    // ----------------------------------------------------------------

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
                        // FIX #4: Правильная проверка прямого солнечного света:
                        // hasStorm() исключает солнце, проверяем свет И время И отсутствие бури
                        if (!player.getWorld().hasStorm()
                                && player.getLocation().getBlock().getLightFromSky() == 15
                                && player.getWorld().getTime() < 13000) {
                            int highestY = player.getWorld().getHighestBlockYAt(player.getLocation());
                            if (player.getLocation().getY() >= highestY) {
                                addEffect(player, PotionEffectType.WEAKNESS, 0, 60);
                            }
                        }
                    }
                    case WINDWALKER -> {
                        if (player.getGameMode() == org.bukkit.GameMode.SURVIVAL
                                || player.getGameMode() == org.bukkit.GameMode.ADVENTURE) {
                            if (!player.getAllowFlight() && !player.isGliding()
                                    && ((org.bukkit.entity.Entity) player).isOnGround()) {
                                player.setAllowFlight(true);
                            }
                        }
                        if (player.getLocation().getY() < 50) {
                            addEffect(player, PotionEffectType.MINING_FATIGUE, 0, 60);
                            addEffect(player, PotionEffectType.WEAKNESS, 0, 60);
                        }
                    }
                    case PHOENIX -> {
                        if (player.isInWaterOrRain() || player.isInWaterOrBubbleColumn()) {
                            player.damage(1.0);
                        }
                    }
                    case GOLIATH -> {
                        addEffect(player, PotionEffectType.HASTE, 1, 60);
                        addEffect(player, PotionEffectType.SLOWNESS, 0, 60);
                        if (player.getFoodLevel() > 0 && ThreadLocalRandom.current().nextInt(100) < 5) {
                            player.setFoodLevel(Math.max(0, player.getFoodLevel() - 1));
                        }
                    }
                    default -> {}
                }
            }
        }, 20L, 40L);
    }

    private void addEffect(Player player, PotionEffectType type, int amplifier, int durationTicks) {
        player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, false, true));
    }

    // ----------------------------------------------------------------
    //  validateEquipment — FIX #5: правильный индекс chestplate (2)
    // ----------------------------------------------------------------

    public void validateEquipment(Player player, AstraClass astraClass) {
        if (astraClass == null) return;
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
            ItemStack chestplate = inv.getChestplate();
            if (chestplate == null || !isInnateElytra(chestplate)) {
                equipInnateElytra(player);
            }

            // FIX #5: getArmorContents() = [boots(0), leggings(1), chestplate(2), helmet(3)]
            // Пропускаем индекс 2 (chestplate), а не length-1
            ItemStack[] armor = inv.getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                if (i == 2) continue; // chestplate — пропускаем (там встроенная элитра)
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

        if (astraClass == AstraClass.PHANTOM || astraClass == AstraClass.DRUID) {
            ItemStack[] armor = inv.getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                ItemStack item = armor[i];
                if (item != null) {
                    String name = item.getType().name();
                    if (name.contains("IRON") || name.contains("DIAMOND")
                            || name.contains("NETHERITE") || name.contains("GOLD")) {
                        armor[i] = null;
                        player.getWorld().dropItemNaturally(player.getLocation(), item);
                        if (astraClass == AstraClass.PHANTOM) {
                            player.damage(2.0);
                        }
                    }
                }
            }
            inv.setArmorContents(armor);
        }
    }

    // ----------------------------------------------------------------
    //  Cooldown message helper
    // ----------------------------------------------------------------

    public void sendCooldownMessage(Player player, String ability, long remainingMs) {
        long seconds = (remainingMs / 1000) + 1;
        player.sendActionBar(net.kyori.adventure.text.Component.text(
                "§cСпособность " + ability + " на перезарядке: §e" + seconds + "с."));
    }
}
