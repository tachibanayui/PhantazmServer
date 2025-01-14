package org.phantazm.core.game.scene.fallback;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.phantazm.core.player.PlayerView;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link SceneFallback} which kicks {@link Player}s.
 */
public class KickFallback implements SceneFallback {

    private final Component kickMessage;

    /**
     * Creates a kick fallback.
     *
     * @param kickMessage The message used for kicks
     */
    public KickFallback(@NotNull Component kickMessage) {
        this.kickMessage = Objects.requireNonNull(kickMessage, "kickMessage");
    }

    @Override
    public boolean fallback(@NotNull PlayerView playerView) {
        Optional<Player> playerOptional = playerView.getPlayer();
        if (playerOptional.isPresent()) {
            playerOptional.get().kick(kickMessage);
            return true;
        }

        return false;
    }

}
