package com.example.myhomes;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;

/**
 * Reads a numeric meta value (e.g. "homes-limit") from LuckPerms so that
 * each rank/group can define its own max home count. Falls back to the
 * plugin's default-max-homes if the meta isn't set on any inherited group.
 */
public class LuckPermsHook {

    private final MyHomesPlugin plugin;
    private LuckPerms luckPerms;

    public LuckPermsHook(MyHomesPlugin plugin) {
        this.plugin = plugin;
        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException ex) {
            plugin.getLogger().warning("LuckPerms not found - falling back to default-max-homes for everyone.");
            this.luckPerms = null;
        }
    }

    public int getMaxHomes(Player player) {
        int def = plugin.getConfig().getInt("default-max-homes", 1);
        int cap = plugin.getConfig().getInt("absolute-max-homes", 99);

        if (luckPerms == null) {
            return Math.min(def, cap);
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return Math.min(def, cap);
        }

        String metaKey = plugin.getConfig().getString("luckperms-meta-key", "homes-limit");
        String raw = user.getCachedData()
                .getMetaData(QueryOptions.defaultContextualOptions())
                .getMetaValue(metaKey);

        int value = def;
        if (raw != null) {
            try {
                value = Integer.parseInt(raw.trim());
            } catch (NumberFormatException ignored) {
                // malformed meta on some group - keep default
            }
        }

        return Math.min(Math.max(value, 0), cap);
    }
}
