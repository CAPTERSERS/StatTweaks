package net.captersers.stattweaks;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public class STAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Stattweaks.MOD_ID, Registries.ATTRIBUTE);

    public static final RegistrySupplier<Attribute> PLAYER_REACH = ATTRIBUTES.register("player_reach",
            () -> new RangedAttribute("attribute.name.generic.player_reach", 0.0, -1024.0, 1024.0)
                    .setSyncable(true));

    public static void init() {
        ATTRIBUTES.register();
    }
}
