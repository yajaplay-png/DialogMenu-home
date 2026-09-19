package com.example.myhomes.dialog;

import com.example.myhomes.MyHomesPlugin;
import org.bukkit.event.Listener;

/**
 * Dialog navigation is handled by DialogAction callbacks in HomesDialogService.
 *
 * Kept as a Listener so existing plugin bootstrap/registration code does not
 * need to change. There is intentionally no PlayerCustomClickEvent handler
 * here anymore.
 */
public final class HomesDialogListener implements Listener {
    public HomesDialogListener(MyHomesPlugin plugin) {
        // Kept for compatibility with the existing plugin constructor.
    }
}
