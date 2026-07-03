package com.flux.koth.util;

public class TimeUtil {

    /**
     * Másodperceket formáz "m:ss" alakra, pl. 420 -> "7:00"
     */
    public static String format(int totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }

    /**
     * Másodperceket formáz "HH:mm:ss" alakra ha kell hosszabb időhöz.
     */
    public static String formatLong(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append(":");
        sb.append(minutes < 10 && hours > 0 ? "0" + minutes : minutes).append(":");
        sb.append(seconds < 10 ? "0" + seconds : String.valueOf(seconds));
        return sb.toString();
    }
}
