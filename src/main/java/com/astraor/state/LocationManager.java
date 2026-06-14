package com.astraor.state;

import com.astraor.AstraORPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LocationManager {

    private final AstraORPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    private final Map<String, Location> locations = new HashMap<>();

    public LocationManager(AstraORPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "locations.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать locations.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            String val = config.getString(key);
            if (val != null) {
                Location loc = parseLocation(val);
                if (loc != null) {
                    locations.put(key, loc);
                }
            }
        }
    }

    public void save() {
        for (Map.Entry<String, Location> entry : locations.entrySet()) {
            config.set(entry.getKey(), formatLocation(entry.getValue()));
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить locations.yml: " + e.getMessage());
        }
    }

    public void setLocation(String id, Location loc) {
        locations.put(id, loc);
        save();
    }

    public Location getLocation(String id) {
        return locations.get(id);
    }

    private String formatLocation(Location loc) {
        if (loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }

    private Location parseLocation(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(",");
        if (parts.length < 4) return null;
        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }
}
