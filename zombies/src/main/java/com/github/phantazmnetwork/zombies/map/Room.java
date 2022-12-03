package com.github.phantazmnetwork.zombies.map;

import com.github.phantazmnetwork.zombies.map.action.Action;
import com.github.steanky.vector.Bounds3I;
import com.github.steanky.vector.Vec3I;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Room {
    private final List<Action<Room>> openActions;
    private final List<Bounds3I> unmodifiableRegions;
    private final RoomInfo roomInfo;
    private boolean isOpen;

    /**
     * Constructs a new instance of this class.
     *
     * @param roomInfo the backing data object
     */
    public Room(@NotNull Vec3I mapOrigin, @NotNull RoomInfo roomInfo, @NotNull List<Action<Room>> openActions) {
        this.openActions = List.copyOf(openActions);

        List<Bounds3I> regions = roomInfo.regions();
        Vec3I origin = mapOrigin.add(
                Bounds3I.enclosingImmutable(regions.toArray(new Bounds3I[0])).immutableCenter().floorToImmutableInt());
        List<Bounds3I> list = new ArrayList<>(roomInfo.regions().size());
        for (Bounds3I region : roomInfo.regions()) {
            list.add(region.shift(origin));
        }

        this.unmodifiableRegions = Collections.unmodifiableList(list);
        this.roomInfo = Objects.requireNonNull(roomInfo, "roomInfo");
    }

    public @UnmodifiableView @NotNull List<Bounds3I> roomBounds() {
        return unmodifiableRegions;
    }

    public @NotNull RoomInfo getRoomInfo() {
        return roomInfo;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void open() {
        if (isOpen) {
            return;
        }

        isOpen = true;
        for (Action<Room> action : openActions) {
            action.perform(this);
        }
    }
}
