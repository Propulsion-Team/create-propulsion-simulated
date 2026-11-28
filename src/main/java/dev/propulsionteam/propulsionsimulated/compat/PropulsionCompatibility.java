package dev.propulsionteam.propulsionsimulated.compat;

import net.neoforged.fml.ModList;

public class PropulsionCompatibility {
    public static final boolean CC_ACTIVE = ModList.get() != null
        && (ModList.get().isLoaded("computercraft") || ModList.get().isLoaded("cc_tweaked"));
}
