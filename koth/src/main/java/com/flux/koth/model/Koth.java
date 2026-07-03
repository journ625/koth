package com.flux.koth.model;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Koth {

    private final String name;
    private String worldName;

    // Nyers pontkoordináták (WorldEdit-szerű balta kijelölés)
    private Integer x1, y1, z1;
    private Integer x2, y2, z2;

    private KothMode mode = KothMode.TIME;

    // TIME mód: hány másodpercig kell folyamatosan bent lenni a győzelemhez
    private int captureTime = 420; // 7 perc alapértelmezett

    // A koth maximális futási ideje (biztonsági timeout / POINTS mód esetén az esemény hossza)
    private int duration = 420;

    private boolean enabled = false;

    private final List<ItemStack> rewardItems = new ArrayList<>();
    private final List<String> rewardCommands = new ArrayList<>();

    // Ütemezések "HH:mm" formátumban
    private final List<String> schedules = new ArrayList<>();

    public Koth(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }

    public void setPos1(String world, int x, int y, int z) {
        this.worldName = world;
        this.x1 = x; this.y1 = y; this.z1 = z;
    }

    public void setPos2(String world, int x, int y, int z) {
        this.worldName = world;
        this.x2 = x; this.y2 = y; this.z2 = z;
    }

    public boolean hasBothPoints() {
        return x1 != null && y1 != null && z1 != null && x2 != null && y2 != null && z2 != null;
    }

    public Cuboid getCuboid() {
        if (!hasBothPoints() || worldName == null) return null;
        return new Cuboid(worldName, x1, y1, z1, x2, y2, z2);
    }

    public Integer getX1() { return x1; }
    public Integer getY1() { return y1; }
    public Integer getZ1() { return z1; }
    public Integer getX2() { return x2; }
    public Integer getY2() { return y2; }
    public Integer getZ2() { return z2; }

    public void rawSetX1(Integer v) { this.x1 = v; }
    public void rawSetY1(Integer v) { this.y1 = v; }
    public void rawSetZ1(Integer v) { this.z1 = v; }
    public void rawSetX2(Integer v) { this.x2 = v; }
    public void rawSetY2(Integer v) { this.y2 = v; }
    public void rawSetZ2(Integer v) { this.z2 = v; }

    public KothMode getMode() { return mode; }
    public void setMode(KothMode mode) { this.mode = mode; }

    public int getCaptureTime() { return captureTime; }
    public void setCaptureTime(int captureTime) { this.captureTime = captureTime; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<ItemStack> getRewardItems() { return rewardItems; }
    public List<String> getRewardCommands() { return rewardCommands; }

    public List<String> getSchedules() { return schedules; }

    public boolean isReadyForEnable() {
        return hasBothPoints() && worldName != null;
    }
}
