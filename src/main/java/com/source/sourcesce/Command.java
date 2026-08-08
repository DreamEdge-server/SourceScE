package com.source.sourcesce;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Mirrors the original ScreenEffects command:
 * {@code /sourcesce <effect> <color> <fadein> <stay> <fadeout> <freeze|nofreeze> [target] [message...]}
 * and {@code /sourcesce stop [target]}. {@code <effect>} is a CraftEngine image id (e.g. fullscreen).
 */
public final class Command implements CommandExecutor, TabCompleter
{
    private final Main plugin;

    public Command(Main plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command cmd, @NotNull String label, @NotNull String[] args)
    {
        if (args.length >= 1 && args[0].equalsIgnoreCase("stop"))
        {
            List<Player> targets = resolveTargets(sender, args.length >= 2 ? args[1] : null);
            if (targets == null) return true;
            for (Player p : targets) plugin.effects().stop(p);
            return true;
        }

        if (args.length < 6)
        {
            sender.sendMessage(message("command.usage", "label", label));
            sender.sendMessage(message("command.spaces"));
            sender.sendMessage(message("command.stop-usage", "label", label));
            return true;
        }

        String effect = args[0];
        TextColor color = parseColor(args[1]);
        if (color == null)
        {
            sender.sendMessage(message("command.invalid-color"));
            return true;
        }

        int fadein, stay, fadeout;
        try
        {
            fadein = Math.max(0, Integer.parseInt(args[2]));
            stay = Math.max(0, Integer.parseInt(args[3]));
            fadeout = Math.max(0, Integer.parseInt(args[4]));
        }
        catch (NumberFormatException e)
        {
            sender.sendMessage(message("command.invalid-ticks"));
            return true;
        }

        boolean freeze = args[5].equalsIgnoreCase("freeze");

        List<Player> targets = resolveTargets(sender, args.length >= 7 ? args[6] : null);
        if (targets == null) return true;

        // Last two args = title and subtitle lines. Since a line can contain spaces, "/_" is a space.
        String title = args.length >= 8 ? deSpace(args[7]) : "";
        String subtitle = args.length >= 9 ? deSpace(args[8]) : "";

        if (!CraftEngineHook.isAvailable())
        {
            sender.sendMessage(message("command.craftengine-missing"));
            return true;
        }

        int shown = 0;
        for (Player p : targets)
        {
            if (plugin.effects().show(p, effect, color, fadein, stay, fadeout, freeze,
                    title.replace("%player%", p.getName()), subtitle.replace("%player%", p.getName())))
                shown++;
        }
        if (shown == 0)
            sender.sendMessage(message("command.no-effect", "effect", effect));
        return true;
    }

    private static TextColor parseColor(String s)
    {
        if (s.startsWith("#"))
            return TextColor.fromCSSHexString(s);
        return NamedTextColor.NAMES.value(s.toLowerCase());
    }

    /** Turns the "/_" space-placeholder into real spaces and translates &amp; colour codes. */
    private static String deSpace(String s)
    {
        return ChatColor.translateAlternateColorCodes('&', s.replace("/_", " "));
    }

    /** {@code me}/{@code all}/name (null = self). Returns null after messaging on error. */
    private List<Player> resolveTargets(CommandSender sender, String target)
    {
        if (target == null || target.equalsIgnoreCase("me"))
        {
            if (sender instanceof Player) return Collections.singletonList((Player) sender);
            sender.sendMessage(message("command.console-target"));
            return null;
        }
        if (!sender.hasPermission("sourcesce.others"))
        {
            sender.sendMessage(message("command.no-others-permission"));
            return null;
        }
        if (target.equalsIgnoreCase("all"))
            return new ArrayList<>(Bukkit.getOnlinePlayers());
        Player p = Bukkit.getPlayerExact(target);
        if (p == null)
        {
            sender.sendMessage(message("command.player-not-found", "player", target));
            return null;
        }
        return Collections.singletonList(p);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command cmd, @NotNull String alias, @NotNull String[] args)
    {
        switch (args.length)
        {
            case 1: return filter(Arrays.asList("fullscreen", "fullscreen_transparent", "stop"), args[0]);
            case 2:
                if (args[0].equalsIgnoreCase("stop")) return targetNames(args[1]);
                return filter(Arrays.asList("#770000", "#000000", "#FFFFFF", "RED", "BLUE", "GREEN", "WHITE", "BLACK"), args[1]);
            case 3: case 4: case 5: return Arrays.asList("5", "10", "20", "40", "60", "100");
            case 6: return filter(Arrays.asList("freeze", "nofreeze"), args[5]);
            case 7: return targetNames(args[6]);
            case 8: return Collections.singletonList("Title/_text");
            case 9: return Collections.singletonList("Subtitle/_text");
            default: return Collections.emptyList();
        }
    }

    private static List<String> targetNames(String prefix)
    {
        List<String> names = new ArrayList<>();
        names.add("me");
        names.add("all");
        for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
        return filter(names, prefix);
    }

    private static List<String> filter(List<String> options, String prefix)
    {
        String low = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String o : options) if (o.toLowerCase().startsWith(low)) out.add(o);
        return out;
    }

    private String message(String path, Object... values)
    {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) map.put(String.valueOf(values[i]), values[i + 1]);
        return plugin.message(path, map);
    }
}
