package com.astraor.mechanics;

import com.astraor.AstraORPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BossManager implements Listener {

    private final AstraORPlugin plugin;
    private final NamespacedKey bossKey;
    private final File dataFile;
    private YamlConfiguration dataConfig;
    private final List<Location> bossSpawns = new ArrayList<>();

    public BossManager(AstraORPlugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, "boss_npc");
        this.dataFile = new File(plugin.getDataFolder(), "boss_data.yml");
        loadData();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public NamespacedKey getBossKey() {
        return bossKey;
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.contains("spawns")) {
            List<?> list = dataConfig.getList("spawns");
            if (list != null) {
                for (Object obj : list) {
                    if (obj instanceof Location loc) {
                        bossSpawns.add(loc);
                    }
                }
            }
        }
        
        // On load, start respawn check for any spawns that don't have a boss currently alive nearby
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Location loc : bossSpawns) {
                checkAndRespawn(loc);
            }
        }, 100L);
    }

    public void saveSpawns() {
        dataConfig.set("spawns", bossSpawns);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void spawnBoss(Location location) {
        WitherSkeleton boss = (WitherSkeleton) location.getWorld().spawnEntity(location, EntityType.WITHER_SKELETON);
        boss.customName(Component.text("§4§lХранитель Катализатора"));
        boss.setCustomNameVisible(true);
        boss.getPersistentDataContainer().set(bossKey, PersistentDataType.BYTE, (byte) 1);
        
        // Buff boss
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
        boolean found = false;
        if (loc.getWorld() != null) {
            for (org.bukkit.entity.Entity entity : loc.getWorld().getNearbyEntities(loc, 10, 10, 10)) {
                if (entity.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            spawnBoss(loc);
        }
    }

    @EventHandler
    public void onBossDeath(EntityDeathEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE)) {
            event.getDrops().clear();
            event.setDroppedExp(500);
            
            // Drop catalyst
            event.getDrops().add(plugin.getCatalystManager().getCatalystItem());
            
            Location deathLoc = event.getEntity().getLocation();
            Location spawnLoc = null;
            
            for (Location loc : bossSpawns) {
                if (loc.getWorld().equals(deathLoc.getWorld()) && loc.distanceSquared(deathLoc) < 2500) { // Within 50 blocks
                    spawnLoc = loc;
                    break;
                }
            }
            
            if (spawnLoc != null) {
                final Location respawnPoint = spawnLoc;
                long respawnTicks = plugin.getSettings().getBossRespawnTimeTicks();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    spawnBoss(respawnPoint);
                }, respawnTicks);
            }
        }
    }

    public void onDisable() {
        saveSpawns();
    }
}
