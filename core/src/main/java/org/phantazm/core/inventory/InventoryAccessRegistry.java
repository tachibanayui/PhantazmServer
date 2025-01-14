package org.phantazm.core.inventory;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Keeps track of and switches between {@link InventoryProfile}s.
 */
// TODO: rewrite javadocs
public interface InventoryAccessRegistry {
    /**
     * Gets the current {@link InventoryProfile} set by the view, if it exists.
     *
     * @return an Optional containing the current view, or the empty optional if there is no current view
     */
    @NotNull Optional<InventoryAccess> getCurrentAccess();

    /**
     * Switches the inventory's view to another view based on a {@link Key}.
     *
     * @param key The {@link Key} of the profile to switch to, or to switch to no profile
     * @throws IllegalArgumentException If no {@link InventoryProfile} is registered with the {@link Key}
     */
    void switchAccess(@Nullable Key key);

    /**
     * Registers a {@link InventoryProfile} to the view.
     *
     * @param key     The {@link Key} to register the {@link InventoryProfile} with
     * @param profile The {@link InventoryProfile} to register
     * @throws IllegalArgumentException If an {@link InventoryProfile} is already registered with the {@link Key}
     */
    void registerAccess(@NotNull Key key, @NotNull InventoryAccess profile);

    /**
     * Unregisters a {@link InventoryProfile} from the view.
     *
     * @param key The {@link Key} of the {@link InventoryProfile} to unregister
     * @throws IllegalArgumentException If no {@link InventoryProfile} is registered with the {@link Key}
     */
    void unregisterAccess(@NotNull Key key);

    boolean canPushTo(@NotNull Key groupKey);

    void replaceObject(int slot, @NotNull InventoryObject newObject);

    void pushObject(@NotNull Key groupKey, @NotNull InventoryObject object);
}
