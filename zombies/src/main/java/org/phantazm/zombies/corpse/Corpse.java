package org.phantazm.zombies.corpse;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.phantazm.commons.Activable;
import org.phantazm.core.entity.fakeplayer.MinimalFakePlayer;
import org.phantazm.core.hologram.Hologram;
import org.phantazm.core.time.TickFormatter;
import org.phantazm.zombies.player.state.revive.ReviveHandler;

import java.util.Objects;

public class Corpse implements Activable {

    private final Hologram hologram;

    private final MinimalFakePlayer corpseEntity;

    private final TickFormatter tickFormatter;

    public Corpse(@NotNull Hologram hologram, @NotNull MinimalFakePlayer corpseEntity,
            @NotNull TickFormatter tickFormatter) {
        this.hologram = Objects.requireNonNull(hologram, "hologram");
        this.corpseEntity = Objects.requireNonNull(corpseEntity, "corpseEntity");
        this.tickFormatter = Objects.requireNonNull(tickFormatter, "tickFormatter");
    }

    @Override
    public void start() {
        hologram.add(Component.text("███████████████", NamedTextColor.YELLOW));
        hologram.add(Component.empty());
        hologram.add(Component.empty());
        hologram.add(Component.text("███████████████", NamedTextColor.YELLOW));

        corpseEntity.init();
        corpseEntity.setPose(Entity.Pose.SLEEPING);
    }

    public void tick(@NotNull ReviveHandler reviveHandler) {
        if (reviveHandler.isReviving()) {
            hologram.set(1,
                    Component.text("REVIVING...").style(Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)));
            hologram.set(2, tickFormatter.format(reviveHandler.getTicksUntilRevive()).append(Component.text("s")));
        }
        else {
            hologram.set(1, Component.text("HOLD SNEAK TO REVIVE!")
                    .style(Style.style(NamedTextColor.YELLOW, TextDecoration.BOLD)));
            hologram.set(2, tickFormatter.format(reviveHandler.getTicksUntilDeath()));
        }
    }

    public void disable() {
        hologram.clear();
    }

    public void remove() {
        disable();
        corpseEntity.remove();
    }

    public @NotNull Activable asKnockActivable(@NotNull ReviveHandler reviveHandler) {
        Objects.requireNonNull(reviveHandler, "reviveHandler");

        return new Activable() {
            @Override
            public void start() {
                Corpse.this.start();
            }

            @Override
            public void tick(long time) {
                Corpse.this.tick(reviveHandler);
            }

            @Override
            public void end() {
                Corpse.this.remove();
            }
        };
    }

    public @NotNull Activable asDeathActivable() {
        return new Activable() {
            @Override
            public void end() {
                Corpse.this.disable();
            }
        };
    }

}
