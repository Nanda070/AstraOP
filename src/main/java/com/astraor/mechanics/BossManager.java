package com.astraor.mechanics;

import com.astraor.AstraORPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BossManager implements Listener {

    private final AstraORPlugin plugin;
    private final NamespacedKey bossKey;
    private final File dataFile;
    private YamlConfiguration dataConfig;
    private final List<Location> bossSpawns = new ArrayList<>();
    // Cooldown между смертью и респавном: ключ = UUID мёртвого босса, значение не важен
    // ConcurrentHashMap — потокобезопасен, UUID нельзя использовать как WeakKey
    private final ConcurrentHashMap<UUID, Boolean> deadBossIds = new ConcurrentHashMap<>();

    public BossManager(AstraORPlugin plugin) {
        this.plugin   = plugin;
        this.bossKey  = new NamespacedKey(plugin, "boss_npc");
        this.dataFile = new File(plugin.getDataFolder(), "boss_data.yml");
        loadData();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickBosses, 100L, 100L); // каждые 5 секунд
    }

    public NamespacedKey getBossKey() {
        return bossKey;
    }

    public List<Location> getBossSpawns() {
        return bossSpawns;
    }

    // ----------------------------------------------------------------
    //  FIX #3: Location сериализуем как строку "world,x,y,z"
    //  FIX #14: проверяем getWorld() != null
    // ----------------------------------------------------------------

    private void loadData() {
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать boss_data.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        List<String> list = dataConfig.getStringList("spawns");
        for (String entry : list) {
            Location loc = parseLocation(entry);
            if (loc != null) {
                bossSpawns.add(loc);
            }
        }

        // После загрузки — проверить, живы ли боссы
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Location loc : List.copyOf(bossSpawns)) {
                checkAndRespawn(loc);
            }
        }, 100L);
    }

    public void saveSpawns() {
        List<String> toSave = new ArrayList<>();
        for (Location loc : bossSpawns) {
            if (loc.getWorld() != null) {
                toSave.add(formatLocation(loc));
            }
        }
        dataConfig.set("spawns", toSave);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить boss_data.yml: " + e.getMessage());
        }
    }

    /** Форматируем Location в строку "worldName,x,y,z". */
    private static String formatLocation(Location loc) {
        return loc.getWorld().getName() + ","
                + loc.getX() + ","
                + loc.getY() + ","
                + loc.getZ();
    }

    /** Парсим строку "worldName,x,y,z" обратно в Location. */
    private static Location parseLocation(String s) {
        if (s == null || s.isBlank()) return null;
        String[] parts = s.split(",");
        if (parts.length != 4) return null;
        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ----------------------------------------------------------------
    //  Спавн и удаление боссов
    // ----------------------------------------------------------------

    public void spawnBoss(Location location) {
        // FIX: getWorld() может быть null если мир выгружен
        if (location.getWorld() == null) {
            plugin.getLogger().warning("spawnBoss: мир не загружен, спавн пропущен.");
            return;
        }

        WitherSkeleton boss = (WitherSkeleton) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);
        boss.customName(Component.text("§4§lХранитель Катализатора"));
        boss.setCustomNameVisible(true);
        boss.getPersistentDataContainer().set(bossKey, PersistentDataType.BYTE, (byte) 1);

        if (boss.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(150.0);
            boss.setHealth(150.0);
        }
        if (boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(25.0);
        }

        if (!bossSpawns.contains(location)) {
            bossSpawns.add(location);
            saveSpawns();
        }
    }

    public void removeBossSpawn(Location location) {
        bossSpawns.removeIf(loc -> loc.distanceSquared(location) < 2500.0);
        saveSpawns();
    }

    private void checkAndRespawn(Location loc) {
        // FIX #14: мир может быть выгружен
        if (loc.getWorld() == null) return;

        boolean found = false;
        for (org.bukkit.entity.Entity entity : loc.getWorld().getNearbyEntities(loc, 10, 10, 10)) {
            if (entity.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) {
                found = true;
                break;
            }
        }
        if (!found) {
            spawnBoss(loc);
        }
    }

    // ----------------------------------------------------------------
    //  Абилки Босса
    // ----------------------------------------------------------------

    private void tickBosses() {
        for (World world : Bukkit.getWorlds()) {
            for (WitherSkeleton boss : world.getEntitiesByClass(WitherSkeleton.class)) {
                if (boss.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) {
                    if (boss.getTarget() != null && !boss.isDead()) {
                        useRandomAbility(boss);
                    }
                }
            }
        }
    }

    private void useRandomAbility(WitherSkeleton boss) {
        int r = java.util.concurrent.ThreadLocalRandom.current().nextInt(3);
        if (r == 0) {
            // Притягивание игроков
            for (org.bukkit.entity.Entity e : boss.getNearbyEntities(15, 15, 15)) {
                if (e instanceof org.bukkit.entity.Player p) {
                    org.bukkit.util.Vector dir = boss.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.5);
                    p.setVelocity(dir);
                    p.sendMessage(Component.text("§4§lХранитель §cпритягивает вас!"));
                }
            }
            boss.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, boss.getLocation(), 100, 2, 2, 2, 1);
            boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        } else if (r == 1) {
            // Огонь
            boss.getWorld().spawnParticle(org.bukkit.Particle.FLAME, boss.getLocation(), 100, 3, 1, 3, 0.1);
            for (org.bukkit.entity.Entity e : boss.getNearbyEntities(10, 5, 10)) {
                if (e instanceof org.bukkit.entity.Player p) {
                    p.setFireTicks(100);
                    p.sendMessage(Component.text("§4§lХранитель §cобжигает вас пламенем!"));
                }
            }
            boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        } else if (r == 2) {
            // Иссушение и замедление
            for (org.bukkit.entity.Entity e : boss.getNearbyEntities(8, 8, 8)) {
                if (e instanceof org.bukkit.entity.Player p) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WITHER, 100, 1));
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 100, 1));
                    p.sendMessage(Component.text("§4§lХранитель §cвытягивает ваши силы!"));
                }
            }
            boss.getWorld().spawnParticle(org.bukkit.Particle.LARGE_SMOKE, boss.getLocation(), 100, 3, 1, 3, 0.1);
            boss.getWorld().playSound(boss.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SHOOT, 1.0f, 0.5f);
        }
    }

    // ----------------------------------------------------------------
    //  Смерть босса
    // ----------------------------------------------------------------

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (!event.getEntity().getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) return;

        event.getDrops().clear();
        event.setDroppedExp(500);
        event.getDrops().add(plugin.getCatalystManager().getCatalystItem());

        Location deathLoc = event.getEntity().getLocation();
        Location spawnLoc = null;

        for (Location loc : bossSpawns) {
            // FIX #14: проверка null перед getWorld()
            if (loc.getWorld() != null
                    && loc.getWorld().equals(deathLoc.getWorld())
                    && loc.distanceSquared(deathLoc) < 2500) {
                spawnLoc = loc;
                break;
            }
        }

        if (spawnLoc != null) {
            final Location respawnPoint = spawnLoc;
            long respawnTicks = plugin.getSettings().getBossRespawnTimeTicks();
            Bukkit.getScheduler().runTaskLater(plugin, () -> spawnBoss(respawnPoint), respawnTicks);
        }
    }

    // Очищаем dead-set при дисконнекте (на случай если игрок был убийцей)
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        deadBossIds.remove(event.getPlayer().getUniqueId());
    }

    public void onDisable() {
        saveSpawns();
    }
}
