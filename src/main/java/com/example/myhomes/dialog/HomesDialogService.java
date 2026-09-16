package com.example.myhomes.dialog;

import com.example.myhomes.Home;
import com.example.myhomes.MyHomesPlugin;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HomesDialogService {

    private static final String NAMESPACE = "myhomes";
    // vanilla's default button width (200) is huge - keep buttons compact
    private static final int BUTTON_WIDTH = 90;
    // exact hex colors (not a gradient) used for the small-caps styled text
    private static final TextColor BLUE = TextColor.fromHexString("#3B82F6");
    private static final TextColor WHITE = TextColor.fromHexString("#FFFFFF");

    private final MyHomesPlugin plugin;
    // how many home slots each player currently has "revealed" via Show More
    // (cumulative - no page/back-and-forth, just grows until the hard cap)
    private final Map<UUID, Integer> visibleCount = new HashMap<>();
    // per-player icon-search state (query text + how many results currently revealed)
    private final Map<UUID, String> iconQuery = new HashMap<>();
    private final Map<UUID, Integer> iconVisibleCount = new HashMap<>();

    // every material usable as an icon, alphabetical
    private static final List<Material> ALL_ICONS = java.util.Arrays.stream(Material.values())
            .filter(Material::isItem)
            .filter(m -> !m.isLegacy())
            .filter(m -> m != Material.AIR)
            .sorted(Comparator.comparing(Enum::name))
            .toList();

    public HomesDialogService(MyHomesPlugin plugin) {
        this.plugin = plugin;
    }

    /** How many home slots are currently revealed for this player (grows via Show More). */
    private int getVisibleCount(Player player, int batchSize, int hardCap) {
        int current = visibleCount.getOrDefault(player.getUniqueId(), Math.min(batchSize, hardCap));
        return Math.min(current, hardCap);
    }

    public void revealMore(Player player, int batchSize, int hardCap) {
        int current = getVisibleCount(player, batchSize, hardCap);
        visibleCount.put(player.getUniqueId(), Math.min(current + batchSize, hardCap));
    }

    /** Shrinks the revealed count back by one batch, never below the initial batch size. */
    public void revealLess(Player player, int batchSize, int hardCap) {
        int current = getVisibleCount(player, batchSize, hardCap);
        visibleCount.put(player.getUniqueId(), Math.max(current - batchSize, batchSize));
    }

    // Minecraft Keys only allow [a-z0-9_\-./]+, so home names (which can have
    // spaces/uppercase) can NEVER go directly into a Key. We reference homes
    // by their list index instead (e.g. "open/0") and look the name up from
    // the player's home list when handling the click.
    private Key key(String value) {
        return Key.key(NAMESPACE, value);
    }

    /**
     * The main "Homes" screen: existing homes, green "New Home" slots up to
     * the player's rank limit, and red "Locked" slots up to the absolute cap.
     * "Show More" reveals additional rows in this SAME dialog cumulatively -
     * there is no paging back and forth, it only grows until the hard cap.
     */
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
                Home home = homeList.get(i);
                buttons.add(ActionButton.builder(Component.text(home.getName()))
                        .width(BUTTON_WIDTH)
                        .tooltip(Component.text("Click to manage this home"))
                        .action(DialogAction.customClick(key("open/" + i), null))
                        .build());
            } else if (i < rankMax) {
                buttons.add(ActionButton.builder(Component.text("New Home", NamedTextColor.GREEN))
                        .width(BUTTON_WIDTH)
                        .tooltip(Component.text("Save your current location as a home"))
                        .action(DialogAction.customClick(key("new"), null))
                        .build());
            } else {
                buttons.add(ActionButton.builder(Component.text("Locked", NamedTextColor.RED))
                        .width(BUTTON_WIDTH)
                        .tooltip(Component.text("Upgrade your rank to unlock this home slot"))
                        .build()); // no .action() -> clicking just closes, nothing happens
            }
        }

        if (visible < hardCap) {
            buttons.add(ActionButton.builder(Component.text("Show More"))
                        .width(BUTTON_WIDTH)
                    .action(DialogAction.customClick(key("showmore"), null))
                    .build());
        }
        if (visible > batch) {
            buttons.add(ActionButton.builder(Component.text("Previous"))
                        .width(BUTTON_WIDTH)
                    .action(DialogAction.customClick(key("showless"), null))
                    .build());
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(
                                Component.text("ꜱᴀɢᴀ", BLUE, TextDecoration.BOLD)
                                        .append(Component.text(" ʜᴏᴍᴇ", WHITE, TextDecoration.BOLD)))
                        .build())
                .type(DialogType.multiAction(buttons, null, columns)));
    }

    /**
     * Per-home submenu: Teleport / Change Icon / Rename / Delete / Back.
     * Referenced by list index, not name.
     */
    public Dialog buildHomeDetail(Player player, int index) {
        Home home = plugin.getHomeManager().getHomes(player).get(index);

        List<ActionButton> buttons = List.of(
                ActionButton.builder(Component.text("Teleport"))
                        .width(BUTTON_WIDTH)
                        .action(DialogAction.customClick(key("teleport/" + index), null))
                        .build(),
                ActionButton.builder(Component.text("Change Icon"))
                        .width(BUTTON_WIDTH)
                        .action(DialogAction.customClick(key("icon/" + index), null))
                        .build(),
                ActionButton.builder(Component.text("Rename"))
                        .width(BUTTON_WIDTH)
                        .action(DialogAction.customClick(key("rename/" + index), null))
                        .build(),
                ActionButton.builder(Component.text("Delete", NamedTextColor.RED))
                        .width(BUTTON_WIDTH)
                        .action(DialogAction.customClick(key("delete/" + index), null))
                        .build(),
                ActionButton.builder(Component.text("Back"))
                        .width(BUTTON_WIDTH)
                        .action(DialogAction.customClick(key("home"), null))
                        .build()
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(home.getName()))
                        .body(List.of(
                                DialogBody.item(new ItemStack(home.getIcon())).build()
                        ))
                        .build())
                .type(DialogType.multiAction(buttons, null, 2)));
    }

    /**
     * Text-input dialog for renaming a home. The typed value comes back
     * in the confirm button's DialogResponseView under the key "newname".
     */
    public Dialog buildRenameDialog(Player player, int index) {
        Home home = plugin.getHomeManager().getHomes(player).get(index);

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Rename " + home.getName()))
                        .inputs(List.of(
                                DialogInput.text("newname", Component.text("New name")).build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Confirm"))
                        .width(BUTTON_WIDTH)
                                .action(DialogAction.customClick(key("confirmrename/" + index), null))
                                .build(),
                        ActionButton.builder(Component.text("Cancel"))
                        .width(BUTTON_WIDTH)
                                .action(DialogAction.customClick(key("open/" + index), null))
                                .build()
                )));
    }

    /**
     * Step 1: ask the player what to search for (blank = show every item).
     */
    public Dialog buildIconSearchDialog(int index) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Search for an Icon"))
                        .inputs(List.of(
                                DialogInput.text("query", Component.text("Item name (blank = show all)")).build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Search"))
                        .width(BUTTON_WIDTH)
                                .action(DialogAction.customClick(key("iconsearch/" + index), null))
                                .build(),
                        ActionButton.builder(Component.text("Cancel"))
                        .width(BUTTON_WIDTH)
                                .action(DialogAction.customClick(key("open/" + index), null))
                                .build()
                )));
    }

    public void setIconQuery(Player player, String query) {
        iconQuery.put(player.getUniqueId(), query);
        iconVisibleCount.remove(player.getUniqueId()); // reset reveal count for the new search
    }

    public String getIconQuery(Player player) {
        return iconQuery.get(player.getUniqueId());
    }

    /**
     * Paginated... no wait, NOT paginated - cumulative, same as the Homes
     * list. Clicking a result goes to the preview screen; the actual icon
     * graphic isn't shown here since Dialog buttons can't carry an item icon
     * - only the preview/body can.
     */
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

        int visible = Math.min(
                iconVisibleCount.getOrDefault(player.getUniqueId(), batch),
                pool.size()
        );

        List<ActionButton> buttons = new ArrayList<>();
        for (int i = 0; i < visible; i++) {
            Material material = pool.get(i);
            buttons.add(ActionButton.builder(Component.text(prettyName(material)))
                        .width(BUTTON_WIDTH)
                    .action(DialogAction.customClick(key("iconpick/" + index + "/" + material.name().toLowerCase(Locale.ROOT)), null))
                    .build());
        }

        if (visible < pool.size()) {
            buttons.add(ActionButton.builder(Component.text("Show More"))
                        .width(BUTTON_WIDTH)
                    .action(DialogAction.customClick(key("iconshowmore/" + index), null))
                    .build());
        }
        buttons.add(ActionButton.builder(Component.text("New Search"))
                        .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(key("iconsearchprompt/" + index), null))
                .build());
        buttons.add(ActionButton.builder(Component.text("Back"))
                        .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(key("open/" + index), null))
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

    /**
     * Step 3: shows the real icon graphic (via the dialog body item) before
     * the player commits to it.
     */
    public Dialog buildIconPreviewDialog(int index, Material material) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Set icon to " + prettyName(material) + "?"))
                        .body(List.of(
                                DialogBody.item(new ItemStack(material)).build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Confirm"))
                        .width(BUTTON_WIDTH)
                                .action(DialogAction.customClick(key("iconconfirm/" + index + "/" + material.name().toLowerCase(Locale.ROOT)), null))
                                .build(),
                        ActionButton.builder(Component.text("Cancel"))
                        .width(BUTTON_WIDTH)
                                .action(DialogAction.customClick(key("iconresults/" + index), null))
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
}
