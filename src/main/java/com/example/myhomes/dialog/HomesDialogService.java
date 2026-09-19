package com.example.myhomes.dialog;

import com.example.myhomes.Home;
import com.example.myhomes.MyHomesPlugin;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public class HomesDialogService {
    private static final int BUTTON_WIDTH = 90;
    private static final TextColor BLUE = TextColor.fromHexString("#3B82F6");
    private static final TextColor WHITE = TextColor.fromHexString("#FFFFFF");

    private final MyHomesPlugin plugin;
    private final Map<UUID, Integer> visibleCount = new HashMap<>();
    private final Map<UUID, String> iconQuery = new HashMap<>();
    private final Map<UUID, Integer> iconVisibleCount = new HashMap<>();

    private static final List<Material> ALL_ICONS = java.util.Arrays.stream(Material.values())
            .filter(Material::isItem)
            .filter(m -> !m.isLegacy())
            .filter(m -> m != Material.AIR)
            .sorted(Comparator.comparing(Enum::name))
            .toList();

    public HomesDialogService(MyHomesPlugin plugin) {
        this.plugin = plugin;
    }

    /*
     * IMPORTANT:
     * Do not use customClick(Key, null) here. That creates a client custom-click
     * event and then the old listener closes/reopens the dialog. Every internal
     * MyHomes navigation is now a local callback attached to the button itself.
     */
    private DialogAction action(BiConsumer<Player, DialogResponseView> handler) {
        return DialogAction.customClick((view, audience) -> {
            if (!(audience instanceof Player player)) return;
            handler.accept(player, view);
        }, ClickCallback.Options.builder()
                .uses(1)
                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                .build());
    }

    private int getVisibleCount(Player player, int batchSize, int hardCap) {
        int current = visibleCount.getOrDefault(player.getUniqueId(), Math.min(batchSize, hardCap));
        return Math.min(current, hardCap);
    }

    public void revealMore(Player player, int batchSize, int hardCap) {
        int current = getVisibleCount(player, batchSize, hardCap);
        visibleCount.put(player.getUniqueId(), Math.min(current + batchSize, hardCap));
    }

    public void revealLess(Player player, int batchSize, int hardCap) {
        int current = getVisibleCount(player, batchSize, hardCap);
        visibleCount.put(player.getUniqueId(), Math.max(current - batchSize, batchSize));
    }

    public Dialog buildHomesList(Player player) {
        List<Home> homeList = plugin.getHomeManager().getHomes(player);
        int rankMax = plugin.getHomeManager().getMaxHomes(player);
        int hardCap = plugin.getConfig().getInt("absolute-max-homes", 99);
        int columns = plugin.getConfig().getInt("gui.columns", 4);
        int rows = plugin.getConfig().getInt("gui.rows", 3);
        int batch = columns * rows;
        int visible = getVisibleCount(player, batch, hardCap);

        List<ActionButton> buttons = new ArrayList<>();
        for (int i = 0; i < visible; i++) {
            if (i < homeList.size()) {
                int index = i;
                Home home = homeList.get(i);
                buttons.add(ActionButton.builder(Component.text(home.getName()))
                        .width(BUTTON_WIDTH)
                        .tooltip(Component.text("Click to manage this home"))
                        .action(action((p, view) -> {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                            p.showDialog(buildHomeDetail(p, index));
                        }))
                        .build());
            } else if (i < rankMax) {
                buttons.add(ActionButton.builder(Component.text("New Home", NamedTextColor.GREEN))
                        .width(BUTTON_WIDTH)
                        .tooltip(Component.text("Save your current location as a home"))
                        .action(action((p, view) -> {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                            Home created = plugin.getHomeManager().createHome(p);
                            if (created == null) {
                                p.sendMessage("§cYou've reached your home limit for your current rank.");
                            } else {
                                p.sendMessage("§aCreated " + created.getName() + " at your current location.");
                            }
                            p.showDialog(buildHomesList(p));
                        }))
                        .build());
            } else {
                buttons.add(ActionButton.builder(Component.text("Locked", NamedTextColor.RED))
                        .width(BUTTON_WIDTH)
                        .tooltip(Component.text("Upgrade your rank to unlock this home slot"))
                        .build());
            }
        }

        if (visible < hardCap) {
            buttons.add(ActionButton.builder(Component.text("Show More"))
                    .width(BUTTON_WIDTH)
                    .action(action((p, view) -> {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        revealMore(p, batch, hardCap);
                        p.showDialog(buildHomesList(p));
                    }))
                    .build());
        }
        if (visible > batch) {
            buttons.add(ActionButton.builder(Component.text("Previous"))
                    .width(BUTTON_WIDTH)
                    .action(action((p, view) -> {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        revealLess(p, batch, hardCap);
                        p.showDialog(buildHomesList(p));
                    }))
                    .build());
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(
                                Component.text("ꜱᴀɢᴀ", BLUE, TextDecoration.BOLD)
                                        .append(Component.text(" ʜᴏᴍᴇ", WHITE, TextDecoration.BOLD)))
                        .build())
                .type(DialogType.multiAction(buttons, null, columns)));
    }

    public Dialog buildHomeDetail(Player player, int index) {
        List<Home> homes = plugin.getHomeManager().getHomes(player);
        if (index < 0 || index >= homes.size()) return buildHomesList(player);
        Home home = homes.get(index);

        List<ActionButton> buttons = List.of(
                ActionButton.builder(Component.text("Teleport"))
                        .width(BUTTON_WIDTH)
                        .action(action((p, view) -> {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                            startTeleportCountdown(p, home);
                        })).build(),
                ActionButton.builder(Component.text("Change Icon"))
                        .width(BUTTON_WIDTH)
                        .action(action((p, view) -> {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                            setIconQuery(p, null);
                            p.showDialog(buildIconResultsDialog(p, index));
                        })).build(),
                ActionButton.builder(Component.text("Rename"))
                        .width(BUTTON_WIDTH)
                        .action(action((p, view) -> {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                            p.showDialog(buildRenameDialog(p, index));
                        })).build(),
                ActionButton.builder(Component.text("Delete", NamedTextColor.RED))
                        .width(BUTTON_WIDTH)
                        .action(action((p, view) -> {
                            List<Home> current = plugin.getHomeManager().getHomes(p);
                            if (index < 0 || index >= current.size()) return;
                            Home target = current.get(index);
                            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1f, 1f);
                            plugin.getHomeManager().deleteHome(p, target);
                            p.sendMessage("§aDeleted " + target.getName() + ".");
                            p.showDialog(buildHomesList(p));
                        })).build(),
                ActionButton.builder(Component.text("Back"))
                        .width(BUTTON_WIDTH)
                        .action(action((p, view) -> {
                            p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
                            p.showDialog(buildHomesList(p));
                        })).build()
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(home.getName()))
                        .body(List.of(DialogBody.item(new ItemStack(home.getIcon())).build()))
                        .build())
                .type(DialogType.multiAction(buttons, null, 2)));
    }

    public Dialog buildRenameDialog(Player player, int index) {
        List<Home> homes = plugin.getHomeManager().getHomes(player);
        if (index < 0 || index >= homes.size()) return buildHomesList(player);
        Home home = homes.get(index);

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Rename " + home.getName()))
                        .inputs(List.of(DialogInput.text("newname", Component.text("New name")).build()))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Confirm"))
                                .width(BUTTON_WIDTH)
                                .action(action((p, view) -> {
                                    String newName = view == null ? null : view.getText("newname");
                                    if (newName == null || newName.isBlank()) {
                                        p.sendMessage("§cName can't be empty.");
                                        p.showDialog(buildHomeDetail(p, index));
                                        return;
                                    }
                                    List<Home> current = plugin.getHomeManager().getHomes(p);
                                    if (index < 0 || index >= current.size()) return;
                                    plugin.getHomeManager().renameHome(current.get(index), newName.trim());
                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                                    p.sendMessage("§aRenamed to " + newName.trim() + ".");
                                    p.showDialog(buildHomesList(p));
                                }))
                                .build(),
                        ActionButton.builder(Component.text("Cancel"))
                                .width(BUTTON_WIDTH)
                                .action(action((p, view) -> p.showDialog(buildHomeDetail(p, index))))
                                .build()
                )));
    }

    public Dialog buildIconSearchDialog(int index) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Search for an Icon"))
                        .inputs(List.of(DialogInput.text("query", Component.text("Item name (blank = show all)")).build()))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Search"))
                                .width(BUTTON_WIDTH)
                                .action(action((p, view) -> {
                                    setIconQuery(p, view == null ? null : view.getText("query"));
                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                                    p.showDialog(buildIconResultsDialog(p, index));
                                })).build(),
                        ActionButton.builder(Component.text("Cancel"))
                                .width(BUTTON_WIDTH)
                                .action(action((p, view) -> p.showDialog(buildHomeDetail(p, index))))
                                .build()
                )));
    }

    public void setIconQuery(Player player, String query) {
        iconQuery.put(player.getUniqueId(), query);
        iconVisibleCount.remove(player.getUniqueId());
    }

    public String getIconQuery(Player player) {
        return iconQuery.get(player.getUniqueId());
    }

    public Dialog buildIconResultsDialog(Player player, int index) {
        String query = getIconQuery(player);
        List<Material> pool = (query == null || query.isBlank())
                ? ALL_ICONS
                : ALL_ICONS.stream()
                .filter(m -> m.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
                .toList();

        int columns = plugin.getConfig().getInt("gui.icon-columns", 4);
        int rows = plugin.getConfig().getInt("gui.icon-rows", 4);
        int batch = columns * rows;
        int visible = Math.min(iconVisibleCount.getOrDefault(player.getUniqueId(), batch), pool.size());

        List<ActionButton> buttons = new ArrayList<>();
        for (int i = 0; i < visible; i++) {
            Material material = pool.get(i);
            int materialIndex = index;
            buttons.add(ActionButton.builder(Component.text(prettyName(material)))
                    .width(BUTTON_WIDTH)
                    .action(action((p, view) -> {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                        p.showDialog(buildIconPreviewDialog(materialIndex, material));
                    })).build());
        }

        if (visible < pool.size()) {
            buttons.add(ActionButton.builder(Component.text("Show More"))
                    .width(BUTTON_WIDTH)
                    .action(action((p, view) -> {
                        revealMoreIcons(p, batch, countMatchingIcons(p));
                        p.showDialog(buildIconResultsDialog(p, index));
                    })).build());
        }
        buttons.add(ActionButton.builder(Component.text("New Search"))
                .width(BUTTON_WIDTH)
                .action(action((p, view) -> p.showDialog(buildIconSearchDialog(index))))
                .build());
        buttons.add(ActionButton.builder(Component.text("Back"))
                .width(BUTTON_WIDTH)
                .action(action((p, view) -> p.showDialog(buildHomeDetail(p, index))))
                .build());

        String title = (query == null || query.isBlank()) ? "All Items" : "Results: " + query;
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title)).build())
                .type(DialogType.multiAction(buttons, null, columns)));
    }

    public void revealMoreIcons(Player player, int batchSize, int poolSize) {
        int current = iconVisibleCount.getOrDefault(player.getUniqueId(), batchSize);
        iconVisibleCount.put(player.getUniqueId(), Math.min(current + batchSize, poolSize));
    }

    public int countMatchingIcons(Player player) {
        String query = getIconQuery(player);
        if (query == null || query.isBlank()) return ALL_ICONS.size();
        return (int) ALL_ICONS.stream()
                .filter(m -> m.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
                .count();
    }

    public Dialog buildIconPreviewDialog(int index, Material material) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Set icon to " + prettyName(material) + "?"))
                        .body(List.of(DialogBody.item(new ItemStack(material)).build()))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Confirm"))
                                .width(BUTTON_WIDTH)
                                .action(action((p, view) -> {
                                    List<Home> homes = plugin.getHomeManager().getHomes(p);
                                    if (index < 0 || index >= homes.size()) return;
                                    homes.get(index).setIcon(material);
                                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                                    p.sendMessage("§aIcon updated.");
                                    p.showDialog(buildHomeDetail(p, index));
                                })).build(),
                        ActionButton.builder(Component.text("Cancel"))
                                .width(BUTTON_WIDTH)
                                .action(action((p, view) -> p.showDialog(buildIconResultsDialog(p, index))))
                                .build()
                )));
    }

    private String prettyName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1).toLowerCase(Locale.ROOT))
                    .append(' ');
        }
        return sb.toString().trim();
    }

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
