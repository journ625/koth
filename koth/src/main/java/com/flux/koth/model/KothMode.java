package com.flux.koth.model;

public enum KothMode {
    /**
     * Aki folyamatosan (megszakítás nélkül) bent tartózkodik a megadott
     * időkeretig, az nyer.
     */
    TIME,
    /**
     * A koth egy megadott ideig fut, ezalatt a bent tartózkodó (egyedüli)
     * játékos másodpercenként pontot kap. A végén a legtöbb ponttal
     * rendelkező játékos nyer.
     */
    POINTS;

    public static KothMode fromString(String s) {
        if (s == null) return TIME;
        try {
            return KothMode.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TIME;
        }
    }
}
