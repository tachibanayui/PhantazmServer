package org.phantazm.zombies.equipment.gun.shoot.fire;

import com.github.steanky.element.core.annotation.*;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;
import org.phantazm.zombies.equipment.gun.Gun;
import org.phantazm.zombies.equipment.gun.GunState;
import org.phantazm.zombies.equipment.gun.shoot.handler.ShotHandler;

import java.util.*;

/**
 * A {@link Firer} which delegates to multiple sub-{@link Firer}.
 * Sub-{@link Firer}s may shoot at a slightly different angle than the direction of the original shot.
 */
@Model("zombies.gun.firer.spread")
@Cache(false)
public class SpreadFirer implements Firer {

    private final Data data;
    private final Random random;
    private final Collection<Firer> subFirers;

    /**
     * Creates a {@link SpreadFirer}.
     *
     * @param data      The {@link SpreadFirer}'s {@link Data}
     * @param random    The {@link Random} to use for angle variance
     * @param subFirers A {@link Collection} of sub-{@link Firer}s
     */
    @FactoryMethod
    public SpreadFirer(@NotNull Data data, @NotNull Random random,
            @NotNull @Child("sub_firers") Collection<Firer> subFirers) {
        this.data = Objects.requireNonNull(data, "data");
        this.random = Objects.requireNonNull(random, "random");
        this.subFirers = List.copyOf(subFirers);
    }

    @Override
    public void fire(@NotNull Gun gun, @NotNull GunState state, @NotNull Pos start,
            @NotNull Collection<UUID> previousHits) {
        if (data.angleVariance() == 0) {
            for (Firer subFirer : subFirers) {
                subFirer.fire(gun, state, start, previousHits);
            }
            return;
        }

        Vec direction = start.direction();
        double yaw = Math.atan2(direction.z(), direction.x());
        double noYMagnitude = Math.sqrt(direction.x() * direction.x() + direction.z() * direction.z());
        double pitch = Math.atan2(direction.y(), noYMagnitude);

        for (Firer subFirer : subFirers) {
            double newYaw = yaw + data.angleVariance() * (2 * random.nextDouble() - 1);
            double newPitch = pitch + data.angleVariance() * (2 * random.nextDouble() - 1);

            Vec newDirection = new Vec(Math.cos(newYaw) * Math.cos(newPitch), Math.sin(newPitch),
                    Math.sin(newYaw) * Math.cos(newPitch));
            subFirer.fire(gun, state, start.withDirection(newDirection), previousHits);
        }
    }

    @Override
    public void addExtraShotHandler(@NotNull ShotHandler shotHandler) {
        Objects.requireNonNull(shotHandler, "shotHandler");
        for (Firer firer : subFirers) {
            firer.addExtraShotHandler(shotHandler);
        }
    }

    @Override
    public void removeExtraShotHandler(@NotNull ShotHandler shotHandler) {
        Objects.requireNonNull(shotHandler, "shotHandler");
        for (Firer firer : subFirers) {
            firer.removeExtraShotHandler(shotHandler);
        }
    }

    @Override
    public void tick(@NotNull GunState state, long time) {
        for (Firer firer : subFirers) {
            firer.tick(state, time);
        }
    }

    /**
     * Data for a {@link SpreadFirer}.
     *
     * @param subFirers     A {@link Collection} of paths to the {@link SpreadFirer}'s sub-{@link Firer}s
     * @param angleVariance The maximum angle variance for each sub-{@link Firer}
     */
    @DataObject
    public record Data(@NotNull @ChildPath("sub_firers") Collection<String> subFirers, float angleVariance) {

    }

}
