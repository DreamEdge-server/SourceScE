package com.source.sourcesce;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.Map;

/**
 * SourceScE — fullscreen screen effects, faithful to the original ScreenEffects: a colour-tinted
 * fullscreen overlay + text sent as a vanilla title (native fade-in / stay / fade-out), all HUD
 * hidden while it shows, optional movement freeze. Overlay image comes from CraftEngine; HUD hiding
 * uses BetterHud + PacketEvents.
 */
public final class Main extends JavaPlugin
{
    private static Main inst;
    private EffectManager effects;
    private YamlConfiguration lang;

    public static Main inst()
    {
        return inst;
    }

    public EffectManager effects()
    {
        return effects;
    }

    @Override
    public void onEnable()
    {
        inst = this;
        saveDefaultConfig();
        saveResource("lang.yml", false);
        lang = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "lang.yml"));

        if (Bukkit.getPluginManager().getPlugin("CraftEngine") == null)
            getLogger().warning("CraftEngine not found — the overlay image needs CraftEngine.");
        if (Bukkit.getPluginManager().getPlugin("packetevents") == null)
            getLogger().warning("PacketEvents not found — the vanilla HUD (hotbar/health) won't be hidden.");

        PackDeployer.deploy(this);

        effects = new EffectManager(this);
        Bukkit.getPluginManager().registerEvents(effects, this);
        Command command = new Command(this);
        getCommand("sourcesce").setExecutor(command);
        getCommand("sourcesce").setTabCompleter(command);
    }

    @Override
    public void onDisable()
    {
        if (effects != null)
            effects.stopAll();
    }

    public String message(String path, Map<String, ?> values)
    {
        String value = lang.getString(path, path);
        for (Map.Entry<String, ?> entry : values.entrySet())
            value = value.replace("%" + entry.getKey() + "%", String.valueOf(entry.getValue()));
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
