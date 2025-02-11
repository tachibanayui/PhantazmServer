package org.phantazm.core.game.scene.lobby;

import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.phantazm.core.config.InstanceConfig;
import org.phantazm.core.game.scene.RouteResult;
import org.phantazm.core.game.scene.fallback.SceneFallback;
import org.phantazm.core.player.PlayerView;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@EnvTest
public class LobbyIntegrationTest {

    private static final UUID playerUUID = UUID.fromString("ade229bf-d062-46e8-99d8-97b667d5a127");

    @Test
    public void testShutdown(Env env) {
        Instance instance = env.createFlatInstance();
        InstanceConfig instanceConfig = new InstanceConfig(InstanceConfig.DEFAULT_POS, InstanceConfig.DEFAULT_TIME,
                InstanceConfig.DEFAULT_TIME_RATE);
        SceneFallback sceneFallback = (ignored) -> true;
        Lobby lobby = new Lobby(instance, instanceConfig, sceneFallback);
        PlayerView playerView = mock(PlayerView.class);

        lobby.shutdown();
        assertTrue(lobby.isShutdown());

        RouteResult result =
                lobby.join(new BasicLobbyJoinRequest(env.process().connection(), Collections.singleton(playerView)));
        assertFalse(result.success());
    }

    @Test
    public void testJoin(Env env) {
        Instance instance = mock(Instance.class);
        InstanceConfig instanceConfig = new InstanceConfig(InstanceConfig.DEFAULT_POS, InstanceConfig.DEFAULT_TIME,
                InstanceConfig.DEFAULT_TIME_RATE);
        SceneFallback sceneFallback = (ignored) -> true;
        Lobby lobby = new Lobby(instance, instanceConfig, sceneFallback);
        Player player = mock(Player.class);
        when(player.setInstance(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        PlayerView playerView = new PlayerView() {
            @Override
            public @NotNull UUID getUUID() {
                return playerUUID;
            }

            @Override
            public @NotNull CompletableFuture<String> getUsername() {
                return CompletableFuture.completedFuture(playerUUID.toString());
            }

            @Override
            public @NotNull Optional<String> getUsernameIfCached() {
                return Optional.of(playerUUID.toString());
            }

            @Override
            public @NotNull CompletableFuture<Component> getDisplayName() {
                return CompletableFuture.completedFuture(Component.empty());
            }

            @Override
            public @NotNull Optional<? extends Component> getDisplayNameIfCached() {
                return Optional.of(Component.empty());
            }

            @Override
            public @NotNull Optional<Player> getPlayer() {
                return Optional.of(player);
            }

        };

        RouteResult result =
                lobby.join(new BasicLobbyJoinRequest(env.process().connection(), Collections.singleton(playerView)));

        assertTrue(result.success());
        verify(player).setInstance(eq(instance), eq(instanceConfig.spawnPoint()));
    }

}
