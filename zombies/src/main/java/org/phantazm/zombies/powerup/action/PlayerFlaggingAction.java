package org.phantazm.zombies.powerup.action;

import com.github.steanky.element.core.annotation.*;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.phantazm.zombies.player.ZombiesPlayer;
import org.phantazm.zombies.powerup.Powerup;
import org.phantazm.zombies.powerup.predicate.DeactivationPredicate;

import java.util.Objects;
import java.util.function.Supplier;

@Model("zombies.powerup.action.player_flagging")
public class PlayerFlaggingAction implements Supplier<PowerupAction> {
    private final Data data;
    private final Supplier<DeactivationPredicate> deactivationPredicate;

    @FactoryMethod
    public PlayerFlaggingAction(@NotNull Data data,
            @NotNull @Child("deactivation_predicate") Supplier<DeactivationPredicate> deactivationPredicate) {
        this.data = Objects.requireNonNull(data, "data");
        this.deactivationPredicate = Objects.requireNonNull(deactivationPredicate, "deactivationPredicate");
    }

    @Override
    public PowerupAction get() {
        return new Action(data, deactivationPredicate.get());
    }

    @DataObject
    public record Data(@NotNull Key flag, @NotNull @ChildPath("deactivation_predicate") String deactivationPredicate) {
    }

    private static class Action extends PowerupActionBase {
        private final Data data;

        private Action(Data data, DeactivationPredicate deactivationPredicate) {
            super(deactivationPredicate);
            this.data = data;
        }

        @Override
        public void activate(@NotNull Powerup powerup, @NotNull ZombiesPlayer player, long time) {
            super.activate(powerup, player, time);
            player.flags().setFlag(data.flag);
        }

        @Override
        public void deactivate(@NotNull ZombiesPlayer player) {
            player.flags().clearFlag(data.flag);
        }
    }
}
