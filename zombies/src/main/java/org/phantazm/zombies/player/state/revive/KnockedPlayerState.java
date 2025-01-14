package org.phantazm.zombies.player.state.revive;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.phantazm.commons.Activable;
import org.phantazm.zombies.player.state.ZombiesPlayerState;
import org.phantazm.zombies.player.state.ZombiesPlayerStateKeys;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public class KnockedPlayerState implements ZombiesPlayerState {
    private static final Component DISPLAY_NAME = Component.text("REVIVE").color(NamedTextColor.YELLOW);

    private final ReviveHandler reviveHandler;

    private final Collection<Activable> activables;

    public KnockedPlayerState(@NotNull ReviveHandler reviveHandler, @NotNull Collection<Activable> activables) {
        this.reviveHandler = Objects.requireNonNull(reviveHandler, "reviveHandler");
        this.activables = Objects.requireNonNull(activables, "activables");
    }

    public @NotNull ReviveHandler getReviveHandler() {
        return reviveHandler;
    }

    @Override
    public void start() {
        reviveHandler.start();
        for (Activable activable : activables) {
            activable.start();
        }
    }

    @Override
    public @NotNull Optional<ZombiesPlayerState> tick(long time) {
        reviveHandler.tick(time);
        for (Activable activable : activables) {
            activable.tick(time);
        }
        return reviveHandler.getSuggestedState();
    }

    @Override
    public void end() {
        reviveHandler.setReviver(null);
        reviveHandler.end();
        for (Activable activable : activables) {
            activable.end();
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public @NotNull Key key() {
        return ZombiesPlayerStateKeys.KNOCKED.key();
    }

}
