package com.github.phantazmnetwork.zombies.game.scoreboard.sidebar.lineupdater.condition;

import com.github.phantazmnetwork.zombies.game.player.state.PlayerStateSwitcher;
import com.github.phantazmnetwork.zombies.game.player.state.ZombiesPlayerStateKeys;
import com.github.steanky.element.core.annotation.Dependency;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BooleanSupplier;

@Model("zombies.sidebar.condition.alive")
public class AliveCondition implements BooleanSupplier {

    private final PlayerStateSwitcher stateSwitcher;

    @FactoryMethod
    public AliveCondition(
            @NotNull @Dependency("zombies.dependency.player.state_switcher") PlayerStateSwitcher stateSwitcher) {
        this.stateSwitcher = Objects.requireNonNull(stateSwitcher, "stateSwitcher");
    }

    @Override
    public boolean getAsBoolean() {
        return stateSwitcher.getState().key().equals(ZombiesPlayerStateKeys.ALIVE.key());
    }
}
