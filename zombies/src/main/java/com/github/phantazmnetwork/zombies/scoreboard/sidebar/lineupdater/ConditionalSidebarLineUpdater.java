package com.github.phantazmnetwork.zombies.scoreboard.sidebar.lineupdater;

import com.github.steanky.element.core.annotation.*;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

@Model("zombies.sidebar.line_updater.conditional")
public class ConditionalSidebarLineUpdater implements SidebarLineUpdater {


    private final List<ChildUpdater> childUpdaters;

    @FactoryMethod
    public ConditionalSidebarLineUpdater(@NotNull Data data,
            @NotNull @DataName("child_updaters") List<ChildUpdater> childUpdaters) {
        this.childUpdaters = List.copyOf(childUpdaters);
    }

    @Override
    public void invalidateCache() {
        for (ChildUpdater childUpdater : childUpdaters) {
            childUpdater.getUpdater().invalidateCache();
        }
    }

    @Override
    public @NotNull Optional<Component> tick(long time) {
        for (ChildUpdater childUpdater : childUpdaters) {
            if (childUpdater.getCondition().getAsBoolean()) {
                return childUpdater.getUpdater().tick(time);
            }
        }

        return Optional.empty();
    }

    @Model("zombies.sidebar.line_updater.conditional.child")
    public static class ChildUpdater {

        private final BooleanSupplier condition;
        private final SidebarLineUpdater updater;

        @FactoryMethod
        public ChildUpdater(@NotNull ChildUpdater.Data data, @NotNull @DataName("condition") BooleanSupplier condition,
                @NotNull @DataName("updater") SidebarLineUpdater updater) {
            this.condition = Objects.requireNonNull(condition, "condition");
            this.updater = Objects.requireNonNull(updater, "updater");
        }

        public @NotNull BooleanSupplier getCondition() {
            return condition;
        }

        public @NotNull SidebarLineUpdater getUpdater() {
            return updater;
        }

        @DataObject
        public record Data(@NotNull @DataPath("condition") String conditionPath,
                           @NotNull @DataPath("updater") String updaterPath) {

            public Data {
                Objects.requireNonNull(conditionPath, "conditionPath");
                Objects.requireNonNull(updaterPath, "updaterPath");
            }

        }

    }

    @DataObject
    public record Data(@NotNull @DataPath("child_updaters") List<String> childUpdaterPaths) {

        public Data {
            Objects.requireNonNull(childUpdaterPaths, "childUpdaterPaths");
        }

    }
}
