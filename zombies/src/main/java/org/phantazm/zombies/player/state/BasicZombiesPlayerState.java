package org.phantazm.zombies.player.state;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.phantazm.commons.Activable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BasicZombiesPlayerState implements ZombiesPlayerState {
    private final Component displayName;
    private final Key key;
    private final Collection<Activable> actions;

    public BasicZombiesPlayerState(@NotNull Component displayName, @NotNull Key key,
            @NotNull Collection<Activable> actions) {
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.key = Objects.requireNonNull(key, "key");
        this.actions = List.copyOf(actions);
    }

    @Override
    public void start() {
        for (Activable action : actions) {
            action.start();
        }
    }

    @Override
    public @NotNull Optional<ZombiesPlayerState> tick(long time) {
        for (Activable action : actions) {
            action.tick(time);
        }
        return Optional.empty();
    }

    @Override
    public void end() {
        for (Activable action : actions) {
            action.end();
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return displayName;
    }

    @Override
    public @NotNull Key key() {
        return key;
    }

}
