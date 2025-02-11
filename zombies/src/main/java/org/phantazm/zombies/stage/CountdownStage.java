package org.phantazm.zombies.stage;

import com.github.steanky.toolkit.collection.Wrapper;
import it.unimi.dsi.fastutil.longs.LongList;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.instance.Instance;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.phantazm.core.time.TickFormatter;
import org.phantazm.zombies.player.ZombiesPlayer;
import org.phantazm.zombies.sidebar.SidebarUpdater;

import java.util.*;
import java.util.function.Function;

public class CountdownStage implements Stage {
    private final Instance instance;

    private final Collection<? extends ZombiesPlayer> zombiesPlayers;

    private final Map<UUID, SidebarUpdater> sidebarUpdaters = new HashMap<>();

    private final List<Component> messages;

    private final Random random;

    private final Wrapper<Long> ticksRemaining;

    private final LongList alertTicks;

    private final TickFormatter tickFormatter;

    private final Function<? super ZombiesPlayer, ? extends SidebarUpdater> sidebarUpdaterCreator;

    protected CountdownStage(@NotNull Instance instance, @NotNull Collection<? extends ZombiesPlayer> zombiesPlayers,
            @NotNull Collection<Component> messages, @NotNull Random random, @NotNull Wrapper<Long> ticksRemaining,
            @NotNull LongList alertTicks, @NotNull TickFormatter tickFormatter,
            @NotNull Function<? super ZombiesPlayer, ? extends SidebarUpdater> sidebarUpdaterCreator) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.zombiesPlayers = Objects.requireNonNull(zombiesPlayers, "zombiesPlayers");
        this.messages = List.copyOf(messages);
        this.random = Objects.requireNonNull(random, "random");
        this.ticksRemaining = Objects.requireNonNull(ticksRemaining, "ticksRemaining");
        this.alertTicks = Objects.requireNonNull(alertTicks, "alertTicks");
        this.tickFormatter = Objects.requireNonNull(tickFormatter, "tickFormatter");
        this.sidebarUpdaterCreator = Objects.requireNonNull(sidebarUpdaterCreator, "sidebarUpdaterCreator");
    }

    public CountdownStage(@NotNull Instance instance, @NotNull Collection<? extends ZombiesPlayer> zombiesPlayers,
            @NotNull Collection<Component> messages, @NotNull Random random, long countdownTicks,
            @NotNull LongList alertTicks, @NotNull TickFormatter tickFormatter,
            @NotNull Function<? super ZombiesPlayer, ? extends SidebarUpdater> sidebarUpdaterCreator) {
        this(instance, zombiesPlayers, messages, random, Wrapper.of(countdownTicks), alertTicks, tickFormatter,
                sidebarUpdaterCreator);
    }

    @Override
    public boolean shouldContinue() {
        return ticksRemaining.get() == 0L;
    }

    @Override
    public boolean shouldRevert() {
        return zombiesPlayers.isEmpty();
    }

    @Override
    public void onJoin(@NotNull ZombiesPlayer zombiesPlayer) {
        zombiesPlayer.getPlayer().ifPresent(player -> {
            if (!messages.isEmpty()) {
                player.sendMessage(messages.get(random.nextInt(messages.size())));
            }
        });
    }

    @Override
    public boolean hasPermanentPlayers() {
        return false;
    }

    @Override
    public void tick(long time) {
        if (alertTicks.contains((long)ticksRemaining.get())) {
            Component message = Component.textOfChildren(Component.text("The game starts in "),
                    tickFormatter.format(ticksRemaining.get()), Component.text("."));
            instance.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1.0F, 1.0F));
            instance.sendMessage(message);
        }
        for (ZombiesPlayer zombiesPlayer : zombiesPlayers) {
            if (!zombiesPlayer.hasQuit()) {
                SidebarUpdater sidebarUpdater = sidebarUpdaters.computeIfAbsent(zombiesPlayer.getUUID(),
                        unused -> sidebarUpdaterCreator.apply(zombiesPlayer));
                sidebarUpdater.tick(time);
            }
        }

        ticksRemaining.apply(ticks -> ticks - 1);
    }

    @Override
    public void end() {
        for (SidebarUpdater sidebarUpdater : sidebarUpdaters.values()) {
            sidebarUpdater.end();
        }
    }

    @Override
    public @NotNull Key key() {
        return StageKeys.COUNTDOWN;
    }
}
