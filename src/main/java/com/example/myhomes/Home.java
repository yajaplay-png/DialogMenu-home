package com.example.myhomes;

import org.bukkit.Location;
import org.bukkit.Material;

public class Home {

    private String name;
    private Location location;
    private Material icon;

    public Home(String name, Location location, Material icon) {
        this.name = name;
        this.location = location;
        this.icon = icon == null ? Material.WHITE_BED : icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Material getIcon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }
}
