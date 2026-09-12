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
                        .action(DialogAction.customClick(key("open/" + home.getName()), null))
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
     */
    public Dialog buildHomeDetail(Player player, String homeName) {
        List<ActionButton> buttons = List.of(
                ActionButton.builder(Component.text("Teleport"))
                        .action(DialogAction.customClick(key("teleport/" + homeName), null))
                        .build(),
                ActionButton.builder(Component.text("Change Icon"))
                        .action(DialogAction.customClick(key("icon/" + homeName), null))
                        .build(),
                ActionButton.builder(Component.text("Rename"))
                        .action(DialogAction.customClick(key("rename/" + homeName), null))
                        .build(),
                ActionButton.builder(Component.text("Delete", NamedTextColor.RED))
                        .action(DialogAction.customClick(key("delete/" + homeName), null))
                        .build(),
                ActionButton.builder(Component.text("Back"))
                        .action(DialogAction.customClick(key("page/" + getLastPage(player)), null))
                        .build()
        );

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(homeName)).build())
                .type(DialogType.multiAction(buttons, null, 2)));
    }

    /**
     * Text-input dialog for renaming a home. The typed value comes back
     * in the confirm button's DialogResponseView under the key "newname".
     */
    public Dialog buildRenameDialog(String homeName) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Rename " + homeName))
                        .inputs(List.of(
                                DialogInput.text("newname", Component.text("New name")).build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Confirm"))
                                .action(DialogAction.customClick(key("confirmrename/" + homeName), null))
                                .build(),
                        ActionButton.builder(Component.text("Cancel"))
                                .action(DialogAction.customClick(key("open/" + homeName), null))
                                .build()
                )));
    }

    /**
     * Text-input dialog for changing a home's icon. Player types a vanilla
     * material name (e.g. "diamond_block"); validated when they confirm.
     */
    public Dialog buildIconDialog(String homeName) {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Change Icon: " + homeName))
                        .inputs(List.of(
                                DialogInput.text("material", Component.text("Material name (e.g. diamond_block)")).build()
                        ))
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.builder(Component.text("Confirm"))
                                .action(DialogAction.customClick(key("confirmicon/" + homeName), null))
                                .build(),
                        ActionButton.builder(Component.text("Cancel"))
                                .action(DialogAction.customClick(key("open/" + homeName), null))
                                .build()
                )));
    }
}
