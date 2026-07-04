package com.source.sourcesce;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hides <b>all</b> HUD while an effect shows, and restores it after:
 * <ul>
 *   <li>Vanilla HUD (hotbar/health/hunger/XP): a CHANGE_GAME_MODE spectator game-state packet via
 *       PacketEvents (client renders spectator with no HUD; server-side gamemode is untouched).</li>
 *   <li>Sidebar: swap to a blank scoreboard, restore the original on show.</li>
 *   <li>BetterHud's own HUDs: {@code HudPlayer.setHudEnabled(false)} via reflection (no compile dep).</li>
 * </ul>
 * Each mechanism no-ops if its plugin is absent.
 */
final class HudHider
{
    private HudHider() {}

    private static final Map<UUID, Scoreboard> savedBoards = new ConcurrentHashMap<>();

    static void hide(Player player)
    {
        if (cfg("hide-betterhud"))
            setBetterHudEnabled(player, false);

        if (cfg("hide-vanilla-hud"))
        {
            if (packetEvents() && player.getGameMode() != GameMode.SPECTATOR)
            {
                sendGamemode(player, 3f); // spectator
                player.setAllowFlight(player.getAllowFlight());
            }
            savedBoards.put(player.getUniqueId(), player.getScoreboard());
            if (Bukkit.getScoreboardManager() != null)
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    static void show(Player player)
    {
        // Always restore a saved scoreboard, even if the toggle was changed mid-effect.
        Scoreboard board = savedBoards.remove(player.getUniqueId());
        if (board != null)
            player.setScoreboard(board);

        if (cfg("hide-vanilla-hud") && packetEvents() && player.getGameMode() != GameMode.SPECTATOR)
        {
            sendGamemode(player, gamemodeId(player.getGameMode()));
            Bukkit.getScheduler().runTaskLater(Main.inst(), () -> player.setAllowFlight(player.getAllowFlight()), 2L);
        }
        if (cfg("hide-betterhud"))
            setBetterHudEnabled(player, true);
    }

    private static boolean cfg(String key)
    {
        return Main.inst().getConfig().getBoolean(key, true);
    }

    // --- vanilla HUD via PacketEvents ---

    private static boolean packetEvents()
    {
        return Bukkit.getPluginManager().isPluginEnabled("packetevents");
    }

    private static void sendGamemode(Player player, float value)
    {
        WrapperPlayServerChangeGameState packet =
                new WrapperPlayServerChangeGameState(WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE, value);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private static float gamemodeId(GameMode mode)
    {
        switch (mode)
        {
            case CREATIVE:  return 1f;
            case ADVENTURE: return 2f;
            case SPECTATOR: return 3f;
            default:        return 0f;
        }
    }

    // --- BetterHud HUDs via reflection (no compile-time dependency) ---

    private static void setBetterHudEnabled(Player player, boolean enabled)
    {
        if (!Bukkit.getPluginManager().isPluginEnabled("BetterHud"))
            return;
        try
        {
            Object api = Class.forName("kr.toxicity.hud.api.BetterHudAPI").getMethod("inst").invoke(null);
            Object playerManager = api.getClass().getMethod("getPlayerManager").invoke(api);
            Object hudPlayer = playerManager.getClass().getMethod("getHudPlayer", UUID.class).invoke(playerManager, player.getUniqueId());
            if (hudPlayer != null)
                hudPlayer.getClass().getMethod("setHudEnabled", boolean.class).invoke(hudPlayer, enabled);
        }
        catch (Throwable ignored) { }
    }
}
