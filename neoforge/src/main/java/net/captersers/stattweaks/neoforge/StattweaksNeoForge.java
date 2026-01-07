package net.captersers.stattweaks.neoforge;

import net.captersers.stattweaks.Stattweaks;
import net.neoforged.fml.common.Mod;

@Mod(Stattweaks.MOD_ID)
public final class StattweaksNeoForge {
    public StattweaksNeoForge() {
        // Run our common setup.
        Stattweaks.init();
    }
}
