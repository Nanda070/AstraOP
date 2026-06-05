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
    }

    public NamespacedKey getBossKey() {
        return bossKey;
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
            boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(300.0);
            boss.setHealth(300.0);
        }
        if (boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
            boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(15.0);
        }

        if (!bossSpawns.contains(location)) {
            bossSpawns.add(location);
            saveSpawns();
        }
    }

    public void removeBossSpawn(Location location) {
        bossSpawns.removeIf(loc -> loc.distanceSquared(location) < 4.0);
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
