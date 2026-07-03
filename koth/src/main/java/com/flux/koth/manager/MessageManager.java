package com.flux.koth.manager;

import com.flux.koth.FluxKoth;
import com.flux.koth.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final FluxKoth plugin;
    private File file;
    private FileConfiguration cfg;
    private String prefix = "";

    public MessageManager(FluxKoth plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        cfg = YamlConfiguration.loadConfiguration(file);

        // Beolvassuk az alapértelmezett fájlt is, hogy hiányzó kulcsok pótolva legyenek (frissítés-biztos)
        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration defCfg = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            cfg.setDefaults(defCfg);
            cfg.options().copyDefaults(true);
            try {
                cfg.save(file);
            } catch (IOException ignored) {}
        }

        this.prefix = ColorUtil.color(cfg.getString("Prefix", ""));
    }

    public void reload() {
        load();
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Egy üzenet path pl. "Koth.StartedCamping" vagy "Config.kothCreated"
     */
    public String getRaw(String path) {
        String base = path + ".txt";
        return cfg.getString(base, "&c[hiányzó üzenet: " + path + "]");
    }

    public boolean isEnabled(String path) {
        return cfg.getBoolean(path + ".enabled", true);
    }

    public String build(String path, Map<String, String> replacements) {
        String txt = getRaw(path);
        txt = txt.replace("%prefix%", prefix);
        if (replacements != null) {
            for (Map.Entry<String, String> e : replacements.entrySet()) {
                txt = txt.replace(e.getKey(), e.getValue() == null ? "" : e.getValue());
            }
        }
        return ColorUtil.color(txt);
    }

    public void send(CommandSender target, String path, Map<String, String> replacements) {
        if (!isEnabled(path)) return;
        target.sendMessage(build(path, replacements));
    }

    public void broadcast(String path, Map<String, String> replacements) {
        if (!isEnabled(path)) return;
        String msg = build(path, replacements);
        Bukkit.broadcastMessage(msg);
    }

    public static Map<String, String> of(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
