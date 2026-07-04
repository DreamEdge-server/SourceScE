package com.source.sourcesce;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows the effect like the original ScreenEffects: the fullscreen overlay + text are sent as a
 * vanilla <b>title</b>, so fade-in / stay / fade-out are the client's native title fade. The title
 * line carries the fullscreen image plus the optional title text; the subtitle line carries the
 * subtitle text. While it shows, all HUD is hidden and (optionally) the player is frozen; both are
 * restored when the title's total duration elapses (or on stop).
 */
public final class EffectManager implements Listener
{
    private static final String NAMESPACE = "sourcesce";

    private final Main plugin;
    private final Map<UUID, BukkitTask> restoreTasks = new ConcurrentHashMap<>();
    private final Set<UUID> frozen = ConcurrentHashMap.newKeySet();

    public EffectManager(Main plugin)
    {
        this.plugin = plugin;
    }

    /**
     * @param effect       CraftEngine image id (fullscreen / fullscreen_transparent)
     * @param color        tint for the white overlay
     * @param titleText    text on the title line (over the overlay), may be empty
     * @param subtitleText text on the subtitle line, may be empty
     */
    public boolean show(Player player, String effect, TextColor color, int fadein, int stay, int fadeout,
                        boolean freeze, String titleText, String subtitleText)
    {
        if (!CraftEngineHook.isAvailable())
            return false;

        // Parse only the image tag, then tint in code: MiniMessage's <image> self-closing insert does
        // NOT inherit a surrounding colour tag, so colouring the parsed component is what tints the
        // white glyph (white glyph x colour = that colour).
        Component image = CraftEngineHook.parse("<image:" + NAMESPACE + ":" + effect + ">");
        if (image == null)
            return false;
        image = image.color(color);

        // Title line = the fullscreen image as a centred BACKGROUND with the title text overlaid.
        // The image is wrapped in symmetric negative-space shifts so it contributes ~zero net advance:
        // that keeps it centred (never pushed off-screen), while the title text then centres normally
        // over it. The subtitle text goes on the subtitle line.
        Component titleLine = image;
        if (titleText != null && !titleText.isEmpty())
        {
            int halfWidth = Math.max(0, plugin.getConfig().getInt("overlay-image-width", 256) / 2);
            Component shift = CraftEngineHook.parse("<shift:-" + halfWidth + ">");
            if (shift != null)
                titleLine = Component.empty().append(shift).append(image).append(shift).append(textComponent(titleText));
        }
        Component subtitleLine = textComponent(subtitleText);

        Title.Times times = Title.Times.times(ticks(fadein), ticks(stay), ticks(fadeout));
        player.showTitle(Title.title(titleLine, subtitleLine, times));

        HudHider.hide(player);
        UUID id = player.getUniqueId();
        if (freeze)
            frozen.add(id);
        runCommands("execute_commands_on_start", player);

        cancelRestore(id);
        long total = Math.max(1L, (long) fadein + stay + fadeout);
        restoreTasks.put(id, Bukkit.getScheduler().runTaskLater(plugin, () -> stop(player), total));
        return true;
    }

    public void stop(Player player)
    {
        UUID id = player.getUniqueId();
        boolean wasActive = cancelRestore(id);
        frozen.remove(id);
        player.clearTitle();
        HudHider.show(player);
        if (wasActive)
            runCommands("execute_commands_on_finish", player);
    }

    public void stopAll()
    {
        for (Player p : Bukkit.getOnlinePlayers())
            if (restoreTasks.containsKey(p.getUniqueId()))
                stop(p);
    }

    private boolean cancelRestore(UUID id)
    {
        BukkitTask task = restoreTasks.remove(id);
        if (task != null)
        {
            task.cancel();
            return true;
        }
        return false;
    }

    private void runCommands(String key, Player player)
    {
        if (!plugin.getConfig().getBoolean(key + ".enabled", false))
            return;
        for (String cmd : plugin.getConfig().getStringList(key + ".commands"))
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
    }

    /** Parses text through CraftEngine (colour codes / tags), falling back to plain text; empty -> empty. */
    private static Component textComponent(String raw)
    {
        if (raw == null || raw.isEmpty())
            return Component.empty();
        Component parsed = CraftEngineHook.parse(raw);
        return parsed != null ? parsed : Component.text(raw);
    }

    private static Duration ticks(int t)
    {
        return Duration.ofMillis(Math.max(0L, (long) t * 50L));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e)
    {
        if (!frozen.contains(e.getPlayer().getUniqueId()) || e.getTo() == null)
            return;
        if (e.getFrom().getX() != e.getTo().getX()
                || e.getFrom().getY() != e.getTo().getY()
                || e.getFrom().getZ() != e.getTo().getZ())
            e.setCancelled(true); // block walking; looking around still allowed
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e)
    {
        UUID id = e.getPlayer().getUniqueId();
        cancelRestore(id);
        frozen.remove(id);
    }
}
