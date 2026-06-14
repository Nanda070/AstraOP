package com.astraor.mechanics;

import com.astraor.AstraORPlugin;
import com.astraor.state.AstraClass;
import com.astraor.state.CooldownManager;
import com.astraor.state.KeybindManager.AbilityKeybind;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MechanicsListener implements Listener {

    private final AstraORPlugin plugin;
    private final MechanicsManager manager;

    public MechanicsListener(AstraORPlugin plugin, MechanicsManager manager) {
        this.plugin   = plugin;
        this.manager  = manager;
    }

    private AstraClass getClass(Player player) {
        return plugin.getClassManager().getPlayerClass(player);
    }

    private CooldownManager cd() {
        return manager.getCooldowns();
    }

    // ----------------------------------------------------------------
    //  Урон по игроку
    // ----------------------------------------------------------------

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        AstraClass c = getClass(player);
        if (c == null) return;

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
                if (event.getCause() == EntityDamageEvent.DamageCause.FLY_INTO_WALL
                        || event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                }
            }
            case PHOENIX -> {
                if (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                        || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                        || event.getCause() == EntityDamageEvent.DamageCause.LAVA
                        || event.getCause() == EntityDamageEvent.DamageCause.HOT_FLOOR) {
                    event.setCancelled(true);
                }
            }
            case VAMPIRE -> {
                if (event.getCause() == EntityDamageEvent.DamageCause.FIRE
                        || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK
                        || event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                    event.setDamage(event.getDamage() * 1.5);
                }
            }
            default -> {}
        }
    }

    // ----------------------------------------------------------------
    //  Урон от игрока / по игроку
    // ----------------------------------------------------------------

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Игрок как атакующий
        if (event.getDamager() instanceof Player attacker) {
            AstraClass c = getClass(attacker);
            if (c != null) {
                switch (c) {
                    case ARCHITECT -> event.setDamage(event.getDamage() * 0.7);
                    case VANGUARD  -> event.setDamage(event.getDamage() * 1.2);
                    case RANGER    -> event.setDamage(event.getDamage() * 0.6);
                    case SHADOW -> {
                        if (attacker.isInvisible() || attacker.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                            event.setDamage(event.getDamage() * 1.5);
                            attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
                        }
                    }
                    case VAMPIRE -> {
                        // Lifesteal: лечим ПОСЛЕ того как урон нанесён, поэтому используем getDamage()
                        double heal = event.getDamage() * 0.20;
                        AttributeInstance maxHpAttr = attacker.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                        if (maxHpAttr != null) {
                            double newHp = Math.min(maxHpAttr.getValue(), attacker.getHealth() + heal);
                            attacker.setHealth(newHp);
                        }
                    }
                    case BERSERKER -> {
                        if (event.getEntity() instanceof org.bukkit.entity.LivingEntity target) {
                            AttributeInstance armorAttr = target.getAttribute(Attribute.GENERIC_ARMOR);
                            if (armorAttr != null) {
                                double armor        = armorAttr.getValue();
                                double ignoreAmount  = armor * 0.20;
                                double damageBonus   = event.getDamage() * (ignoreAmount * 0.04);
                                event.setDamage(event.getDamage() + damageBonus);
                            }
                        }
                    }
                    default -> {}
                }
            }
        }

        // Урон стрелой от игрока
        if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player shooter) {
            AstraClass c = getClass(shooter);
            if (c == AstraClass.VANGUARD) {
                event.setDamage(event.getDamage() * 0.5);
            } else if (c == AstraClass.RANGER) {
                event.setDamage(event.getDamage() * 1.5);
            }
        }

        // Windwalker получает увеличенный урон от стрел
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Arrow) {
            if (getClass(victim) == AstraClass.WINDWALKER) {
                event.setDamage(event.getDamage() * 1.5);
            }
        }

        // Vanguard Adrenaline: игрок ПОЛУЧАЕТ урон → проверяем, выживет ли он
        // Используем getFinalDamage() (после брони) для точного расчёта
        if (event.getEntity() instanceof Player target) {
            if (getClass(target) == AstraClass.VANGUARD) {
                double hpAfterHit = target.getHealth() - event.getFinalDamage();
                if (hpAfterHit < 9.0 && hpAfterHit > 0) { // живой, но критически мало HP
                    long cooldownMs = 30_000L;
                    UUID uuid = target.getUniqueId();
                    if (!cd().isOnCooldown(uuid, MechanicsManager.ABILITY_VANGUARD_ADRENALINE, cooldownMs)) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 100, 0));
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
                        cd().setCooldown(uuid, MechanicsManager.ABILITY_VANGUARD_ADRENALINE);
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------------
    //  Голод
    // ----------------------------------------------------------------

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player
                && getClass(player) == AstraClass.VANGUARD) {
            if (event.getFoodLevel() < player.getFoodLevel()) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    event.setFoodLevel(Math.max(0, event.getFoodLevel() - 1));
                }
            }
        }
    }

    // ----------------------------------------------------------------
    //  Невидимость (Shadow / Phantom)
    // ----------------------------------------------------------------

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        AstraClass c = getClass(player);

        if (c == AstraClass.SHADOW) {
            if (event.isSneaking()) {
                if (player.getLocation().getBlock().getLightLevel() < 7) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false, true));
                }
            } else {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        } else if (c == AstraClass.PHANTOM) {
            if (event.isSneaking()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false, true));
            } else {
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }
    }

    // ----------------------------------------------------------------
    //  Ranger — стрельба
    // ----------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player
                && getClass(player) == AstraClass.RANGER) {
            Entity proj = event.getProjectile();
            proj.setVelocity(proj.getVelocity().multiply(1.2));
            if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                event.setConsumeItem(false);
            }
        }
    }

    // ----------------------------------------------------------------
    //  Смерть
    // ----------------------------------------------------------------

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
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(item -> manager.isInnateElytra(item));
    }

    // ----------------------------------------------------------------
    //  Урон предметам
    // ----------------------------------------------------------------

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (manager.isInnateElytra(event.getItem())) {
            event.setCancelled(true);
            return;
        }

        if (getClass(event.getPlayer()) == AstraClass.BERSERKER) {
            String typeName = event.getItem().getType().name();
            if (typeName.contains("HELMET") || typeName.contains("CHESTPLATE")
                    || typeName.contains("LEGGINGS") || typeName.contains("BOOTS")) {
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    event.setDamage(event.getDamage() + 1);
                }
            }
        }
    }

    // ----------------------------------------------------------------
    //  Windwalker — полёт / скольжение
    // ----------------------------------------------------------------

    @EventHandler
    public void onGlide(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player
                && getClass(player) == AstraClass.JUGGERNAUT) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL
                && player.getGameMode() != org.bukkit.GameMode.ADVENTURE) return;

        if (getClass(player) == AstraClass.WINDWALKER) {
            event.setCancelled(true);
            player.setAllowFlight(false);
            player.setGliding(true);
            player.setVelocity(player.getLocation().getDirection().multiply(1.2).setY(0.4));
        }
    }

    // ----------------------------------------------------------------
    //  Druid — еда / цели мобов
    // ----------------------------------------------------------------

    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (getClass(player) == AstraClass.DRUID) {
            String mat = event.getItem().getType().name();
            if (mat.contains("ROTTEN_FLESH") || mat.contains("SPIDER_EYE")
                    || mat.contains("POISONOUS_POTATO") || mat.contains("RAW_BEEF")
                    || mat.contains("RAW_CHICKEN") || mat.contains("RAW_PORKCHOP")
                    || mat.contains("RAW_MUTTON")) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.removePotionEffect(PotionEffectType.HUNGER);
                    player.removePotionEffect(PotionEffectType.POISON);
                    player.setFoodLevel(Math.min(20, player.getFoodLevel() + 2));
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player player
                && getClass(player) == AstraClass.DRUID) {
            if (event.getEntity() instanceof org.bukkit.entity.Monster) {
                String name = event.getEntity().getType().name();
                if (name.contains("SPIDER") || name.contains("BEE")
                        || name.contains("WOLF") || name.contains("POLAR_BEAR")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ----------------------------------------------------------------
    //  Активация способности (keybind)
    // ----------------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // BUG FIX: PlayerInteractEvent fires TWICE per click (once per hand).
        // We only handle the MAIN hand to avoid triggering abilities twice.
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        AstraClass c = getClass(player);
        Action action = event.getAction();

        if (player.isSneaking()) {
            AbilityKeybind bind = plugin.getKeybindManager().getPlayerKeybind(player);
            if (bind == AbilityKeybind.SHIFT_RIGHT_CLICK) {
                // Right-clicking air with a sword doesn't send packets in modern versions.
                // To allow abilities (like dash) to work in the air, we accept LEFT_CLICK_AIR as a fallback.
                if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR) {
                    handleAbilityTrigger(player, c);
                }
            } else if (bind == AbilityKeybind.SHIFT_LEFT_CLICK) {
                if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    handleAbilityTrigger(player, c);
                }
            }
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (plugin.getKeybindManager().getPlayerKeybind(player) == AbilityKeybind.SWAP_HANDS) {
            if (handleAbilityTrigger(player, getClass(player))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getKeybindManager().getPlayerKeybind(player) == AbilityKeybind.DROP_ITEM) {
            if (handleAbilityTrigger(player, getClass(player))) {
                event.setCancelled(true);
            }
        }
    }

    // ----------------------------------------------------------------
    //  handleAbilityTrigger — использует CooldownManager
    // ----------------------------------------------------------------

    private boolean handleAbilityTrigger(Player player, AstraClass c) {
        if (c == null) return false;

        UUID uuid = player.getUniqueId();

        if (c == AstraClass.WINDWALKER) {
            long cooldownMs = 45_000L;
            if (!cd().isOnCooldown(uuid, MechanicsManager.ABILITY_WINDWALKER_UPDRAFT, cooldownMs)) {
                player.setVelocity(player.getVelocity().add(new Vector(0, 2.0, 0)));
                player.setGliding(true);
                cd().setCooldown(uuid, MechanicsManager.ABILITY_WINDWALKER_UPDRAFT);
            } else {
                manager.sendCooldownMessage(player, "Updraft", cd().getRemaining(uuid, MechanicsManager.ABILITY_WINDWALKER_UPDRAFT, cooldownMs));
            }
            return true;
        }

        if (c == AstraClass.PHANTOM) {
            long cooldownMs = 20_000L;
            if (!cd().isOnCooldown(uuid, MechanicsManager.ABILITY_PHANTOM_PHASE, cooldownMs)) {
                org.bukkit.Location loc = player.getLocation();
                org.bukkit.Location target = loc.clone().add(loc.getDirection().multiply(2));
                if (target.getBlock().isPassable() && target.clone().add(0, 1, 0).getBlock().isPassable()) {
                    player.teleport(target);
                    player.getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH, loc, 20);
                    cd().setCooldown(uuid, MechanicsManager.ABILITY_PHANTOM_PHASE);
                } else {
                    player.sendMessage(Component.text("§cНет места для фазового рывка!"));
                }
            } else {
                manager.sendCooldownMessage(player, "Phase Dash", cd().getRemaining(uuid, MechanicsManager.ABILITY_PHANTOM_PHASE, cooldownMs));
            }
            return true;
        }

        if (c == AstraClass.PHOENIX) {
            long cooldownMs = 60_000L;
            if (!cd().isOnCooldown(uuid, MechanicsManager.ABILITY_PHOENIX_SUPERNOVA, cooldownMs)) {
                player.getWorld().spawnParticle(org.bukkit.Particle.FLAME, player.getLocation(), 100, 1, 1, 1, 0.2);
                for (Entity e : player.getNearbyEntities(6, 6, 6)) {
                    if (e instanceof org.bukkit.entity.LivingEntity le && e != player) {
                        le.damage(10.0, player);
                        le.setFireTicks(100);
                    }
                }
                cd().setCooldown(uuid, MechanicsManager.ABILITY_PHOENIX_SUPERNOVA);
            } else {
                manager.sendCooldownMessage(player, "Supernova", cd().getRemaining(uuid, MechanicsManager.ABILITY_PHOENIX_SUPERNOVA, cooldownMs));
            }
            return true;
        }

        if (c == AstraClass.DRUID) {
            long cooldownMs = 45_000L;
            if (!cd().isOnCooldown(uuid, MechanicsManager.ABILITY_DRUID_GRASP, cooldownMs)) {
                for (Entity e : player.getNearbyEntities(10, 10, 10)) {
                    if (e instanceof org.bukkit.entity.LivingEntity le && e != player) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 255));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 80, 128));
                    }
                }
                cd().setCooldown(uuid, MechanicsManager.ABILITY_DRUID_GRASP);
            } else {
                manager.sendCooldownMessage(player, "Nature's Grasp", cd().getRemaining(uuid, MechanicsManager.ABILITY_DRUID_GRASP, cooldownMs));
            }
            return true;
        }

        if (c == AstraClass.VAMPIRE) {
            long cooldownMs = 30_000L;
            if (!cd().isOnCooldown(uuid, MechanicsManager.ABILITY_VAMPIRE_DASH, cooldownMs)) {
                if (player.getHealth() > 6.0) {
                    player.damage(4.0);
                    player.setVelocity(player.getLocation().getDirection().multiply(1.5));
                    for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                        if (e instanceof org.bukkit.entity.LivingEntity le && e != player) {
                            le.damage(8.0, player);
                            le.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                        }
                    }
                    cd().setCooldown(uuid, MechanicsManager.ABILITY_VAMPIRE_DASH);
                } else {
                    player.sendMessage(Component.text("§cНедостаточно здоровья для Кровавого Рывка!"));
                }
            } else {
                manager.sendCooldownMessage(player, "Blood Dash", cd().getRemaining(uuid, MechanicsManager.ABILITY_VAMPIRE_DASH, cooldownMs));
            }
            return true;
        }

        if (c == AstraClass.GOLIATH) {
            long cooldownMs = 35_000L;
            if (!cd().isOnCooldown(uuid, MechanicsManager.ABILITY_GOLIATH_TREMOR, cooldownMs)) {
                player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, player.getLocation(), 1);
                for (Entity e : player.getNearbyEntities(8, 4, 8)) {
                    if (e instanceof org.bukkit.entity.LivingEntity le && e != player) {
                        le.setVelocity(le.getVelocity().add(new Vector(0, 1.2, 0)));
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                    }
                }
                cd().setCooldown(uuid, MechanicsManager.ABILITY_GOLIATH_TREMOR);
            } else {
                manager.sendCooldownMessage(player, "Tremor", cd().getRemaining(uuid, MechanicsManager.ABILITY_GOLIATH_TREMOR, cooldownMs));
            }
            return true;
        }

        return false;
    }

    // ----------------------------------------------------------------
    //  Инвентарь Windwalker
    // ----------------------------------------------------------------

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        AstraClass c = getClass(player);

        if (c == AstraClass.WINDWALKER) {
            ItemStack current = event.getCurrentItem();
            ItemStack cursor  = event.getCursor();

            if (current != null && manager.isInnateElytra(current)) {
                event.setCancelled(true);
                return;
            }
            if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR
                    && event.getSlot() == 38
                    && cursor != null && cursor.getType() != org.bukkit.Material.AIR) {
                event.setCancelled(true);
                return;
            }
        }

        manager.validateEquipment(player, c);
    }
}
