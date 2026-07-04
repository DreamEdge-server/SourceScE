package com.source.sourcesce;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Ships the bundled CraftEngine overlay image (config + white fullscreen PNG) into CraftEngine's
 * {@code plugins/CraftEngine/resources/sourcesce} folder on enable, version-stamped so it runs once
 * per plugin version and never overwrites the owner's edits. After a fresh deploy the owner runs
 * {@code /ce reload pack} to (re)generate the resource pack.
 */
final class PackDeployer
{
    private PackDeployer() {}

    private static final String PREFIX = "craftengine-pack/sourcesce/";

    static void deploy(JavaPlugin plugin)
    {
        Plugin ce = Bukkit.getPluginManager().getPlugin("CraftEngine");
        if (ce == null)
            return;

        Path target = ce.getDataFolder().toPath().resolve("resources").resolve("sourcesce");
        String version = plugin.getDescription().getVersion();
        Path marker = target.resolve(".sourcesce_pack_version");

        try
        {
            if (Files.isRegularFile(marker) && version.equals(new String(Files.readAllBytes(marker), StandardCharsets.UTF_8).trim()))
                return;
        }
        catch (Exception ignored) { }

        CodeSource src = PackDeployer.class.getProtectionDomain().getCodeSource();
        if (src == null)
            return;

        int extracted = 0;
        URL jar = src.getLocation();
        try (ZipInputStream zip = new ZipInputStream(jar.openStream()))
        {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null)
            {
                String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith(PREFIX))
                    continue;
                Path dest = target.resolve(name.substring(PREFIX.length()));
                Files.createDirectories(dest.getParent());
                try (InputStream in = plugin.getResource(name))
                {
                    if (in != null)
                    {
                        Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                        extracted++;
                    }
                }
            }
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Failed to deploy CraftEngine resources: " + e.getMessage());
            return;
        }

        try
        {
            Files.write(marker, version.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception ignored) { }

        if (extracted > 0)
        {
            plugin.getLogger().info("Deployed " + extracted + " CraftEngine resource file(s) to " + target);
            plugin.getLogger().warning("Run '/ce reload all' so CraftEngine registers the image AND regenerates the resource pack.");
        }
    }
}
