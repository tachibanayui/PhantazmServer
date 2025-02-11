package org.phantazm.zombies.equipment.gun.target.intersectionfinder;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Finds the intersection of a gun's shot and an {@link Entity}.
 */
@FunctionalInterface
public interface IntersectionFinder {

    /**
     * Finds the intersection of a gun's shot and an {@link Entity}.
     *
     * @param entity               The {@link Entity} to find the intersection of the shot with
     * @param start                The start of the shot
     * @param end                  The endpoint of the shot
     * @param distanceLimitSquared The squared distance between {@code start} and {@code end}
     * @return The intersection of the shot and the {@link Entity}, or {@link Optional#empty()} if there is no intersection
     */
    @NotNull Optional<Vec> getHitLocation(@NotNull Entity entity, @NotNull Pos start, @NotNull Point end,
            double distanceLimitSquared);

}
