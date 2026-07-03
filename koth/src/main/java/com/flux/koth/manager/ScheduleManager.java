package com.flux.koth.manager;

import com.flux.koth.FluxKoth;
import com.flux.koth.model.Koth;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ScheduleManager extends BukkitRunnable {

    private final FluxKoth plugin;
    private final SimpleDateFormat format = new SimpleDateFormat("HH:mm");
    private String lastChecked = "";

    public ScheduleManager(FluxKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        String now = format.format(new Date());
        // Csak percváltáskor fut le ténylegesen a logika (a task 20 tick-enként hívódik = 1 mp)
        if (now.equals(lastChecked)) return;
        lastChecked = now;

        KothManager manager = plugin.getKothManager();
        for (Koth koth : manager.getAllKoths()) {
            if (!koth.isEnabled() || !koth.isReadyForEnable()) continue;
            if (manager.isActive(koth.getName())) continue;

            for (String schedule : koth.getSchedules()) {
                // Formátum: "HH:mm" vagy "HH:mm|<másodperc>" (egyedi foglalási idő ehhez az induláshoz)
                String[] parts = schedule.split("\\|", 2);
                String time = parts[0];
                if (!time.equals(now)) continue;

                Integer override = null;
                if (parts.length > 1) {
                    try {
                        override = Integer.parseInt(parts[1].trim());
                    } catch (NumberFormatException ignored) {}
                }
                plugin.startKothInternal(koth.getName(), override);
                break;
            }
        }
    }
}
