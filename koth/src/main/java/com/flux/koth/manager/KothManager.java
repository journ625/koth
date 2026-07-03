package com.flux.koth.manager;

import com.flux.koth.FluxKoth;
import com.flux.koth.model.Koth;
import com.flux.koth.model.KothMode;
import com.flux.koth.model.KothState;
import com.flux.koth.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KothManager {

    private final FluxKoth plugin;
    private File file;
    private FileConfiguration cfg;

    private final Map<String, Koth> koths = new LinkedHashMap<>();
    private final Map<String, KothState> active = new ConcurrentHashMap<>();

    // Inspector módban lévő játékosok
    private final Set<UUID> inspectorMode = new HashSet<>();

    public KothManager(FluxKoth plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);

        koths.clear();
        ConfigurationSection root = cfg.getConfigurationSection("koths");
        if (root != null) {
            for (String name : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(name);
                if (s == null) continue;
                Koth koth = new Koth(name);
                koth.setWorldName(s.getString("world"));
                if (s.contains("x1")) koth.rawSetX1(s.getInt("x1"));
                if (s.contains("y1")) koth.rawSetY1(s.getInt("y1"));
                if (s.contains("z1")) koth.rawSetZ1(s.getInt("z1"));
                if (s.contains("x2")) koth.rawSetX2(s.getInt("x2"));
                if (s.contains("y2")) koth.rawSetY2(s.getInt("y2"));
                if (s.contains("z2")) koth.rawSetZ2(s.getInt("z2"));
                koth.setMode(KothMode.fromString(s.getString("mode", "TIME")));
                koth.setCaptureTime(s.getInt("captureTime", 420));
                koth.setDuration(s.getInt("duration", 420));
                koth.setEnabled(s.getBoolean("enabled", false));
                for (String cmd : s.getStringList("rewardCommands")) {
                    koth.getRewardCommands().add(cmd);
                }
                for (String b64 : s.getStringList("rewardItems")) {
                    ItemStack it = ItemUtil.itemFromBase64(b64);
                    if (it != null) koth.getRewardItems().add(it);
                }
                for (String sch : s.getStringList("schedules")) {
                    koth.getSchedules().add(sch);
                }
                koths.put(name.toLowerCase(), koth);
            }
        }
    }

    public void saveAll() {
        cfg.set("koths", null);
        for (Koth koth : koths.values()) {
            String base = "koths." + koth.getName();
            cfg.set(base + ".world", koth.getWorldName());
            if (koth.getX1() != null) cfg.set(base + ".x1", koth.getX1());
            if (koth.getY1() != null) cfg.set(base + ".y1", koth.getY1());
            if (koth.getZ1() != null) cfg.set(base + ".z1", koth.getZ1());
            if (koth.getX2() != null) cfg.set(base + ".x2", koth.getX2());
            if (koth.getY2() != null) cfg.set(base + ".y2", koth.getY2());
            if (koth.getZ2() != null) cfg.set(base + ".z2", koth.getZ2());
            cfg.set(base + ".mode", koth.getMode().name());
            cfg.set(base + ".captureTime", koth.getCaptureTime());
            cfg.set(base + ".duration", koth.getDuration());
            cfg.set(base + ".enabled", koth.isEnabled());
            cfg.set(base + ".rewardCommands", koth.getRewardCommands());
            List<String> itemsB64 = new ArrayList<>();
            for (ItemStack it : koth.getRewardItems()) {
                String b64 = ItemUtil.itemToBase64(it);
                if (b64 != null) itemsB64.add(b64);
            }
            cfg.set(base + ".rewardItems", itemsB64);
            cfg.set(base + ".schedules", koth.getSchedules());
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== Koth CRUD ====================

    public Koth getKoth(String name) {
        return koths.get(name.toLowerCase());
    }

    public boolean exists(String name) {
        return koths.containsKey(name.toLowerCase());
    }

    public Koth createKoth(String name) {
        Koth k = new Koth(name);
        koths.put(name.toLowerCase(), k);
        saveAll();
        return k;
    }

    public void removeKoth(String name) {
        stopKoth(name);
        koths.remove(name.toLowerCase());
        saveAll();
    }

    public Collection<Koth> getAllKoths() {
        return koths.values();
    }

    public List<String> getKothNames() {
        List<String> names = new ArrayList<>();
        for (Koth k : koths.values()) names.add(k.getName());
        return names;
    }

    // ==================== Inspector mode ====================

    public boolean isInInspectorMode(UUID uuid) {
        return inspectorMode.contains(uuid);
    }

    public void toggleInspectorMode(UUID uuid) {
        if (inspectorMode.contains(uuid)) inspectorMode.remove(uuid);
        else inspectorMode.add(uuid);
    }

    public void setInspectorMode(UUID uuid, boolean value) {
        if (value) inspectorMode.add(uuid);
        else inspectorMode.remove(uuid);
    }

    // ==================== Active state management ====================

    public boolean isActive(String name) {
        return active.containsKey(name.toLowerCase());
    }

    public KothState getState(String name) {
        return active.get(name.toLowerCase());
    }

    public Collection<KothState> getActiveStates() {
        return active.values();
    }

    public boolean startKoth(String name) {
        return startKoth(name, null);
    }

    /**
     * @param overrideSeconds ha nem null, csak erre az egy indításra érvényesen felülírja
     *                        a koth alapértelmezett capture/duration idejét.
     */
    public boolean startKoth(String name, Integer overrideSeconds) {
        Koth koth = getKoth(name);
        if (koth == null) return false;
        if (isActive(name)) return false;
        if (!koth.isReadyForEnable()) return false;

        KothState state = new KothState(koth, overrideSeconds);

        if (plugin.getConfig().getBoolean("settings.bossbar.enabled", true)) {
            BarColor color = safeBarColor(plugin.getConfig().getString("settings.bossbar.color", "YELLOW"));
            BarStyle style = safeBarStyle(plugin.getConfig().getString("settings.bossbar.style", "SOLID"));
            BossBar bar = Bukkit.createBossBar(koth.getName(), color, style);
            for (Player p : Bukkit.getOnlinePlayers()) bar.addPlayer(p);
            bar.setVisible(true);
            state.setBossBar(bar);
        }

        active.put(name.toLowerCase(), state);
        return true;
    }

    public void stopKoth(String name) {
        KothState state = active.remove(name.toLowerCase());
        if (state != null && state.getBossBar() != null) {
            state.getBossBar().removeAll();
            state.getBossBar().setVisible(false);
        }
    }

    public void stopAll() {
        for (String name : new ArrayList<>(active.keySet())) {
            stopKoth(name);
        }
    }

    public void addPlayerToAllBossBars(Player p) {
        for (KothState s : active.values()) {
            if (s.getBossBar() != null) s.getBossBar().addPlayer(p);
        }
    }

    private BarColor safeBarColor(String s) {
        try {
            return BarColor.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return BarColor.YELLOW;
        }
    }

    private BarStyle safeBarStyle(String s) {
        try {
            return BarStyle.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return BarStyle.SOLID;
        }
    }

    public FileConfiguration getRawConfig() {
        return cfg;
    }
}
