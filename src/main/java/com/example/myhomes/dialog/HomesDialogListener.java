package com.example.myhomes.dialog;

import com.example.myhomes.Home;
import com.example.myhomes.MyHomesPlugin;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.Optional;

public class HomesDialogListener implements Listener {

    private final MyHomesPlugin plugin;

    public HomesDialogListener(MyHomesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        Key identifier = event.getIdentifier();
        if (!"myhomes".equals(identifier.namespace())) return;

        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;
        Player player = connection.getPlayer();

        String[] parts = identifier.value().split("/", 2);
        String action = parts[0];
        String arg = parts.length > 1 ? parts[1] : null;
        HomesDialogService dialogs = plugin.getHomesDialogService();

        switch (action) {
            case "page" -> player.showDialog(dialogs.buildHomesList(player, Integer.parseInt(arg)));

            case "new" -> {
                Home created = plugin.getHomeManager().createHome(player);
                if (created == null) {
                    player.sendMessage("§cYou've reached your home limit for your current rank.");
                } else {
                    player.sendMessage("§aCreated " + created.getName() + " at your current location.");
                }
                player.showDialog(dialogs.buildHomesList(player, dialogs.getLastPage(player)));
            }

            case "open" -> player.showDialog(dialogs.buildHomeDetail(player, arg));

            case "teleport" -> findHome(player, arg).ifPresent(home -> {
                player.teleport(home.getLocation());
                player.sendMessage("§aTeleported to " + home.getName() + ".");
            });

            case "delete" -> findHome(player, arg).ifPresent(home -> {
                plugin.getHomeManager().deleteHome(player, home);
                player.sendMessage("§aDeleted " + home.getName() + ".");
                player.showDialog(dialogs.buildHomesList(player, dialogs.getLastPage(player)));
            });

            case "rename" -> player.showDialog(dialogs.buildRenameDialog(arg));

            case "confirmrename" -> {
                DialogResponseView view = event.getDialogResponseView();
                if (view == null) return;
                String newName = view.getText("newname");
                if (newName == null || newName.isBlank()) {
                    player.sendMessage("§cName can't be empty.");
                    player.showDialog(dialogs.buildHomeDetail(player, arg));
                    return;
                }
                findHome(player, arg).ifPresent(home -> {
                    plugin.getHomeManager().renameHome(home, newName.trim());
                    player.sendMessage("§aRenamed to " + newName.trim() + ".");
                });
                player.showDialog(dialogs.buildHomesList(player, dialogs.getLastPage(player)));
            }

            case "icon" -> player.showDialog(dialogs.buildIconDialog(arg));

            case "confirmicon" -> {
                DialogResponseView view = event.getDialogResponseView();
                if (view == null) return;
                String materialName = view.getText("material");
                Material material = materialName == null ? null : Material.matchMaterial(materialName.trim());
                if (material == null) {
                    player.sendMessage("§cUnknown material name. Try e.g. diamond_block.");
                    player.showDialog(dialogs.buildHomeDetail(player, arg));
                    return;
                }
                Material finalMaterial = material;
                findHome(player, arg).ifPresent(home -> home.setIcon(finalMaterial));
                player.sendMessage("§aIcon updated.");
                player.showDialog(dialogs.buildHomeDetail(player, arg));
            }

            default -> { /* unknown action, ignore */ }
        }
    }

    private Optional<Home> findHome(Player player, String name) {
        List<Home> homes = plugin.getHomeManager().getHomes(player);
        return homes.stream().filter(h -> h.getName().equals(name)).findFirst();
    }
}
