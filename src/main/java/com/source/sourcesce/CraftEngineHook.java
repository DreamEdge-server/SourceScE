package com.source.sourcesce;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Turns a string like {@code "<#770000><image:sourcesce:fullscreen>"} into a Paper {@link Component}
 * using CraftEngine's text engine (which resolves the {@code <image:...>} tag to the fullscreen
 * font glyph). CraftEngine shades Adventure under a relocated package, so we bridge via JSON and do
 * it all by reflection — no compile-time CraftEngine dependency, no version coupling.
 */
public final class CraftEngineHook
{
    private CraftEngineHook() {}

    private static final String HELPER = "net.momirealms.craftengine.core.util.AdventureHelper";
    private static final String CE_COMPONENT = "net.momirealms.craftengine.libraries.adventure.text.Component";

    static boolean isAvailable()
    {
        return Bukkit.getPluginManager().isPluginEnabled("CraftEngine");
    }

    @Nullable
    static Component parse(String raw)
    {
        if (raw == null || !isAvailable())
            return null;
        try
        {
            Class<?> helper = Class.forName(HELPER);
            String mm = (String) helper.getMethod("legacyToMiniMessage", String.class).invoke(null, raw);
            Object customMM = helper.getMethod("customMiniMessage").invoke(null);
            // customMiniMessage() returns a non-public MiniMessageImpl. Its concrete deserialize(String)
            // is public but declared on a non-public class, so direct invoke fails with
            // IllegalAccessException — setAccessible(true) bypasses that check. (The MiniMessage
            // interface only exposes the generic-erased deserialize(Object), so we can't use it.)
            Method deserialize = null;
            for (Method m : customMM.getClass().getMethods())
                if (m.getName().equals("deserialize") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class)
                {
                    deserialize = m;
                    break;
                }
            if (deserialize == null)
                return null;
            deserialize.setAccessible(true);
            Object ceComponent = deserialize.invoke(customMM, mm);
            String json = (String) helper.getMethod("componentToJson", Class.forName(CE_COMPONENT)).invoke(null, ceComponent);
            return GsonComponentSerializer.gson().deserialize(json);
        }
        catch (Throwable t)
        {
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            Bukkit.getLogger().warning("[SourceScE] CraftEngine parse failed for \"" + raw + "\": "
                    + cause.getClass().getName() + ": " + cause.getMessage());
            return null;
        }
    }
}
