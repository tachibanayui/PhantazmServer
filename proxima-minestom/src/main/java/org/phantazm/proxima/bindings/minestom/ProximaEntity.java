package org.phantazm.proxima.bindings.minestom;

import com.github.steanky.proxima.Navigator;
import com.github.steanky.proxima.node.Node;
import com.github.steanky.proxima.path.PathResult;
import com.github.steanky.proxima.path.PathTarget;
import com.github.steanky.proxima.resolver.PositionResolver;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.phantazm.core.VecUtils;
import org.phantazm.proxima.bindings.minestom.controller.Controller;
import org.phantazm.proxima.bindings.minestom.goal.GoalGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * An entity with navigation capabilities based on the Proxima library.
 */
public class ProximaEntity extends LivingEntity {
    protected final Pathfinding pathfinding;
    protected final List<GoalGroup> goalGroups;

    private PathTarget destination;
    private PathResult currentPath;

    private Node current;
    private Node target;

    private long recalculationDelay;
    private long lastPathfind;
    private long lastMoved;

    private double lastX;
    private double lastY;
    private double lastZ;

    public ProximaEntity(@NotNull EntityType entityType, @NotNull UUID uuid, @NotNull Pathfinding pathfinding) {
        super(entityType, uuid);
        this.pathfinding = Objects.requireNonNull(pathfinding, "pathfinding");
        this.goalGroups = new ArrayList<>(5);
    }

    public @NotNull Pathfinding pathfinding() {
        return pathfinding;
    }

    public void setDestination(@NotNull PathTarget destination) {
        if (getInstance() == null) {
            throw new IllegalArgumentException("Cannot pathfind before an instance is set");
        }

        this.destination = Objects.requireNonNull(destination);
    }

    public void setDestination(@NotNull Entity targetEntity) {
        Instance ourInstance = getInstance();
        if (ourInstance == null) {
            throw new IllegalArgumentException("Cannot pathfind before an instance is set");
        }

        if (ourInstance != targetEntity.getInstance()) {
            throw new IllegalArgumentException("Cannot pathfind to an entity in a different instance");
        }

        this.destination = PathTarget.resolving(() -> {
            if (targetEntity.isRemoved()) {
                return null;
            }

            return VecUtils.toDouble(targetEntity.getPosition());
        }, PositionResolver.seekBelow(pathfinding.spaceHandler.space(), 8, targetEntity.getEntityType().width(),
                Vec.EPSILON), (oldPosition, newPosition) -> oldPosition.distanceSquaredTo(newPosition) > 2);
    }

    /**
     * Adds a {@link GoalGroup} to this entity.
     *
     * @param group The {@link GoalGroup} to add
     */
    public void addGoalGroup(@NotNull GoalGroup group) {
        Objects.requireNonNull(group, "group");
        goalGroups.add(group);
    }

    @Override
    public void tick(long time) {
        Navigator navigator = pathfinding.getNavigator();

        if (navigator.navigationComplete()) {
            currentPath = navigator.getResult();
            if (!initPath(currentPath)) {
                currentPath = null;
            }
        }
        else if (destination != null && (time - lastPathfind < recalculationDelay && destination.hasChanged())) {
            navigator.navigate(position.x(), position.y(), position.z(), destination);
        }

        if (currentPath != null && moveAlongPath(time)) {
            resetPath(time);
        }

        aiTick(time);
        super.tick(time);
    }

    protected void aiTick(long time) {
        for (GoalGroup group : goalGroups) {
            group.tick(time);
        }
    }

    protected boolean initPath(@NotNull PathResult pathResult) {
        recalculationDelay = pathfinding.recalculationDelay(pathResult);

        Node head = pathResult.head();
        if (head == null) {
            return false;
        }

        Node node = head;
        Point currentPosition = getPosition();

        double closestNodeDistance = Double.POSITIVE_INFINITY;
        Node closestNode = null;

        while (node != null) {
            double thisDistance =
                    currentPosition.distanceSquared(node.x + 0.5, node.y + node.blockOffset, node.z + 0.5);

            if (thisDistance < closestNodeDistance) {
                closestNodeDistance = thisDistance;
                closestNode = node;
            }

            if (thisDistance < 1) {
                break;
            }

            node = node.parent;
        }

        assert closestNode != null;

        current = closestNode;

        Node currentParent = closestNode.parent;
        target = currentParent == null ? closestNode : currentParent;

        return true;
    }

    protected boolean withinDistance(@NotNull Node node) {
        Pos position = getPosition();
        return node.x == position.blockX() && node.y == position.blockY() && node.z == position.blockZ();
    }

    protected boolean moveAlongPath(long time) {
        Controller controller = pathfinding.getController(this);

        if (withinDistance(target)) {
            current = target;
            target = current.parent;
        }

        if (target != null) {
            Point pos = getPosition();

            double currentX = pos.x();
            double currentY = pos.y();
            double currentZ = pos.z();

            if (!controller.hasControl()) {
                if (!(currentX == lastX && currentY == lastY && currentZ == lastZ)) {
                    lastMoved = time;
                }
                else if (time - lastMoved > pathfinding.immobileThreshold()) {
                    //if we don't have any movement, stop moving along this path
                    return true;
                }
            }
            else {
                //if jumping, keep updating lastMoved, so we don't consider ourselves stuck
                lastMoved = time;
            }

            controller.advance(current, target);

            lastX = currentX;
            lastY = currentY;
            lastZ = currentZ;
            return false;
        }

        return true;
    }

    protected void resetPath(long time) {
        current = null;
        target = null;
        lastPathfind = time;
        lastMoved = time;
        lastX = 0;
        lastY = 0;
        lastZ = 0;
    }
}
