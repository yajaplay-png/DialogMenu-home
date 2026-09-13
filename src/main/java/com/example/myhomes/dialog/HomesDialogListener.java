package com.example.myhomes.dialog;

import com.example.myhomes.Home;
import com.example.myhomes.MyHomesPlugin;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

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
        List<Home> homes = plugin.getHomeManager().getHomes(player);

        switch (action) {
            case "home" -> player.showDialog(dialogs.buildHomesList(player));

            case "showmore" -> {
                int columns = plugin.getConfig().getInt("gui.columns", 4);
                int rows = plugin.getConfig().getInt("gui.rows", 3);
                int hardCap = plugin.getConfig().getInt("absolute-max-homes", 99);
                dialogs.revealMore(player, columns * rows, hardCap);
                player.showDialog(dialogs.buildHomesList(player));
            }

            case "new" -> {
                Home created = plugin.getHomeManager().createHome(player);
                if (created == null) {
                    player.sendMessage("§cYou've reached your home limit for your current rank.");
                } else {
                    player.sendMessage("§aCreated " + created.getName() + " at your current location.");
                }
                player.showDialog(dialogs.buildHomesList(player));
            }

            case "open" -> player.showDialog(dialogs.buildHomeDetail(player, Integer.parseInt(arg)));

            case "teleport" -> withHome(homes, arg, home -> startTeleportCountdown(player, home));

            case "delete" -> withHome(homes, arg, home -> {
                plugin.getHomeManager().deleteHome(player, home);
                player.sendMessage("§aDeleted " + home.getName() + ".");
                player.showDialog(dialogs.buildHomesList(player));
            });

            case "rename" -> player.showDialog(dialogs.buildRenameDialog(player, Integer.parseInt(arg)));

            case "confirmrename" -> {
                int index = Integer.parseInt(arg);
                DialogResponseView view = event.getDialogResponseView();
                if (view == null) return;
                String newName = view.getText("newname");
                if (newName == null || newName.isBlank()) {
                    player.sendMessage("§cName can't be empty.");
                    player.showDialog(dialogs.buildHomeDetail(player, index));
                    return;
                }
                withHome(homes, arg, home -> {
                    plugin.getHomeManager().renameHome(home, newName.trim());
                    player.sendMessage("§aRenamed to " + newName.trim() + ".");
                });
                player.showDialog(dialogs.buildHomesList(player));
            }

            // --- icon flow: search -> results (text only) -> preview (real icon) -> confirm ---

            case "icon" -> {
                int index = Integer.parseInt(arg);
                dialogs.setIconQuery(player, null);
                player.showDialog(dialogs.buildIconResultsDialog(player, index));
            }

            case "iconsearchprompt" -> player.showDialog(dialogs.buildIconSearchDialog(Integer.parseInt(arg)));

            case "iconsearch" -> {
                int index = Integer.parseInt(arg);
                DialogResponseView view = event.getDialogResponseView();
                String query = view == null ? null : view.getText("query");
                dialogs.setIconQuery(player, query);
                player.showDialog(dialogs.buildIconResultsDialog(player, index));
            }

            case "iconshowmore" -> {
                int index = Integer.parseInt(arg);
                int columns = plugin.getConfig().getInt("gui.icon-columns", 4);
                int rows = plugin.getConfig().getInt("gui.icon-rows", 4);
                dialogs.revealMoreIcons(player, columns * rows, dialogs.countMatchingIcons(player));
                player.showDialog(dialogs.buildIconResultsDialog(player, index));
            }

            case "iconresults" -> {
                int index = Integer.parseInt(arg);
                player.showDialog(dialogs.buildIconResultsDialog(player, index));
            }

            case "iconpick" -> {
                String[] p = arg.split("/", 2);
                int index = Integer.parseInt(p[0]);
                Material material = Material.matchMaterial(p[1]);
                if (material == null) return;
                player.showDialog(dialogs.buildIconPreviewDialog(index, material));
            }

            case "iconconfirm" -> {
                String[] p = arg.split("/", 2);
                int index = Integer.parseInt(p[0]);
                Material material = Material.matchMaterial(p[1]);
                if (material != null) {
                    withHome(homes, String.valueOf(index), home -> {
                        home.setIcon(material);
                        player.sendMessage("§aIcon updated.");
                    });
                }
                player.showDialog(dialogs.buildHomeDetail(player, index));
            }

            default -> { /* unknown action, ignore */ }
        }
    }

    private void withHome(List<Home> homes, String indexStr, java.util.function.Consumer<Home> action) {
        int index = Integer.parseInt(indexStr);
        if (index < 0 || index >= homes.size()) return;
        action.accept(homes.get(index));
    }

    /**
     * Counts down from teleport-delay-seconds, showing "Teleporting in N..."
     * in the action bar (bottom-center of the screen). Cancels if the player
     * moves during the countdown.
     */
    private void startTeleportCountdown(Player player, Home home) {
        int delay = plugin.getConfig().getInt("teleport-delay-seconds", 5);
        Location startLocation = player.getLocation();
        Location destination = home.getLocation();
        String homeName = home.getName();

        BukkitTask[] taskHolder = new BukkitTask[1];
        int[] remaining = {delay};

        taskHolder[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                taskHolder[0].cancel();
                return;
            }
            if (hasMoved(player.getLocation(), startLocation)) {
                player.sendActionBar(Component.text("§cTeleport cancelled - you moved!"));
                taskHolder[0].cancel();
                return;
            }
            if (remaining[0] <= 0) {
                player.teleport(destination);
                player.sendActionBar(Component.text("§aTeleported to " + homeName + "."));
                taskHolder[0].cancel();
                return;
            }
            player.sendActionBar(Component.text("§eTeleporting in §c" + remaining[0] + "§e..."));
            remaining[0]--;
        }, 0L, 20L);
    }

    private boolean hasMoved(Location current, Location start) {
        if (!current.getWorld().equals(start.getWorld())) return true;
        return current.distanceSquared(start) > 0.01;
    }
}
