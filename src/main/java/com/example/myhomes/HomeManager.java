package com.example.myhomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HomeManager {

    private final MyHomesPlugin plugin;
    private final File file;
    private final FileConfiguration data;

    // uuid -> ordered homes
    private final Map<UUID, List<Home>> homes = new HashMap<>();

    public HomeManager(MyHomesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "homes.yml");
        if (!file.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create homes.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    private void loadAll() {
        for (String uuidStr : data.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            List<Home> list = new ArrayList<>();
            for (String homeName : data.getConfigurationSection(uuidStr).getKeys(false)) {
                String path = uuidStr + "." + homeName;
                String worldName = data.getString(path + ".world");
                if (worldName == null || Bukkit.getWorld(worldName) == null) continue;
                Location loc = new Location(
                        Bukkit.getWorld(worldName),
                        data.getDouble(path + ".x"),
                        data.getDouble(path + ".y"),
                        data.getDouble(path + ".z"),
                        (float) data.getDouble(path + ".yaw"),
                        (float) data.getDouble(path + ".pitch")
                );
                Material icon = Material.matchMaterial(data.getString(path + ".icon", "WHITE_BED"));
                list.add(new Home(homeName, loc, icon));
            }
            homes.put(uuid, list);
        }
    }

    public void save() {
        for (Map.Entry<UUID, List<Home>> entry : homes.entrySet()) {
            String uuidStr = entry.getKey().toString();
            data.set(uuidStr, null); // clear old entries first
            for (Home home : entry.getValue()) {
                String path = uuidStr + "." + home.getName();
                Location loc = home.getLocation();
                data.set(path + ".world", loc.getWorld().getName());
                data.set(path + ".x", loc.getX());
                data.set(path + ".y", loc.getY());
                data.set(path + ".z", loc.getZ());
                data.set(path + ".yaw", loc.getYaw());
                data.set(path + ".pitch", loc.getPitch());
                data.set(path + ".icon", home.getIcon().name());
            }
        }
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save homes.yml: " + e.getMessage());
        }
    }

    public List<Home> getHomes(Player player) {
        return homes.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>());
    }

    public int getMaxHomes(Player player) {
        return plugin.getLuckPermsHook().getMaxHomes(player);
    }

    public boolean canCreateHome(Player player) {
        return getHomes(player).size() < getMaxHomes(player);
    }

    /**
     * Creates a new home named "Home <n>" (matching the "New Home" button
     * in the screenshot) at the player's current location.
     */
    public Home createHome(Player player) {
        if (!canCreateHome(player)) return null;
        List<Home> list = getHomes(player);
        String name = "Home " + (list.size() + 1);
        Home home = new Home(name, player.getLocation(), Material.WHITE_BED);
        list.add(home);
        save();
        return home;
    }

    public void deleteHome(Player player, Home home) {
        getHomes(player).remove(home);
        save();
    }

    public void renameHome(Home home, String newName) {
        home.setName(newName);
        save();
    }
}
