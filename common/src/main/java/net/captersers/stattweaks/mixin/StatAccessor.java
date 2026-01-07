package net.captersers.stattweaks.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponentMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;


/**
 * Mixin Accessor for the {@link Item} class.
 * <p>
 * This interface utilizes the Mixin framework to bypass visibility restrictions (private/protected)
 * of the base Item class, allowing direct manipulation of the internal Data Component system.
 * </p>
 */
@Mixin(Item.class)
public interface StatAccessor {



    /**
     * Inject a new {@link DataComponentMap} into the Item instance.
     * <p>
     * The {@link Mutable} annotation is critical here: it instructs the Mixin processor
     * to remove the {@code final} modifier from the target 'components' field,
     * allowing us to overwrite the vanilla stats at runtime.
     * </p>
     *
     * @param components The new component map containing modified stats (damage, durability, etc.).
     */
    @Accessor("components")
    @Mutable
    void setComponents(DataComponentMap components);



    /**
     * Retrieves the current {@link DataComponentMap} associated with this item.
     * <p>
     * Used to fetch the base statistics of an item (immutable) before creating
     * a new builder to modify them.
     * </p>
     *
     * @return The current immutable map of data components.
     */
    @Accessor("components")
    DataComponentMap getComponents();
}

