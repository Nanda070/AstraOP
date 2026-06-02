package com.astraor.mechanics;

import com.astraor.AstraORPlugin;
import com.astraor.state.AstraClass;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.bukkit.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

public class MechanicsListener implements Listener {

    private final AstraORPlugin plugin;
    private final MechanicsManager manager;

    public MechanicsListener(AstraORPlugin plugin, MechanicsManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private AstraClass getClass(Player player) {
        return plugin.getClassManager().getPlayerClass(player);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player))
            return;
        AstraClass c = getClass(player);
        if (c == null)
            return;

        switch (c) {
            case ARCHITECT -> {
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                } else if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                        || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                    event.setDamage(event.getDamage() * 1.5);
                }
            }
            case WINDWALKER -> {
                // Immunity to fly-into-wall AND fall damage
                if (event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL
                        || event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Player as Damager
        if (event.getDamager() instanceof Player player) {
            AstraClass c = getClass(player);
            if (c != null) {
                switch (c) {
                    case ARCHITECT -> event.setDamage(event.getDamage() * 0.7); // -30% melee
                    case VANGUARD -> event.setDamage(event.getDamage() * 1.2); // +20% melee
                    case RANGER -> event.setDamage(event.getDamage() * 0.6); // -40% melee
                    case SHADOW -> {
                        if (player.isInvisible() || player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            event.setDamage(event.getDamage() * 1.5);
                            player.removePotionEffect(PotionEffectType.INVISIBILITY); // Break invis on hit
                        }
                    }
                    case BERSERKER -> {
                        // Ignore 20% armor -> Increase damage based on target's armor, or apply custom
                        // logic
                        // Paper API allows getting target armor if target is LivingEntity
                        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                            org.bukkit.attribute.AttributeInstance armorAttr = target
                                    .getAttribute(org.bukkit.attribute.Attribute.GENERIC_ARMOR);
                            if (armorAttr != null) {
                                double armor = armorAttr.getValue();
                                double ignoreAmount = armor * 0.20;
                                // Add roughly 4% damage per point of armor ignored
                                double damageBonus = event.getDamage() * (ignoreAmount * 0.04);
                                event.setDamage(event.getDamage() + damageBonus);
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
        }

        // Arrow Damage
        if (event.getDamager() instanceof Projectile proj) {
            if (proj.getShooter() instanceof Player shooter) {
                AstraClass c = getClass(shooter);
                if (c == AstraClass.VANGUARD) {
                    event.setDamage(event.getDamage() * 0.5); // -50% bow
                } else if (c == AstraClass.RANGER) {
                    event.setDamage(event.getDamage() * 1.5); // +50% bow
                }
            }
        }

        // Arrow Damage to Windwalker
        if (event.getEntity() instanceof Player player && event.getDamager() instanceof Arrow) {
            if (getClass(player) == AstraClass.WINDWALKER) {
                event.setDamage(event.getDamage() * 1.5);
            }
        }

        // Vanguard Adrenaline Trigger (Target is Vanguard)
        if (event.getEntity() instanceof Player player) {
            if (getClass(player) == AstraClass.VANGUARD) {
                if (player.getHealth() - event.getFinalDamage() < 9.0) { // < 30% HP (assuming 20 max)
                    long now = System.currentTimeMillis();
                    long last = manager.vanguardAdrenalineCooldowns.getOrDefault(player.getUniqueId(), 0L);
                    if (now - last > 30000) { // 30s cooldown
                        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
                        manager.vanguardAdrenalineCooldowns.put(player.getUniqueId(), now);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (getClass(player) == AstraClass.VANGUARD) {
                // Deplete faster. If food level goes down
                if (event.getFoodLevel() < player.getFoodLevel()) {
                    // Random chance to lose extra food
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        event.setFoodLevel(Math.max(0, event.getFoodLevel() - 1));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (getClass(player) == AstraClass.SHADOW && event.isSneaking()) {
            if (player.getLocation().getBlock().getLightLevel() < 7) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false, true));
            }
        } else if (getClass(player) == AstraClass.SHADOW && !event.isSneaking()) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (getClass(player) == AstraClass.RANGER) {
                Entity proj = event.getProjectile();
                proj.setVelocity(proj.getVelocity().multiply(1.2)); // +20% speed
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    event.setConsumeItem(false); // 30% save
                }
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player killer = event.getEntity().getKiller();
            if (getClass(killer) == AstraClass.BERSERKER) {
                killer.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
                killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
            }
        }
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (getClass(event.getPlayer()) == AstraClass.BERSERKER) {
            if (event.getItem().getType().name().contains("HELMET") ||
                    event.getItem().getType().name().contains("CHESTPLATE") ||
                    event.getItem().getType().name().contains("LEGGINGS") ||
                    event.getItem().getType().name().contains("BOOTS")) {

                // 30% faster degradation -> 30% chance to take an extra point of damage
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    event.setDamage(event.getDamage() + 1);
                }
            }
        }
    }

    @EventHandler
    public void onGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player) {
            AstraClass c = getClass(player);
            if (c == AstraClass.JUGGERNAUT) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL && player.getGameMode() != org.bukkit.GameMode.ADVENTURE) return;
        
        if (getClass(player) == AstraClass.WINDWALKER) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setGliding(true);
            // Small boost to start gliding
            player.setVelocity(player.getLocation().getDirection().multiply(1.2).setY(0.4));
        }
    }

    // Handle Windwalker innate elytra activation + Updraft
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        AstraClass c = getClass(player);

        if (c == AstraClass.WINDWALKER) {
            if (event.getAction().name().contains("RIGHT_CLICK") && player.isSneaking()) {
                long now = System.currentTimeMillis();
                long last = manager.windwalkerUpdraftCooldowns.getOrDefault(player.getUniqueId(), 0L);
                if (now - last > 45000) { // 45s cooldown
                    // We assume disable in combat is checked via some tag, but omitting for brevity
                    // if not stored.
                    player.setVelocity(player.getVelocity().add(new Vector(0, 2.0, 0))); // Launch 20 blocks roughly ~
                                                                                         // velocity 2.0+
                    player.setGliding(true);
                    manager.windwalkerUpdraftCooldowns.put(player.getUniqueId(), now);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        AstraClass c = getClass(player);

        // Prevent Windwalker from removing their innate elytra
        if (c == AstraClass.WINDWALKER) {
            ItemStack current = event.getCurrentItem();
            ItemStack cursor = event.getCursor();

            // Block moving innate elytra OUT of the chestplate slot
            if (current != null && manager.isInnateElytra(current)) {
                event.setCancelled(true);
                return;
            }
            // Block placing another item INTO the chestplate slot
            if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR
                    && event.getSlot() == 38 // chestplate slot
                    && cursor != null && cursor.getType() != org.bukkit.Material.AIR) {
                event.setCancelled(true);
                return;
            }
        }

        manager.validateEquipment(player, c);
    }
}
