package org.phantazm.zombies.equipment.gun.effect;

import com.github.steanky.element.core.annotation.Cache;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.phantazm.core.player.PlayerView;
import org.phantazm.zombies.equipment.gun.GunState;

import java.util.Objects;

/**
 * A {@link GunEffect} that sets the level of a {@link Player} based on the ammo of a gun.
 */
@Model("zombies.gun.effect.ammo_level")
@Cache(false)
public class AmmoLevelEffect implements GunEffect {
    private final PlayerView playerView;
    private boolean currentlyActive = false;

    /**
     * Creates a new {@link AmmoLevelEffect}.
     *
     * @param playerView The {@link PlayerView} of the {@link Player} to set the level of
     */
    @FactoryMethod
    public AmmoLevelEffect(@NotNull PlayerView playerView) {
        this.playerView = Objects.requireNonNull(playerView, "playerView");
    }

    @Override
    public void apply(@NotNull GunState state) {
        if (state.isMainEquipment()) {
            currentlyActive = true;
            playerView.getPlayer().ifPresent(player -> player.setLevel(state.ammo()));
        }
        else if (currentlyActive) {
            playerView.getPlayer().ifPresent(player -> player.setLevel(0));
            currentlyActive = false;
        }
    }

    @Override
    public void tick(@NotNull GunState state, long time) {

    }

}
