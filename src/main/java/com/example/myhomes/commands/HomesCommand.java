package com.example.myhomes.commands;

import com.example.myhomes.MyHomesPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomesCommand implements CommandExecutor {

    private final MyHomesPlugin plugin;

    public HomesCommand(MyHomesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        plugin.getHomesGUIListener().open(player, 0);
        return true;
    }
}
