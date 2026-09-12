package com.example.myhomes.gui;

import com.example.myhomes.Home;
import com.example.myhomes.MyHomesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class HomesGUI {

    public static final String TITLE_PREFIX = "Homes";

    private final MyHomesPlugin plugin;

    public HomesGUI(MyHomesPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * page 0 = first screen. Each page shows up to `perPage` buttons
     * (existing homes + one "New Home" slot if under the player's max),
     * anything beyond the player's absolute-max-homes cap is never shown
     * at all - it just stops. Anything between the player's rank limit
     * and the absolute cap shows as a red "Locked" button, matching the
     * reference screenshot.
     */
    public Inventory build(Player player, int page) {
        int rows = plugin.getConfig().getInt("gui.rows", 6);
        int size = rows * 9;
        String title = TITLE_PREFIX;
        Inventory inv = Bukkit.createInventory(null, size, title);

        List<Home> homeList = plugin.getHomeManager().getHomes(player);
        int rankMax = plugin.getHomeManager().getMaxHomes(player);
        int hardCap = plugin.getConfig().getInt("absolute-max-homes", 99);

        int perPage = size - 9; // reserve the bottom row for Back / Show More
        int startIndex = page * perPage;

        int slot = 0;
        for (int i = startIndex; i < hardCap && slot < perPage; i++) {
            if (i < homeList.size()) {
                inv.setItem(slot, buildHomeItem(homeList.get(i)));
            } else if (i < rankMax) {
                inv.setItem(slot, buildNewHomeItem());
            } else {
                inv.setItem(slot, buildLockedItem());
            }
            slot++;
        }

        // bottom row nav
        int bottomRowStart = size - 9;
        boolean hasMore = startIndex + perPage < hardCap;
        if (hasMore) {
            inv.setItem(bottomRowStart + 8, namedItem(Material.ARROW, "Show More"));
        }
        if (page > 0) {
            inv.setItem(bottomRowStart, namedItem(Material.ARROW, "Previous Page"));
        }

        return inv;
    }

    private ItemStack buildHomeItem(Home home) {
        return namedItem(home.getIcon(), home.getName());
    }

    private ItemStack buildNewHomeItem() {
        return namedItem(Material.LIME_STAINED_GLASS_PANE, "New Home");
    }

    private ItemStack buildLockedItem() {
        ItemStack item = namedItem(Material.RED_STAINED_GLASS_PANE, "§cLocked");
        ItemMeta meta = item.getItemMeta();
        meta.setLore(List.of("§7Requires a higher rank", "§7to unlock this home slot."));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
