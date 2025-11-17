package com.omniblock.flappymobs.flight;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class Waypoint {

    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    public Waypoint(String worldName, int x, int y, int z) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Location getAsLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

    public String saveToString() {
        return x + "%" + y + "%" + z + "%" + worldName;
    }

    public static Waypoint loadFromString(String wpData) {
        String[] parts = wpData.split("%");
        if (parts.length != 4) {
            return null;
        }

        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            String world = parts[3];
            return new Waypoint(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "Waypoint(" + x + ", " + y + ", " + z + ", " + worldName + ")";
    }
}