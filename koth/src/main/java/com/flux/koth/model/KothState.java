package com.flux.koth.model;

import org.bukkit.boss.BossBar;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KothState {

    private final Koth koth;
    private boolean running = true;

    // Az a játékos aki jelenleg EGYEDÜL foglalja a területet (null ha senki/több van bent)
    private UUID currentCamper;

    // TIME módban: hány másodperce foglalja már folyamatosan a jelenlegi camper
    private int continuousCaptureSeconds = 0;

    // POINTS módban: játékos -> pontok
    private final Map<UUID, Integer> points = new HashMap<>();

    // Az esemény hátralévő ideje másodpercben (timeout / duration számláló)
    private int secondsLeft;

    // Az adott indításra vonatkozó (parancsból/schedule-ból felülírható) szükséges idők.
    // TIME módban ez a folyamatos foglaláshoz szükséges idő, POINTS módban az esemény hossza.
    // Ha nem lett felülírva induláskor, a koth alapértelmezett capture/duration értéke kerül ide.
    private final int requiredCaptureTime;
    private final int requiredDuration;

    // Hány másodperce nem küldtünk "Camping" (folyamatban lévő foglalás) üzenetet a chatbe.
    // Ez a beállított intervallumonként (alapból 25 mp) nullázódik, amikor tényleg üzenetet küldünk.
    private int campingMessageCounter = 0;

    private BossBar bossBar;
    private long startedAtMillis;

    public KothState(Koth koth) {
        this(koth, null);
    }

    /**
     * @param overrideSeconds ha nem null, ez felülírja a koth alapértelmezett capture/duration
     *                        idejét erre az egy indításra (parancsból vagy schedule-ból megadva).
     */
    public KothState(Koth koth, Integer overrideSeconds) {
        this.koth = koth;
        this.requiredCaptureTime = overrideSeconds != null ? overrideSeconds : koth.getCaptureTime();
        this.requiredDuration = overrideSeconds != null ? overrideSeconds : koth.getDuration();
        this.secondsLeft = koth.getMode() == KothMode.TIME ? requiredCaptureTime : requiredDuration;
        this.startedAtMillis = System.currentTimeMillis();
    }

    public Koth getKoth() { return koth; }

    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }

    public UUID getCurrentCamper() { return currentCamper; }
    public void setCurrentCamper(UUID currentCamper) { this.currentCamper = currentCamper; }

    public int getContinuousCaptureSeconds() { return continuousCaptureSeconds; }
    public void setContinuousCaptureSeconds(int continuousCaptureSeconds) { this.continuousCaptureSeconds = continuousCaptureSeconds; }

    public Map<UUID, Integer> getPoints() { return points; }

    public void addPoint(UUID uuid) {
        points.merge(uuid, 1, Integer::sum);
    }

    public UUID getTopPlayer() {
        UUID top = null;
        int max = -1;
        for (Map.Entry<UUID, Integer> e : points.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                top = e.getKey();
            }
        }
        return top;
    }

    public int getPointsOf(UUID uuid) {
        return points.getOrDefault(uuid, 0);
    }

    public int getSecondsLeft() { return secondsLeft; }
    public void setSecondsLeft(int secondsLeft) { this.secondsLeft = secondsLeft; }

    public int getRequiredCaptureTime() { return requiredCaptureTime; }
    public int getRequiredDuration() { return requiredDuration; }

    public int getCampingMessageCounter() { return campingMessageCounter; }
    public void setCampingMessageCounter(int campingMessageCounter) { this.campingMessageCounter = campingMessageCounter; }
    public void incrementCampingMessageCounter() { this.campingMessageCounter++; }

    public BossBar getBossBar() { return bossBar; }
    public void setBossBar(BossBar bossBar) { this.bossBar = bossBar; }

    public long getUptimeSeconds() {
        return (System.currentTimeMillis() - startedAtMillis) / 1000L;
    }
}
