package com.flux.koth.model;

import org.bukkit.Location;
import org.bukkit.World;

public class Cuboid {

    private final String worldName;
    private int minX, minY, minZ, maxX, maxY, maxZ;

    public Cuboid(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(worldName)) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= minX && x <= maxX + 1 &&
                y >= minY && y <= maxY + 1 &&
                z >= minZ && z <= maxZ + 1;
    }

    public Location getCenter() {
        World world = org.bukkit.Bukkit.getWorld(worldName);
        double x = (minX + maxX + 1) / 2.0;
        double y = (minY + maxY + 1) / 2.0;
        double z = (minZ + maxZ + 1) / 2.0;
        return new Location(world, x, y, z);
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
}
