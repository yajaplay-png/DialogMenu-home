package com.example.myhomes.gui;

import com.example.myhomes.Home;
import com.example.myhomes.MyHomesPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HomesGUIListener implements Listener {

    private final MyHomesPlugin plugin;
    // tracks which page each player currently has open
    private final Map<UUID, Integer> openPages = new HashMap<>();

    public HomesGUIListener(MyHomesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        openPages.put(player.getUniqueId(), page);
        player.openInventory(plugin.getHomesGUI().build(player, page));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!HomesGUI.TITLE_PREFIX.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
                ? clicked.getItemMeta().getDisplayName()
                : "";

        int page = openPages.getOrDefault(player.getUniqueId(), 0);

        switch (clicked.getType()) {
            case LIME_STAINED_GLASS_PANE -> { // "New Home"
                Home created = plugin.getHomeManager().createHome(player);
                if (created != null) {
                    player.sendMessage("§aCreated " + created.getName() + " at your current location.");
                } else {
                    player.sendMessage("§cYou've reached your home limit for your current rank.");
                }
                open(player, page); // refresh
            }
            case RED_STAINED_GLASS_PANE -> // "Locked"
                    player.sendMessage("§cThis home slot is locked. Upgrade your rank to unlock more homes.");
            case ARROW -> {
                if ("Show More".equals(name)) {
                    open(player, page + 1);
                } else if ("Previous Page".equals(name)) {
                    open(player, Math.max(0, page - 1));
                }
            }
            default -> {
                // clicked an existing home icon -> teleport
                List<Home> homes = plugin.getHomeManager().getHomes(player);
                homes.stream()
                        .filter(h -> h.getName().equals(name))
                        .findFirst()
                        .ifPresent(home -> {
                            player.closeInventory();
                            player.teleport(home.getLocation());
                            player.sendMessage("§aTeleported to " + home.getName() + ".");
                        });
            }
        }
    }
}
