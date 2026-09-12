package com.example.myhomes.dialog;

import com.example.myhomes.Home;
import com.example.myhomes.MyHomesPlugin;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HomesDialogService {

    private static final String NAMESPACE = "myhomes";

    private final MyHomesPlugin plugin;
    // remembers which list page each player last had open, so "Back" returns them to it
    private final Map<UUID, Integer> lastPage = new HashMap<>();

    public HomesDialogService(MyHomesPlugin plugin) {
        this.plugin = plugin;
    }

    public int getLastPage(Player player) {
        return lastPage.getOrDefault(player.getUniqueId(), 0);
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
     */
    public Dialog buildHomesList(Player player, int page) {
        lastPage.put(player.getUniqueId(), page);

        List<Home> homeList = plugin.getHomeManager().getHomes(player);
        int rankMax = plugin.getHomeManager().getMaxHomes(player);
        int hardCap = plugin.getConfig().getInt("absolute-max-homes", 99);
        int columns = plugin.getConfig().getInt("gui.columns", 4);
        int perPage = plugin.getConfig().getInt("gui.per-page", 20);

        int start = page * perPage;
        int end = Math.min(start + perPage, hardCap);

        List<ActionButton> buttons = new ArrayList<>();
        for (int i = start; i < end; i++) {
            if (i < homeList.size()) {
                Home home = homeList.get(i);
                buttons.add(ActionButton.builder(Component.text(home.getName()))
                        .tooltip(Component.text("Click to manage this home"))
                        .action(DialogAction.customClick(key("open/" + i), null))
                        .build());
            } else if (i < rankMax) {
                buttons.add(ActionButton.builder(Component.text("New Home", NamedTextColor.GREEN))
                        .tooltip(Component.text("Save your current location as a home"))
                        .action(DialogAction.customClick(key("new"), null))
                        .build());
            } else {
                buttons.add(ActionButton.builder(Component.text("Locked", NamedTextColor.RED))
                        .tooltip(Component.text("Upgrade your rank to unlock this home slot"))
                        .build()); // no .action() -> clicking just closes, nothing happens
            }
        }

        if (page > 0) {
            buttons.add(ActionButton.builder(Component.text("Previous Page"))
                    .action(DialogAction.customClick(key("page/" + (page - 1)), null))
                    .build());
        }
        if (end < hardCap) {
            buttons.add(ActionButton.builder(Component.text("Show More"))
                    .action(DialogAction.customClick(key("page/" + (page + 1)), null))
                    .build());
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Homes")).build())
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
                        .action(DialogAction.customClick(key("teleport/" + index), null))
                        .build(),
                ActionButton.builder(Component.text("Change Icon"))
                        .action(DialogAction.customClick(key("icon/" + index), null))
                        .build(),
                ActionButton.builder(Component.text("Rename"))
                        .action(DialogAction.customClick(key("rename/" + index), null))
                        .build(),
                ActionButton.builder(Component.text("Delete", NamedTextColor.RED))
                        .action(DialogAction.customClick(key("delete/" + index), null))
                        .build(),
                ActionButton.builder(Component.text("Back"))
                        .action(DialogAction.customClick(key("page/" + getLastPage(player)), null))
                        .build()
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(home.getName())).build())
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
                                .action(DialogAction.customClick(key("confirmrename/" + index), null))
                                .build(),
                        ActionButton.builder(Component.text("Cancel"))
                                .action(DialogAction.customClick(key("open/" + index), null))
                                .build()
                )));
    }

    /**
     * Text-input dialog for changing a home's icon. Player types a vanilla
     * material name (e.g. "diamond_block"); validated when they confirm.
     */
    public Dialog buildIconDialog(Player player, int index) {
        Home home = plugin.getHomeManager().getHomes(player).get(index);

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Change Icon: " + home.getName()))
                        .inputs(List.of(
                                DialogInput.text("material", Component.text("Material name (e.g. diamond_block)")).build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Confirm"))
                                .action(DialogAction.customClick(key("confirmicon/" + index), null))
                                .build(),
                        ActionButton.builder(Component.text("Cancel"))
                                .action(DialogAction.customClick(key("open/" + index), null))
                                .build()
                )));
    }
}
