package org.phantazm.zombies.map.handler;

import org.jetbrains.annotations.NotNull;
import org.phantazm.commons.Tickable;
import org.phantazm.core.tracker.BoundedTracker;
import org.phantazm.zombies.map.Window;
import org.phantazm.zombies.player.ZombiesPlayer;

import java.util.Collection;

public interface WindowHandler extends Tickable {
    void handleCrouchStateChange(@NotNull ZombiesPlayer zombiesPlayer, boolean crouching);

    @NotNull BoundedTracker<Window> tracker();

    interface Source {
        @NotNull WindowHandler make(@NotNull BoundedTracker<Window> windowTracker,
                @NotNull Collection<? extends ZombiesPlayer> players);
    }
}
