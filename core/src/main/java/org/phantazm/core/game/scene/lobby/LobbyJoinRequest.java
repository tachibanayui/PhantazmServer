package org.phantazm.core.game.scene.lobby;

import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.phantazm.core.config.InstanceConfig;
import org.phantazm.core.game.scene.SceneJoinRequest;
import org.phantazm.core.player.PlayerView;

import java.util.Collection;

/**
 * A join request for lobbies.
 */
public interface LobbyJoinRequest extends SceneJoinRequest {
    /**
     * Gets an unmodifiable view of the players in the request.
     *
     * @return An unmodifiable view of the players in the request
     */
    @UnmodifiableView @NotNull Collection<PlayerView> getPlayers();

    /**
     * Handles {@link Instance} used for the join.
     *
     * @param instance       The {@link Instance} the players are joining
     * @param instanceConfig Configuration for the {@link Instance}
     */
    void handleJoin(@NotNull Instance instance, @NotNull InstanceConfig instanceConfig);

    @Override
    default int getRequestWeight() {
        return getPlayers().size();
    }
}
