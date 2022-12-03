package com.github.phantazmnetwork.zombies.scoreboard.sidebar.lineupdater;

import com.github.phantazmnetwork.core.time.TickFormatter;
import com.github.steanky.element.core.ElementFactory;
import com.github.steanky.element.core.annotation.DataObject;
import com.github.steanky.element.core.annotation.FactoryMethod;
import com.github.steanky.element.core.annotation.Model;
import com.github.steanky.toolkit.collection.Wrapper;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

@Model("zombies.sidebar.lineupdater.ticks")
public class TicksLineUpdater implements SidebarLineUpdater {

    private static final ElementFactory<Data, TicksLineUpdater> FACTORY = (objectData, context, dependencyProvider) -> {
        Wrapper<Long> ticksWrapper =
                dependencyProvider.provide(Key.key("zombies.dependency.sidebar" + ".ticks_since_start"));
        TickFormatter tickFormatter = context.provide(objectData.tickFormatterPath(), dependencyProvider, false);
        return new TicksLineUpdater(ticksWrapper, tickFormatter);
    };
    private final Wrapper<Long> ticksWrapper;
    private final TickFormatter tickFormatter;
    private long lastTicks = -1;

    public TicksLineUpdater(@NotNull Wrapper<Long> ticksWrapper, @NotNull TickFormatter tickFormatter) {
        this.ticksWrapper = Objects.requireNonNull(ticksWrapper, "ticksWrapper");
        this.tickFormatter = Objects.requireNonNull(tickFormatter, "tickFormatter");
    }

    @FactoryMethod
    public static @NotNull ElementFactory<Data, TicksLineUpdater> factory() {
        return FACTORY;
    }

    @Override
    public void invalidateCache() {
        lastTicks = -1;
    }

    @Override
    public @NotNull Optional<Component> tick(long time) {
        if (lastTicks == -1 || lastTicks != ticksWrapper.get()) {
            lastTicks = ticksWrapper.get();
            return Optional.of(tickFormatter.format(ticksWrapper.get()));
        }

        return Optional.empty();
    }

    @DataObject
    public record Data(@NotNull String tickFormatterPath) {

        public Data {
            Objects.requireNonNull(tickFormatterPath, "tickFormatterPath");
        }

    }
}
