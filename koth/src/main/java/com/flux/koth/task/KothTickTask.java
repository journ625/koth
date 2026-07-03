package com.flux.koth.task;

import com.flux.koth.FluxKoth;
import com.flux.koth.manager.KothManager;
import com.flux.koth.manager.MessageManager;
import com.flux.koth.model.Cuboid;
import com.flux.koth.model.Koth;
import com.flux.koth.model.KothMode;
import com.flux.koth.model.KothState;
import com.flux.koth.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KothTickTask extends BukkitRunnable {

    private final FluxKoth plugin;

    public KothTickTask(FluxKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        KothManager manager = plugin.getKothManager();
        List<String> toStop = new ArrayList<>();

        for (KothState state : manager.getActiveStates()) {
            Koth koth = state.getKoth();
            Cuboid cuboid = koth.getCuboid();
            if (cuboid == null) {
                toStop.add(koth.getName());
                continue;
            }

            List<Player> inside = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (cuboid.contains(p.getLocation()) && !p.getGameMode().name().equals("SPECTATOR")) {
                    inside.add(p);
                }
            }

            if (koth.getMode() == KothMode.TIME) {
                handleTimeMode(state, koth, inside);
            } else {
                handlePointsMode(state, koth, inside);
            }

            if (!state.isRunning()) {
                updateBossBar(state, koth);
                toStop.add(koth.getName());
                continue;
            }

            updateBossBar(state, koth);

            // Globális timeout ellenőrzés (biztonsági / esemény vége)
            state.setSecondsLeft(state.getSecondsLeft() - 1);
            if (koth.getMode() == KothMode.POINTS && state.getSecondsLeft() <= 0) {
                finishPointsKoth(state, koth);
                toStop.add(koth.getName());
            } else if (koth.getMode() == KothMode.TIME && state.getUptimeSeconds() >= (state.getRequiredDuration() * 2L) && state.getRequiredDuration() > 0) {
                // Vészfék: ha TIME módban túl sokáig senki nem tudja elfoglalni, ne fusson vég nélkül
                MessageManager mm = plugin.getMessageManager();
                mm.broadcast("Koth.timeUp", MessageManager.of(
                        "%koth%", koth.getName(),
                        "%uptime%", TimeUtil.format((int) state.getUptimeSeconds())));
                toStop.add(koth.getName());
            }
        }

        for (String name : toStop) {
            manager.stopKoth(name);
        }
    }

    private void handleTimeMode(KothState state, Koth koth, List<Player> inside) {
        MessageManager mm = plugin.getMessageManager();
        int msgInterval = plugin.getConfig().getInt("settings.camping-message-interval-seconds", 25);

        UUID camperUUID = state.getCurrentCamper();
        if (camperUUID != null) {
            Player camper = findPlayer(inside, camperUUID);
            if (camper != null) {
                // A foglaló játékos még mindig bent van -> a foglalás folytatódik,
                // FÜGGETLENÜL attól, hogy közben mások beléptek-e a területre.
                int captured = state.getContinuousCaptureSeconds() + 1;
                state.setContinuousCaptureSeconds(captured);
                int remaining = state.getRequiredCaptureTime() - captured;

                if (remaining <= 0) {
                    // Nyertes!
                    mm.broadcast("Koth.Camped", MessageManager.of(
                            "%player%", camper.getName(),
                            "%koth%", koth.getName(),
                            "%uptime%", TimeUtil.format((int) state.getUptimeSeconds())));
                    giveRewards(koth, camper);
                    state.setRunning(false);
                } else {
                    state.incrementCampingMessageCounter();
                    if (state.getCampingMessageCounter() >= msgInterval) {
                        state.setCampingMessageCounter(0);
                        mm.broadcast("Koth.Camping", MessageManager.of(
                                "%player%", camper.getName(),
                                "%koth%", koth.getName(),
                                "%time%", TimeUtil.format(remaining)));
                    }
                }
                return;
            } else {
                // A foglaló játékos elhagyta a területet -> a foglalás megszakad
                mm.broadcast("Koth.LostControl", MessageManager.of("%koth%", koth.getName()));
                state.setCurrentCamper(null);
                state.setContinuousCaptureSeconds(0);
                state.setCampingMessageCounter(0);
            }
        }

        // Jelenleg senki nem foglal -> csak akkor indul új foglalás, ha pontosan egy
        // játékos van bent (ha 0 vagy 2+ játékos van bent, várakozunk)
        if (inside.size() == 1) {
            Player newCamper = inside.get(0);
            state.setCurrentCamper(newCamper.getUniqueId());
            state.setContinuousCaptureSeconds(0);
            state.setCampingMessageCounter(0);
            mm.broadcast("Koth.StartedCamping", MessageManager.of(
                    "%player%", newCamper.getName(),
                    "%koth%", koth.getName(),
                    "%time%", TimeUtil.format(state.getRequiredCaptureTime())));
        }
    }

    private void handlePointsMode(KothState state, Koth koth, List<Player> inside) {
        MessageManager mm = plugin.getMessageManager();
        int msgInterval = plugin.getConfig().getInt("settings.camping-message-interval-seconds", 25);

        UUID camperUUID = state.getCurrentCamper();
        if (camperUUID != null) {
            Player camper = findPlayer(inside, camperUUID);
            if (camper != null) {
                // A foglaló játékos még mindig bent van -> kap pontot, FÜGGETLENÜL attól,
                // hogy közben mások beléptek-e a területre.
                state.addPoint(camperUUID);
                state.incrementCampingMessageCounter();
                if (state.getCampingMessageCounter() >= msgInterval) {
                    state.setCampingMessageCounter(0);
                    mm.broadcast("Koth.Camping", MessageManager.of(
                            "%player%", camper.getName(),
                            "%koth%", koth.getName(),
                            "%time%", TimeUtil.format(state.getSecondsLeft())));
                }
                return;
            } else {
                // A foglaló játékos elhagyta a területet -> a foglalás megszakad
                mm.broadcast("Koth.LostControl", MessageManager.of("%koth%", koth.getName()));
                state.setCurrentCamper(null);
                state.setCampingMessageCounter(0);
            }
        }

        // Jelenleg senki nem foglal -> csak akkor indul új foglalás, ha pontosan egy
        // játékos van bent (ha 0 vagy 2+ játékos van bent, várakozunk)
        if (inside.size() == 1) {
            Player newCamper = inside.get(0);
            state.setCurrentCamper(newCamper.getUniqueId());
            state.setCampingMessageCounter(0);
            state.addPoint(newCamper.getUniqueId());
            mm.broadcast("Koth.StartedCamping", MessageManager.of(
                    "%player%", newCamper.getName(),
                    "%koth%", koth.getName(),
                    "%time%", TimeUtil.format(state.getSecondsLeft())));
        }
    }

    private Player findPlayer(List<Player> list, UUID uuid) {
        for (Player p : list) {
            if (p.getUniqueId().equals(uuid)) return p;
        }
        return null;
    }

    private void finishPointsKoth(KothState state, Koth koth) {
        MessageManager mm = plugin.getMessageManager();
        UUID topUuid = state.getTopPlayer();
        if (topUuid != null) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(topUuid);
            String name = op.getName() != null ? op.getName() : "?";
            mm.broadcast("Koth.Camped", MessageManager.of(
                    "%player%", name,
                    "%koth%", koth.getName(),
                    "%uptime%", TimeUtil.format((int) state.getUptimeSeconds())));
            if (op.isOnline() && op.getPlayer() != null) {
                giveRewards(koth, op.getPlayer());
            }
        } else {
            mm.broadcast("Koth.timeUp", MessageManager.of(
                    "%koth%", koth.getName(),
                    "%uptime%", TimeUtil.format((int) state.getUptimeSeconds())));
        }
    }

    private void giveRewards(Koth koth, Player winner) {
        for (ItemStack item : koth.getRewardItems()) {
            if (item != null) {
                winner.getInventory().addItem(item.clone());
            }
        }
        for (String cmd : koth.getRewardCommands()) {
            String parsed = cmd.replace("%player%", winner.getName())
                    .replace("%koth%", koth.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private void updateBossBar(KothState state, Koth koth) {
        BossBar bar = state.getBossBar();
        if (bar == null) return;

        String title;
        double progress;

        if (koth.getMode() == KothMode.TIME) {
            int remaining = state.getRequiredCaptureTime() - state.getContinuousCaptureSeconds();
            if (state.getCurrentCamper() == null) remaining = state.getRequiredCaptureTime();
            progress = 1.0 - Math.max(0, Math.min(1.0, (double) remaining / state.getRequiredCaptureTime()));

            String camperName = state.getCurrentCamper() != null
                    ? Bukkit.getOfflinePlayer(state.getCurrentCamper()).getName()
                    : "Senki";

            title = String.format("§6%s §7| §fIdő: §e%s §7| §fFoglalja: §a%s",
                    koth.getName(), TimeUtil.format(Math.max(0, remaining)), camperName);
        } else {
            progress = 1.0 - Math.max(0, Math.min(1.0, (double) state.getSecondsLeft() / state.getRequiredDuration()));

            UUID topUuid = state.getTopPlayer();
            String topName = "Senki";
            int topPoints = 0;
            if (topUuid != null) {
                topName = Bukkit.getOfflinePlayer(topUuid).getName();
                topPoints = state.getPointsOf(topUuid);
            }
            String camperName = state.getCurrentCamper() != null
                    ? Bukkit.getOfflinePlayer(state.getCurrentCamper()).getName()
                    : "Senki";

            title = String.format("§6%s §7| §fIdő: §e%s §7| §fVezet: §a%s §7(§e%d pont§7) §7| §fFoglalja: §a%s",
                    koth.getName(), TimeUtil.format(Math.max(0, state.getSecondsLeft())), topName, topPoints, camperName);
        }

        bar.setTitle(title);
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }
}
