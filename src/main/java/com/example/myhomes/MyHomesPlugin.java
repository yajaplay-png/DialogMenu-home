package com.example.myhomes;

import com.example.myhomes.commands.HomesCommand;
import com.example.myhomes.dialog.HomesDialogService;
import org.bukkit.plugin.java.JavaPlugin;

public class MyHomesPlugin extends JavaPlugin {
    private HomeManager homeManager;
    private LuckPermsHook luckPermsHook;
    private HomesDialogService homesDialogService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.luckPermsHook = new LuckPermsHook(this);
        this.homeManager = new HomeManager(this);
        this.homesDialogService = new HomesDialogService(this);
        getCommand("homes").setExecutor(new HomesCommand(this));
        getLogger().info("MyHomes enabled - max homes are read from LuckPerms meta '"
                + getConfig().getString("luckperms-meta-key") + "'.");
    }

    @Override
    public void onDisable() {
        if (homeManager != null) homeManager.save();
    }

    public HomeManager getHomeManager() { return homeManager; }
    public LuckPermsHook getLuckPermsHook() { return luckPermsHook; }
    public HomesDialogService getHomesDialogService() { return homesDialogService; }
}
