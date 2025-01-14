package org.phantazm.zombies.player.state;

import com.github.steanky.toolkit.collection.Wrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.instance.Instance;
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.scoreboard.TabList;
import org.jetbrains.annotations.NotNull;
import org.phantazm.commons.Activable;
import org.phantazm.core.player.PlayerView;
import org.phantazm.core.time.TickFormatter;
import org.phantazm.zombies.player.ZombiesPlayer;
import org.phantazm.zombies.player.ZombiesPlayerMeta;
import org.phantazm.zombies.player.state.context.KnockedPlayerStateContext;
import org.phantazm.zombies.player.state.revive.ReviveHandler;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class BasicKnockedStateActivable implements Activable {

    private final KnockedPlayerStateContext context;

    private final Instance instance;

    private final PlayerView playerView;

    private final ReviveHandler reviveHandler;

    private final TickFormatter tickFormatter;

    private final Sidebar sidebar;

    private final TabList tabList;

    private final ZombiesPlayerMeta meta;

    public BasicKnockedStateActivable(@NotNull KnockedPlayerStateContext context, @NotNull Instance instance,
            @NotNull PlayerView playerView, @NotNull ReviveHandler reviveHandler, @NotNull TickFormatter tickFormatter,
            @NotNull ZombiesPlayerMeta meta, @NotNull Sidebar sidebar, @NotNull TabList tabList) {
        this.context = Objects.requireNonNull(context, "context");
        this.instance = Objects.requireNonNull(instance, "instance");
        this.playerView = Objects.requireNonNull(playerView, "playerView");
        this.reviveHandler = Objects.requireNonNull(reviveHandler, "reviveHandler");
        this.tickFormatter = Objects.requireNonNull(tickFormatter, "tickFormatter");
        this.meta = Objects.requireNonNull(meta, "meta");
        this.sidebar = Objects.requireNonNull(sidebar, "sidebar");
        this.tabList = tabList;
    }

    @Override
    public void start() {
        playerView.getPlayer().ifPresent(player -> {
            player.setInvisible(true);
            player.setAllowFlying(true);
            player.setFlyingSpeed(0F);
            player.setFlying(true);
            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0F);
            player.setGameMode(GameMode.SPECTATOR);
            sidebar.addViewer(player);
            tabList.addViewer(player);
            context.getVehicle().addPassenger(player);
        });
        playerView.getDisplayName().thenAccept(displayName -> {
            instance.sendMessage(buildDeathMessage(displayName));
        });
        meta.setInGame(true);
        meta.setCanRevive(false);
        meta.setCanTriggerSLA(false);
    }

    @Override
    public void tick(long time) {
        reviveHandler.getReviver().ifPresentOrElse(reviver -> {
            Wrapper<Component> knockedDisplayName = Wrapper.ofNull(), reviverDisplayName = Wrapper.ofNull();
            CompletableFuture<Void> knockedFuture = playerView.getDisplayName().thenAccept(knockedDisplayName::set);
            CompletableFuture<Void> reviverFuture =
                    reviver.module().getPlayerView().getDisplayName().thenAccept(reviverDisplayName::set);
            CompletableFuture.allOf(knockedFuture, reviverFuture).thenAccept(v -> {
                sendReviveStatus(reviver, knockedDisplayName.get(), reviverDisplayName.get());
            });
        }, this::sendDyingStatus);
    }

    @Override
    public void end() {
        playerView.getPlayer().ifPresent(player -> {
            player.setInvisible(false);
            player.setFlying(false);
            player.setAllowFlying(false);
            player.setFlyingSpeed(Attribute.FLYING_SPEED.defaultValue());
            player.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.1F);
            player.setGameMode(GameMode.ADVENTURE);
            sidebar.addViewer(player);
            tabList.addViewer(player);
            context.getVehicle().remove();
            player.teleport(Pos.fromPoint(context.getKnockLocation()));
        });
    }

    private void sendReviveStatus(@NotNull ZombiesPlayer reviver, @NotNull Component knockedDisplayName,
            @NotNull Component reviverDisplayName) {
        reviver.module().getPlayerView().getPlayer().ifPresent(reviverPlayer -> {
            reviverPlayer.sendActionBar(
                    Component.textOfChildren(Component.text("Reviving "), knockedDisplayName, Component.text(" - "),
                            tickFormatter.format(reviveHandler.getTicksUntilRevive())));
        });
        playerView.getPlayer().ifPresent(player -> {
            player.sendActionBar(Component.textOfChildren(reviverDisplayName, Component.text(" is reviving you - "),
                    tickFormatter.format(reviveHandler.getTicksUntilRevive())));
        });
    }

    private void sendDyingStatus() {
        playerView.getPlayer().ifPresent(player -> {
            player.sendActionBar(Component.textOfChildren(Component.text("You are dying - "),
                    tickFormatter.format(Math.max(reviveHandler.getTicksUntilDeath(), 0))));
        });
    }

    private @NotNull Component buildDeathMessage(@NotNull Component displayName) {
        TextComponent.Builder builder = Component.text();
        builder.append(displayName);
        builder.append(Component.text(" was knocked down"));
        context.getKnockRoom().ifPresent(room -> {
            builder.append(Component.text(" in "));
            builder.append(room);
        });
        context.getKiller().ifPresent(killer -> {
            builder.append(Component.text(" by "));
            builder.append(killer);
        });
        builder.append(Component.text("."));

        return builder.build();
    }
}
