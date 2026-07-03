package com.flux.koth.command;

import com.flux.koth.FluxKoth;
import com.flux.koth.listener.SelectionListener;
import com.flux.koth.manager.KothManager;
import com.flux.koth.manager.MessageManager;
import com.flux.koth.model.Koth;
import com.flux.koth.model.KothMode;
import com.flux.koth.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class KothCommand implements CommandExecutor, TabCompleter {

    private final FluxKoth plugin;

    public KothCommand(FluxKoth plugin) {
        this.plugin = plugin;
    }

    private MessageManager mm() { return plugin.getMessageManager(); }
    private KothManager km() { return plugin.getKothManager(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fluxkoth.admin")) {
            mm().send(sender, "Config.NoPermission", null);
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create": return cmdCreate(sender, args);
            case "remove": return cmdRemove(sender, args);
            case "select": return cmdSelect(sender, args);
            case "inspect": return cmdInspect(sender, args);
            case "setmode": return cmdSetMode(sender, args);
            case "settime": return cmdSetTime(sender, args);
            case "setduration": return cmdSetDuration(sender, args);
            case "addreward": return cmdAddReward(sender, args);
            case "clearrewards": return cmdClearRewards(sender, args);
            case "enable": return cmdEnable(sender, args);
            case "disable": return cmdDisable(sender, args);
            case "start": return cmdStart(sender, args);
            case "stop": return cmdStop(sender, args);
            case "list": return cmdList(sender, args);
            case "schedule": return cmdSchedule(sender, args);
            case "reload": return cmdReload(sender, args);
            case "help": sendHelp(sender); return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    // ==================== Subcommands ====================

    private boolean cmdCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth create <name>"));
            return true;
        }
        String name = args[1];
        if (km().exists(name)) {
            mm().send(sender, "Config.AlreadyKoth", null);
            return true;
        }
        km().createKoth(name);
        mm().send(sender, "Config.kothCreated", MessageManager.of("%koth%", name));
        return true;
    }

    private boolean cmdRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth remove <name>"));
            return true;
        }
        String name = args[1];
        if (!km().exists(name)) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", name));
            return true;
        }
        km().removeKoth(name);
        mm().send(sender, "Config.Removed", MessageManager.of("%koth%", name));
        return true;
    }

    private boolean cmdSelect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            mm().send(sender, "Config.NotPlayer", null);
            return true;
        }
        if (args.length < 2) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth select <name>"));
            return true;
        }
        String name = args[1];
        if (!km().exists(name)) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", name));
            return true;
        }
        plugin.getSelectionListener().setEditingKoth(player.getUniqueId(), km().getKoth(name).getName());
        km().setInspectorMode(player.getUniqueId(), true);
        giveInspectorAxe(player);
        mm().send(sender, "Config.InspectorMode", null);
        return true;
    }

    private boolean cmdInspect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            mm().send(sender, "Config.NotPlayer", null);
            return true;
        }
        boolean wasOn = km().isInInspectorMode(player.getUniqueId());
        km().toggleInspectorMode(player.getUniqueId());
        if (!wasOn) {
            giveInspectorAxe(player);
            mm().send(sender, "Config.InspectorMode", null);
        } else {
            mm().send(sender, "Config.ExitInspectorMode", null);
        }
        return true;
    }

    private boolean cmdSetMode(CommandSender sender, String[] args) {
        if (args.length < 3) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth setmode <name> <TIME|POINTS>"));
            return true;
        }
        Koth koth = km().getKoth(args[1]);
        if (koth == null) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", args[1]));
            return true;
        }
        koth.setMode(KothMode.fromString(args[2]));
        km().saveAll();
        mm().send(sender, "Config.ModeSet", MessageManager.of("%koth%", koth.getName(), "%args%", koth.getMode().name()));
        return true;
    }

    private boolean cmdSetTime(CommandSender sender, String[] args) {
        if (args.length < 3) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth settime <name> <másodperc>"));
            return true;
        }
        Koth koth = km().getKoth(args[1]);
        if (koth == null) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", args[1]));
            return true;
        }
        try {
            int seconds = Integer.parseInt(args[2]);
            koth.setCaptureTime(seconds);
            km().saveAll();
            mm().send(sender, "Config.SetTime", null);
        } catch (NumberFormatException e) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth settime <name> <másodperc>"));
        }
        return true;
    }

    private boolean cmdSetDuration(CommandSender sender, String[] args) {
        if (args.length < 3) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth setduration <name> <másodperc>"));
            return true;
        }
        Koth koth = km().getKoth(args[1]);
        if (koth == null) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", args[1]));
            return true;
        }
        try {
            int seconds = Integer.parseInt(args[2]);
            koth.setDuration(seconds);
            km().saveAll();
            mm().send(sender, "Config.DurationSet", MessageManager.of("%koth%", koth.getName()));
        } catch (NumberFormatException e) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth setduration <name> <másodperc>"));
        }
        return true;
    }

    private boolean cmdAddReward(CommandSender sender, String[] args) {
        // /koth addreward item <name>   (a kézben tartott tárgyat adja hozzá)
        // /koth addreward command <name> <parancs...>
        if (args.length < 3) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth addreward <item|command> <name> [parancs]"));
            return true;
        }
        String type = args[1].toLowerCase();
        Koth koth = km().getKoth(args[2]);
        if (koth == null) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", args[2]));
            return true;
        }

        if (type.equals("item")) {
            if (!(sender instanceof Player player)) {
                mm().send(sender, "Config.NotPlayer", null);
                return true;
            }
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                sender.sendMessage(mm().getPrefix() + " §cTarts egy tárgyat a kezedben!");
                return true;
            }
            koth.getRewardItems().add(hand.clone());
            km().saveAll();
            mm().send(sender, "Config.RewardAdded", MessageManager.of("%koth%", koth.getName()));
        } else if (type.equals("command")) {
            if (args.length < 4) {
                mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth addreward command <name> <parancs>"));
                return true;
            }
            String cmdStr = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            koth.getRewardCommands().add(cmdStr);
            km().saveAll();
            mm().send(sender, "Config.RewardAdded", MessageManager.of("%koth%", koth.getName()));
        } else {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth addreward <item|command> <name> [parancs]"));
        }
        return true;
    }

    private boolean cmdClearRewards(CommandSender sender, String[] args) {
        if (args.length < 2) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth clearrewards <name>"));
            return true;
        }
        Koth koth = km().getKoth(args[1]);
        if (koth == null) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", args[1]));
            return true;
        }
        koth.getRewardItems().clear();
        koth.getRewardCommands().clear();
        km().saveAll();
        mm().send(sender, "Config.RewardsCleared", MessageManager.of("%koth%", koth.getName()));
        return true;
    }

    private boolean cmdEnable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth enable <name>"));
            return true;
        }
        Koth koth = km().getKoth(args[1]);
        if (koth == null || !koth.isReadyForEnable()) {
            mm().send(sender, "Config.NoExistsEnabled", MessageManager.of("%koth%", args[1]));
            return true;
        }
        koth.setEnabled(true);
        km().saveAll();
        mm().send(sender, "Config.Enabled", MessageManager.of("%koth%", koth.getName()));
        return true;
    }

    private boolean cmdDisable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth disable <name>"));
            return true;
        }
        Koth koth = km().getKoth(args[1]);
        if (koth == null) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", args[1]));
            return true;
        }
        koth.setEnabled(false);
        km().saveAll();
        mm().send(sender, "Config.Disabled", MessageManager.of("%koth%", koth.getName()));
        return true;
    }

    private boolean cmdStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth start <name> [másodperc]"));
            return true;
        }
        Koth koth = km().getKoth(args[1]);
        if (koth == null) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", args[1]));
            return true;
        }
        if (!koth.isEnabled()) {
            mm().send(sender, "Config.NoEnabled", MessageManager.of("%koth%", koth.getName()));
            return true;
        }
        if (km().isActive(koth.getName())) {
            mm().send(sender, "Config.AlreadyStarted", null);
            return true;
        }

        Integer overrideSeconds = null;
        if (args.length >= 3) {
            try {
                overrideSeconds = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth start <name> [másodperc]"));
                return true;
            }
        }

        plugin.startKothInternal(koth.getName(), overrideSeconds);
        mm().send(sender, "Config.Started", MessageManager.of("%koth%", koth.getName()));
        return true;
    }

    private boolean cmdStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth stop <name>"));
            return true;
        }
        Koth koth = km().getKoth(args[1]);
        if (koth == null) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", args[1]));
            return true;
        }
        if (!km().isActive(koth.getName())) {
            mm().send(sender, "Config.NoSearch", MessageManager.of("%koth%", koth.getName()));
            return true;
        }
        long uptime = km().getState(koth.getName()).getUptimeSeconds();
        km().stopKoth(koth.getName());
        mm().send(sender, "Koth.StoppedKoth", MessageManager.of(
                "%koth%", koth.getName(),
                "%uptime%", com.flux.koth.util.TimeUtil.format((int) uptime)));
        mm().send(sender, "Config.Stopped", MessageManager.of("%koth%", koth.getName()));
        return true;
    }

    private boolean cmdList(CommandSender sender, String[] args) {
        if (km().getAllKoths().isEmpty()) {
            mm().send(sender, "Config.NoActiveKoths", null);
            return true;
        }
        mm().send(sender, "Config.ListHeader", MessageManager.of("%args%", String.valueOf(km().getAllKoths().size())));
        for (Koth koth : km().getAllKoths()) {
            String status = km().isActive(koth.getName()) ? "§aFutó" : (koth.isEnabled() ? "§eEngedélyezett" : "§cLetiltott");
            String info = status + " §7| §f" + koth.getMode().name() + " §7| §f" + (koth.getWorldName() == null ? "-" : koth.getWorldName());
            mm().send(sender, "Config.ListEntry", MessageManager.of("%koth%", koth.getName(), "%args%", info));
        }
        return true;
    }

    private boolean cmdSchedule(CommandSender sender, String[] args) {
        // /koth schedule add <name> <HH:mm> [másodperc]  - a [másodperc] felülírja ehhez az induláshoz a foglalási időt
        // /koth schedule remove <name> <HH:mm>
        // /koth schedule list <name>
        if (args.length < 3) {
            mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth schedule <add|remove|list> <name> [HH:mm] [másodperc]"));
            return true;
        }
        String action = args[1].toLowerCase();
        Koth koth = km().getKoth(args[2]);
        if (koth == null) {
            mm().send(sender, "Config.NoExists", MessageManager.of("%koth%", args[2]));
            return true;
        }

        switch (action) {
            case "add": {
                if (args.length < 4 || !args[3].matches("^([01]\\d|2[0-3]):[0-5]\\d$")) {
                    mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth schedule add <name> <HH:mm> [másodperc]"));
                    return true;
                }
                String entry = args[3];
                if (args.length >= 5) {
                    try {
                        int seconds = Integer.parseInt(args[4]);
                        entry = args[3] + "|" + seconds;
                    } catch (NumberFormatException e) {
                        mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth schedule add <name> <HH:mm> [másodperc]"));
                        return true;
                    }
                }
                koth.getSchedules().add(entry);
                km().saveAll();
                mm().send(sender, "Config.scheduleCreated", null);
                return true;
            }
            case "remove": {
                if (args.length < 4) {
                    mm().send(sender, "Config.scheduleNoExists", null);
                    return true;
                }
                // Egyezés a "HH:mm" időpont alapján, függetlenül attól, hogy van-e hozzá egyedi idő megadva
                String toRemove = koth.getSchedules().stream()
                        .filter(s -> s.split("\\|", 2)[0].equals(args[3]))
                        .findFirst().orElse(null);
                if (toRemove == null) {
                    mm().send(sender, "Config.scheduleNoExists", null);
                    return true;
                }
                koth.getSchedules().remove(toRemove);
                km().saveAll();
                mm().send(sender, "Config.scheduleRemoved", null);
                return true;
            }
            case "list": {
                List<String> pretty = koth.getSchedules().stream().map(s -> {
                    String[] parts = s.split("\\|", 2);
                    return parts.length > 1 ? parts[0] + " §7(§e" + parts[1] + " mp§7)" : parts[0];
                }).collect(Collectors.toList());
                sender.sendMessage(mm().getPrefix() + " §7Ütemezések §7(§e" + koth.getName() + "§7): §f" +
                        String.join(", ", pretty));
                return true;
            }
            default:
                mm().send(sender, "Config.BadUse", MessageManager.of("%args%", "/koth schedule <add|remove|list> <name> [HH:mm] [másodperc]"));
                return true;
        }
    }

    private boolean cmdReload(CommandSender sender, String[] args) {
        plugin.reloadEverything();
        mm().send(sender, "Config.Reloaded", null);
        return true;
    }

    // ==================== Helpers ====================

    private void giveInspectorAxe(Player player) {
        String matName = plugin.getConfig().getString("settings.inspector-axe-material", "GOLDEN_AXE");
        Material mat;
        try {
            mat = Material.valueOf(matName.toUpperCase());
        } catch (Exception e) {
            mat = Material.GOLDEN_AXE;
        }
        ItemStack axe = new ItemStack(mat);
        ItemMeta meta = axe.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(plugin.getConfig().getString("settings.inspector-axe-name", "&6&lFluxKoth Foglaló Balta")));
            List<String> lore = plugin.getConfig().getStringList("settings.inspector-axe-lore").stream()
                    .map(ColorUtil::color).collect(Collectors.toList());
            meta.setLore(lore);
            axe.setItemMeta(meta);
        }
        player.getInventory().addItem(axe);
    }

    private void sendHelp(CommandSender sender) {
        String p = mm().getPrefix();
        sender.sendMessage(p + " §7--- §6FluxKoth súgó §7---");
        sender.sendMessage(" §e/koth create <name>");
        sender.sendMessage(" §e/koth remove <name>");
        sender.sendMessage(" §e/koth select <name> §7- inspector mód indítása adott koth-hoz");
        sender.sendMessage(" §e/koth inspect §7- inspector mód ki/be");
        sender.sendMessage(" §e/koth setmode <name> <TIME|POINTS>");
        sender.sendMessage(" §e/koth settime <name> <mp> §7- TIME mód: szükséges folyamatos foglalási idő");
        sender.sendMessage(" §e/koth setduration <name> <mp> §7- esemény max hossza / POINTS mód hossza");
        sender.sendMessage(" §e/koth addreward item <name> §7- kézben tartott tárgy hozzáadása");
        sender.sendMessage(" §e/koth addreward command <name> <parancs>");
        sender.sendMessage(" §e/koth clearrewards <name>");
        sender.sendMessage(" §e/koth enable|disable <name>");
        sender.sendMessage(" §e/koth start <name> [mp] §7- opcionálisan egyedi foglalási idő erre az indításra");
        sender.sendMessage(" §e/koth stop <name>");
        sender.sendMessage(" §e/koth list");
        sender.sendMessage(" §e/koth schedule add|remove|list <name> [HH:mm] [mp]");
        sender.sendMessage(" §e/koth reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subs = Arrays.asList("create", "remove", "select", "inspect", "setmode", "settime",
                "setduration", "addreward", "clearrewards", "enable", "disable", "start", "stop", "list",
                "schedule", "reload", "help");

        if (args.length == 1) {
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "remove": case "select": case "setmode": case "settime": case "setduration":
                case "clearrewards": case "enable": case "disable": case "start": case "stop":
                    return filterNames(args[1]);
                case "addreward":
                    return Arrays.asList("item", "command").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "schedule":
                    return Arrays.asList("add", "remove", "list").stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setmode")) {
                return Arrays.asList("TIME", "POINTS").stream().filter(s -> s.startsWith(args[2].toUpperCase())).collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("addreward") || args[0].equalsIgnoreCase("schedule")) {
                return filterNames(args[2]);
            }
        }

        return new ArrayList<>();
    }

    private List<String> filterNames(String typed) {
        return km().getKothNames().stream()
                .filter(n -> n.toLowerCase().startsWith(typed.toLowerCase()))
                .collect(Collectors.toList());
    }
}
