package com.flux.koth.placeholder;

import com.flux.koth.FluxKoth;
import com.flux.koth.manager.KothManager;
import com.flux.koth.model.Koth;
import com.flux.koth.model.KothMode;
import com.flux.koth.model.KothState;
import com.flux.koth.util.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Kezeli az összes %fluxkoth_...% placeholdert.
 *
 * Globális placeholderek:
 *   %fluxkoth_active_amount%          - jelenleg aktív koth-ok száma
 *   %fluxkoth_active_list%            - aktív koth-ok neve vesszővel elválasztva
 *   %fluxkoth_total_amount%           - összes létrehozott koth száma
 *
 * Koth-specifikus placeholderek: %fluxkoth_<kothName>_<stat>%
 *   status          -> "Aktív" / "Inaktív"
 *   enabled         -> "true" / "false"
 *   mode            -> "TIME" / "POINTS"
 *   world           -> world neve
 *   timeleft        -> hátralévő idő mm:ss
 *   capturetime     -> a beállított capture time mm:ss
 *   duration        -> a beállított esemény hossz mm:ss
 *   camper          -> aki épp foglalja (vagy "Senki")
 *   captureseconds  -> hány másodperce foglalja folyamatosan (TIME mód)
 *   topplayer       -> jelenlegi (vagy végső) élen álló játékos neve (POINTS mód)
 *   topplayer_points-> az élen álló játékos pontszáma (POINTS mód)
 *   progress        -> 0-100 közötti progress százalék
 *   uptime          -> a koth mennyi ideje fut (mm:ss)
 */
public class FluxPlaceholderExpansion extends PlaceholderExpansion {

    private final FluxKoth plugin;

    public FluxPlaceholderExpansion(FluxKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "fluxkoth";
    }

    @Override
    public String getAuthor() {
        return "Flux";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params == null) return "";
        KothManager manager = plugin.getKothManager();

        if (params.equalsIgnoreCase("active_amount")) {
            return String.valueOf(manager.getActiveStates().size());
        }
        if (params.equalsIgnoreCase("total_amount")) {
            return String.valueOf(manager.getAllKoths().size());
        }
        if (params.equalsIgnoreCase("active_list")) {
            StringBuilder sb = new StringBuilder();
            for (KothState s : manager.getActiveStates()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(s.getKoth().getName());
            }
            return sb.length() == 0 ? "-" : sb.toString();
        }

        // Koth-specifikus: <kothname>_<stat> — a stat mindig az utolsó "_" utáni rész(ek)
        for (Koth koth : manager.getAllKoths()) {
            String name = koth.getName();
            String lower = params.toLowerCase();
            String prefix = name.toLowerCase() + "_";
            if (!lower.startsWith(prefix)) continue;

            String stat = lower.substring(prefix.length());
            KothState state = manager.getState(name);

            switch (stat) {
                case "status":
                    return state != null ? "Aktív" : "Inaktív";
                case "enabled":
                    return String.valueOf(koth.isEnabled());
                case "mode":
                    return koth.getMode().name();
                case "world":
                    return koth.getWorldName() == null ? "-" : koth.getWorldName();
                case "capturetime":
                    return TimeUtil.format(koth.getCaptureTime());
                case "duration":
                    return TimeUtil.format(koth.getDuration());
                case "timeleft":
                    if (state == null) return TimeUtil.format(0);
                    if (koth.getMode() == KothMode.TIME) {
                        int remaining = koth.getCaptureTime() - state.getContinuousCaptureSeconds();
                        return TimeUtil.format(Math.max(0, remaining));
                    }
                    return TimeUtil.format(Math.max(0, state.getSecondsLeft()));
                case "camper":
                    if (state == null || state.getCurrentCamper() == null) return "Senki";
                    OfflinePlayer camper = Bukkit.getOfflinePlayer(state.getCurrentCamper());
                    return camper.getName() == null ? "Senki" : camper.getName();
                case "captureseconds":
                    return state == null ? "0" : String.valueOf(state.getContinuousCaptureSeconds());
                case "topplayer":
                    if (state == null) return "-";
                    UUID top = state.getTopPlayer();
                    if (top == null) return "-";
                    OfflinePlayer tp = Bukkit.getOfflinePlayer(top);
                    return tp.getName() == null ? "-" : tp.getName();
                case "topplayer_points":
                    if (state == null) return "0";
                    UUID top2 = state.getTopPlayer();
                    return top2 == null ? "0" : String.valueOf(state.getPointsOf(top2));
                case "uptime":
                    return state == null ? TimeUtil.format(0) : TimeUtil.format((int) state.getUptimeSeconds());
                case "progress":
                    if (state == null) return "0";
                    if (koth.getMode() == KothMode.TIME) {
                        int remaining = koth.getCaptureTime() - state.getContinuousCaptureSeconds();
                        double p = 1.0 - Math.max(0, Math.min(1.0, (double) remaining / koth.getCaptureTime()));
                        return String.valueOf((int) (p * 100));
                    } else {
                        double p = 1.0 - Math.max(0, Math.min(1.0, (double) state.getSecondsLeft() / koth.getDuration()));
                        return String.valueOf((int) (p * 100));
                    }
                case "schedules_amount":
                    return String.valueOf(koth.getSchedules().size());
                default:
                    return null;
            }
        }

        return null;
    }
}
