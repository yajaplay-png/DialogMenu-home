package com.example.myhomes.dialog;

import com.example.myhomes.Home;
import com.example.myhomes.MyHomesPlugin;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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
            case "home" -> {
                playSound(player, Sound.BLOCK_CHEST_OPEN);
                showNextTick(player, () -> dialogs.buildHomesList(player));
            }

            case "showmore" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                int columns = plugin.getConfig().getInt("gui.columns", 4);
                int rows = plugin.getConfig().getInt("gui.rows", 3);
                int hardCap = plugin.getConfig().getInt("absolute-max-homes", 99);
                dialogs.revealMore(player, columns * rows, hardCap);
                showNextTick(player, () -> dialogs.buildHomesList(player));
            }

            case "showless" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                int columns = plugin.getConfig().getInt("gui.columns", 4);
                int rows = plugin.getConfig().getInt("gui.rows", 3);
                int hardCap = plugin.getConfig().getInt("absolute-max-homes", 99);
                dialogs.revealLess(player, columns * rows, hardCap);
                showNextTick(player, () -> dialogs.buildHomesList(player));
            }

            case "new" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                Home created = plugin.getHomeManager().createHome(player);
                if (created == null) {
                    player.sendMessage("§cYou've reached your home limit for your current rank.");
                } else {
                    player.sendMessage("§aCreated " + created.getName() + " at your current location.");
                }
                showNextTick(player, () -> dialogs.buildHomesList(player));
            }

            case "open" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                showNextTick(player, () -> dialogs.buildHomeDetail(player, Integer.parseInt(arg)));
            }

            case "teleport" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                withHome(homes, arg, home -> startTeleportCountdown(player, home));
            }

            case "delete" -> withHome(homes, arg, home -> {
                playSound(player, Sound.BLOCK_ANVIL_LAND);
                plugin.getHomeManager().deleteHome(player, home);
                player.sendMessage("§aDeleted " + home.getName() + ".");
                showNextTick(player, () -> dialogs.buildHomesList(player));
            });

            case "rename" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                showNextTick(player, () -> dialogs.buildRenameDialog(player, Integer.parseInt(arg)));
            }

            case "confirmrename" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                int index = Integer.parseInt(arg);
                DialogResponseView view = event.getDialogResponseView();
                if (view == null) return;
                String newName = view.getText("newname");
                if (newName == null || newName.isBlank()) {
                    player.sendMessage("§cName can't be empty.");
                    showNextTick(player, () -> dialogs.buildHomeDetail(player, index));
                    return;
                }
                withHome(homes, arg, home -> {
                    plugin.getHomeManager().renameHome(home, newName.trim());
                    player.sendMessage("§aRenamed to " + newName.trim() + ".");
                });
                showNextTick(player, () -> dialogs.buildHomesList(player));
            }

            // --- icon flow: search -> results (text only) -> preview (real icon) -> confirm ---

            case "icon" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                int index = Integer.parseInt(arg);
                dialogs.setIconQuery(player, null);
                showNextTick(player, () -> dialogs.buildIconResultsDialog(player, index));
            }

            case "iconsearchprompt" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                showNextTick(player, () -> dialogs.buildIconSearchDialog(Integer.parseInt(arg)));
            }

            case "iconsearch" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                int index = Integer.parseInt(arg);
                DialogResponseView view = event.getDialogResponseView();
                String query = view == null ? null : view.getText("query");
                dialogs.setIconQuery(player, query);
                showNextTick(player, () -> dialogs.buildIconResultsDialog(player, index));
            }

            case "iconshowmore" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                int index = Integer.parseInt(arg);
                int columns = plugin.getConfig().getInt("gui.icon-columns", 4);
                int rows = plugin.getConfig().getInt("gui.icon-rows", 4);
                dialogs.revealMoreIcons(player, columns * rows, dialogs.countMatchingIcons(player));
                showNextTick(player, () -> dialogs.buildIconResultsDialog(player, index));
            }

            case "iconresults" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                int index = Integer.parseInt(arg);
                showNextTick(player, () -> dialogs.buildIconResultsDialog(player, index));
            }

            case "iconpick" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                String[] p = arg.split("/", 2);
                int index = Integer.parseInt(p[0]);
                Material material = Material.matchMaterial(p[1]);
                if (material == null) return;
                showNextTick(player, () -> dialogs.buildIconPreviewDialog(index, material));
            }

            case "iconconfirm" -> {
                playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
                String[] p = arg.split("/", 2);
                int index = Integer.parseInt(p[0]);
                Material material = Material.matchMaterial(p[1]);
                if (material != null) {
                    withHome(homes, String.valueOf(index), home -> {
                        home.setIcon(material);
                        player.sendMessage("§aIcon updated.");
                    });
                }
                showNextTick(player, () -> dialogs.buildHomeDetail(player, index));
            }

            default -> { /* unknown action, ignore */ }
        }
    }

    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    /**
     * Paper closes/updates the current dialog as part of handling a custom
     * dialog click. Opening another dialog from inside the same click event
     * can race that client update and produce a visible screen flicker.
     *
     * Re-open the next dialog on the next server tick so the original click
     * transaction has finished first.
     */
    private void showNextTick(Player player, java.util.function.Supplier<io.papermc.paper.dialog.Dialog> dialog) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.showDialog(dialog.get());
            }
        });
    }

    private void withHome(List<Home> homes, String indexStr, java.util.function.Consumer<Home> action) {
        int index = Integer.parseInt(indexStr);
        if (index < 0 || index >= homes.size()) return;
        action.accept(homes.get(index));
    }

    // exact hex colors (not a gradient) for the small-caps "ᴛᴇʟᴇᴘᴏʀᴛ ɪɴ" text
    private static final TextColor BLUE = TextColor.fromHexString("#3B82F6");
    private static final TextColor WHITE = TextColor.fromHexString("#FFFFFF");

    /**
     * Counts down from teleport-delay-seconds, showing "ᴛᴇʟᴇᴘᴏʀᴛ ɪɴ N..."
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
                player.sendActionBar(Component.text("Teleport cancelled - you moved!", NamedTextColor.RED));
                taskHolder[0].cancel();
                return;
            }
            if (remaining[0] <= 0) {
                player.teleport(destination);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                player.sendActionBar(Component.text("Teleported to " + homeName + ".", NamedTextColor.GREEN));
                taskHolder[0].cancel();
                return;
            }
            Component countdown = Component.text("ᴛᴇʟᴇᴘᴏʀᴛ", BLUE)
                    .append(Component.text(" ɪɴ ", WHITE))
                    .append(Component.text(remaining[0], BLUE))
                    .append(Component.text("...", WHITE));
            player.sendActionBar(countdown);
            remaining[0]--;
        }, 0L, 20L);
    }

    private boolean hasMoved(Location current, Location start) {
        if (!current.getWorld().equals(start.getWorld())) return true;
        return current.distanceSquared(start) > 0.01;
    }
}
