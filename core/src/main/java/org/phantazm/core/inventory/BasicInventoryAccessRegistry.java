package org.phantazm.core.inventory;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.phantazm.core.equipment.Equipment;
import org.phantazm.core.player.PlayerView;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Basic implementation of an {@link InventoryAccessRegistry}.
 */
public class BasicInventoryAccessRegistry implements InventoryAccessRegistry {
    private final Map<Key, InventoryAccess> accessMap = new HashMap<>();
    private final Object sync = new Object();
    private InventoryAccess currentAccess = null;
    private final PlayerView playerView;

    public BasicInventoryAccessRegistry(@NotNull PlayerView playerView) {
        this.playerView = Objects.requireNonNull(playerView, "playerView");
    }

    @Override
    public @NotNull Optional<InventoryAccess> getCurrentAccess() {
        return Optional.ofNullable(currentAccess);
    }

    @Override
    public void switchAccess(@Nullable Key key) {
        synchronized (sync) {
            if (key == null) {
                applyTo(null);
                currentAccess = null;
            }
            else {
                InventoryAccess access = accessMap.get(key);
                if (access == null) {
                    throw new IllegalArgumentException("No matching inventory access found");
                }

                if (access == currentAccess) {
                    return;
                }

                currentAccess = access;
                applyTo(access);
            }
        }
    }

    @Override
    public void registerAccess(@NotNull Key key, @NotNull InventoryAccess profile) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(profile, "profile");

        synchronized (sync) {
            if (accessMap.containsKey(key)) {
                throw new IllegalArgumentException("Inventory profile already registered");
            }

            accessMap.put(key, profile);
        }
    }

    @Override
    public void unregisterAccess(@NotNull Key key) {
        Objects.requireNonNull(key, "key");

        synchronized (sync) {
            if (!accessMap.containsKey(key)) {
                throw new IllegalArgumentException("Inventory profile not yet registered");
            }

            accessMap.remove(key);
        }
    }

    @Override
    public boolean canPushTo(@NotNull Key groupKey) {
        Optional<InventoryAccess> accessOptional = getCurrentAccess();
        if (accessOptional.isPresent()) {
            InventoryAccess inventoryAccess = accessOptional.get();
            InventoryObjectGroup group = inventoryAccess.groups().get(groupKey);
            return group != null && !group.isFull();
        }

        return false;
    }

    @Override
    public void replaceObject(int slot, @NotNull InventoryObject newObject) {
        InventoryAccess access = getAccess();

        InventoryProfile profile = access.profile();
        if (profile.hasInventoryObject(slot)) {
            InventoryObject old = profile.removeInventoryObject(slot);
            old.end();
        }
        
        profile.setInventoryObject(slot, newObject);
        onAdd(slot, newObject);
    }

    @Override
    public void pushObject(@NotNull Key groupKey, @NotNull InventoryObject object) {
        InventoryAccess access = getAccess();
        InventoryObjectGroup group = getGroup(access, groupKey);

        int slot = group.pushInventoryObject(object);
        onAdd(slot, object);
    }

    private void onAdd(int slot, InventoryObject object) {
        object.start();
        playerView.getPlayer().ifPresent(player -> {
            if (player.getHeldSlot() == slot && object instanceof Equipment equipment) {
                equipment.setSelected(true);
            }
        });
    }

    private InventoryAccess getAccess() {
        Optional<InventoryAccess> accessOptional = getCurrentAccess();
        if (accessOptional.isEmpty()) {
            throw new IllegalArgumentException("No current access");
        }

        return accessOptional.get();
    }

    private InventoryObjectGroup getGroup(InventoryAccess access, Key groupKey) {
        InventoryObjectGroup group = access.groups().get(groupKey);
        if (group == null) {
            throw new IllegalArgumentException("Group " + groupKey + " does not exist");
        }

        return group;
    }

    private void applyTo(InventoryAccess newAccess) {
        playerView.getPlayer().ifPresent(player -> {
            player.getInventory().clear();

            InventoryProfile oldProfile = this.currentAccess.profile();
            for (int slot = 0; slot < oldProfile.getSlotCount(); slot++) {
                if (!oldProfile.hasInventoryObject(slot)) {
                    continue;
                }

                InventoryObject object = oldProfile.getInventoryObject(slot);
                if (slot == player.getHeldSlot() && object instanceof Equipment equipment) {
                    equipment.setSelected(false);
                }

                object.end();
            }

            if (newAccess != null) {
                InventoryProfile newProfile = newAccess.profile();
                for (int slot = 0; slot < newProfile.getSlotCount(); slot++) {
                    if (!newProfile.hasInventoryObject(slot)) {
                        continue;
                    }

                    InventoryObject inventoryObject = newProfile.getInventoryObject(slot);
                    inventoryObject.start();

                    //don't ask for redraw when we initially set the item
                    player.getInventory().setItemStack(slot, inventoryObject.getItemStack());

                    if (slot == player.getHeldSlot() && inventoryObject instanceof Equipment equipment) {
                        equipment.setSelected(true);
                    }
                }
            }
        });
    }

}
