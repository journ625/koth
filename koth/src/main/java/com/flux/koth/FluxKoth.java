package com.flux.koth;

import com.flux.koth.command.KothCommand;
import com.flux.koth.listener.SelectionListener;
import com.flux.koth.manager.KothManager;
import com.flux.koth.manager.MessageManager;
import com.flux.koth.manager.ScheduleManager;
import com.flux.koth.model.Koth;
import com.flux.koth.placeholder.FluxGuildPlaceholder;
import com.flux.koth.placeholder.FluxPlaceholderExpansion;
import com.flux.koth.task.KothTickTask;
import com.flux.koth.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class FluxKoth extends JavaPlugin {

    private static FluxKoth instance;

    private KothManager kothManager;
    private MessageManager messageManager;
    private SelectionListener selectionListener;

    private KothTickTask tickTask;
    private ScheduleManager scheduleTask;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.messageManager = new MessageManager(this);
        this.kothManager = new KothManager(this);

        this.selectionListener = new SelectionListener(this);
        getServer().getPluginManager().registerEvents(selectionListener, this);

        KothCommand kothCommand = new KothCommand(this);
        getCommand("koth").setExecutor(kothCommand);
        getCommand("koth").setTabCompleter(kothCommand);

        // Fő tick task - minden aktív koth-ot kezel, 20 tick (1 mp) periódussal
        int interval = getConfig().getInt("settings.update-interval-ticks", 20);
        this.tickTask = new KothTickTask(this);
        tickTask.runTaskTimer(this, interval, interval);

        // Ütemezés kezelő - percenként ellenőriz
        this.scheduleTask = new ScheduleManager(this);
        scheduleTask.runTaskTimer(this, 20L, 20L);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FluxPlaceholderExpansion(this).register();
            new FluxGuildPlaceholder(this).register();
            getLogger().info("PlaceholderAPI megtalálva - %fluxkoth_...% és %flux_guild_player% placeholderek regisztrálva.");
        } else {
            getLogger().warning("PlaceholderAPI nem található - a placeholderek nem lesznek elérhetők!");
        }

        getLogger().info("FluxKoth engedélyezve! " + kothManager.getAllKoths().size() + " koth betöltve.");
    }

    @Override
    public void onDisable() {
        if (kothManager != null) {
            kothManager.stopAll();
            kothManager.saveAll();
        }
        if (tickTask != null) tickTask.cancel();
        if (scheduleTask != null) scheduleTask.cancel();
    }

    public void reloadEverything() {
        reloadConfig();
        messageManager.reload();
        kothManager.load();
    }

    /**
     * Belső koth indítás (parancsból vagy ütemezésből) - üzenet küldéssel együtt.
     */
    public void startKothInternal(String name) {
        startKothInternal(name, null);
    }

    /**
     * @param overrideSeconds ha nem null, csak erre az egy indításra érvényesen felülírja
     *                        a koth alapértelmezett capture/duration idejét (parancsból vagy schedule-ból).
     */
    public void startKothInternal(String name, Integer overrideSeconds) {
        Koth koth = kothManager.getKoth(name);
        if (koth == null) return;
        boolean started = kothManager.startKoth(name, overrideSeconds);
        if (started) {
            int time = overrideSeconds != null
                    ? overrideSeconds
                    : (koth.getMode() == com.flux.koth.model.KothMode.TIME ? koth.getCaptureTime() : koth.getDuration());
            messageManager.broadcast("Koth.StartedKoth", MessageManager.of(
                    "%koth%", koth.getName(),
                    "%time%", TimeUtil.format(time)));
        }
    }

    public static FluxKoth getInstance() {
        return instance;
    }

    public KothManager getKothManager() {
        return kothManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public SelectionListener getSelectionListener() {
        return selectionListener;
    }
}
