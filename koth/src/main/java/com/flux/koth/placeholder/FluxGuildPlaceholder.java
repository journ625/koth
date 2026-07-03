package com.flux.koth.placeholder;

import com.flux.koth.FluxKoth;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * Ez az expansion regisztrálja a %flux_guild_player% placeholdert.
 *
 * FONTOS: a guild-rendszer (guild plugin) nincs ehhez a pluginhoz csatolva,
 * ezért ez jelenleg csak egy "stub" (üres/placeholder visszatérés).
 * Ha van külön guild plugin, itt kell összekötni vele (pl. a guild plugin
 * saját API-jának meghívásával), hogy a %flux_guild_player% a játékos
 * guildjének nevét adja vissza.
 */
public class FluxGuildPlaceholder extends PlaceholderExpansion {

    private final FluxKoth plugin;

    public FluxGuildPlaceholder(FluxKoth plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "flux";
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
        if (params.equalsIgnoreCase("guild_player")) {
            if (player == null) return "";
            // TODO: kösd össze a szerveren használt guild plugin API-jával.
            return "";
        }
        return null;
    }
}
