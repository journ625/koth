package com.flux.koth.listener;

import com.flux.koth.FluxKoth;
import com.flux.koth.manager.MessageManager;
import com.flux.koth.model.Koth;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectionListener implements Listener {

    private final FluxKoth plugin;
    // Melyik játékos éppen melyik koth-ot szerkeszti inspector módban
    private final Map<UUID, String> editingKoth = new HashMap<>();

    public SelectionListener(FluxKoth plugin) {
        this.plugin = plugin;
    }

    public void setEditingKoth(UUID uuid, String kothName) {
        editingKoth.put(uuid, kothName);
    }

    public String getEditingKoth(UUID uuid) {
        return editingKoth.get(uuid);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getKothManager().addPlayerToAllBossBars(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getKothManager().isInInspectorMode(player.getUniqueId())) return;

        ItemStack item = event.getItem();
        if (item == null) return;
        if (!isInspectorAxe(item)) return;

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        event.setCancelled(true);

        String kothName = editingKoth.get(player.getUniqueId());
        if (kothName == null) {
            player.sendMessage(plugin.getMessageManager().getPrefix() +
                    " §cElőbb válassz egy koth-ot: /koth select <name>");
            return;
        }

        Koth koth = plugin.getKothManager().getKoth(kothName);
        if (koth == null) return;

        MessageManager mm = plugin.getMessageManager();
        int x = event.getClickedBlock().getX();
        int y = event.getClickedBlock().getY();
        int z = event.getClickedBlock().getZ();
        String world = player.getWorld().getName();

        if (action == Action.LEFT_CLICK_BLOCK) {
            koth.setPos1(world, x, y, z);
            mm.send(player, "Config.p1", MessageManager.of("%koth%", koth.getName()));
        } else {
            koth.setPos2(world, x, y, z);
            mm.send(player, "Config.p2", MessageManager.of("%koth%", koth.getName()));
        }
        plugin.getKothManager().saveAll();
    }

    public boolean isInspectorAxe(ItemStack item) {
        String matName = plugin.getConfig().getString("settings.inspector-axe-material", "GOLDEN_AXE");
        Material mat;
        try {
            mat = Material.valueOf(matName.toUpperCase());
        } catch (Exception e) {
            mat = Material.GOLDEN_AXE;
        }
        if (item.getType() != mat) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String expected = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("settings.inspector-axe-name", "&6&lFluxKoth Foglaló Balta"));
        return meta.getDisplayName().equals(expected);
    }
}
