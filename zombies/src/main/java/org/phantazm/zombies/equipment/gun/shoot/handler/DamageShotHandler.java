package org.phantazm.zombies.equipment.gun.shoot.handler;

import com.github.steanky.element.core.annotation.*;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.event.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.phantazm.zombies.Tags;
import org.phantazm.zombies.equipment.gun.Gun;
import org.phantazm.zombies.equipment.gun.GunState;
import org.phantazm.zombies.equipment.gun.shoot.GunHit;
import org.phantazm.zombies.equipment.gun.shoot.GunShot;
import org.phantazm.zombies.event.EntityGunDamageEvent;

import java.beans.EventHandler;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/**
 * A {@link ShotHandler} that deals damage to targets.
 */
@Model("zombies.gun.shot_handler.damage")
@Cache
public class DamageShotHandler implements ShotHandler {

    private final Data data;

    /**
     * Creates a new {@link DamageShotHandler} with the given {@link Data}.
     *
     * @param data The {@link Data} to use
     */
    @FactoryMethod
    public DamageShotHandler(@NotNull Data data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public void handle(@NotNull Gun gun, @NotNull GunState state, @NotNull Entity attacker,
            @NotNull Collection<UUID> previousHits, @NotNull GunShot shot) {
        handleDamageTargets(gun, attacker, shot.regularTargets(), data.damage, false);
        handleDamageTargets(gun, attacker, shot.headshotTargets(), data.headshotDamage, true);
    }

    private void handleDamageTargets(Gun gun, Entity attacker, Collection<GunHit> targets, float damage,
            boolean headshot) {
        boolean hasInstakill = attacker.getTag(Tags.HAS_INSTAKILL);

        for (GunHit target : targets) {
            LivingEntity targetEntity = target.entity();
            if (hasInstakill && !targetEntity.hasTag(Tags.RESIST_INSTAKILL)) {
                EntityGunDamageEvent event =
                        new EntityGunDamageEvent(gun, targetEntity, attacker, headshot, true, damage);
                EventDispatcher.call(event);
                if (!event.isCancelled()) {
                    targetEntity.kill();
                }

                continue;
            }

            EntityGunDamageEvent event = new EntityGunDamageEvent(gun, targetEntity, attacker, headshot, false, damage);
            EventDispatcher.call(event);

            if (!event.isCancelled()) {
                targetEntity.damage(DamageType.fromEntity(attacker), damage);
            }
        }
    }

    @Override
    public void tick(@NotNull GunState state, long time) {

    }

    /**
     * Data for a {@link DamageShotHandler}.
     *
     * @param damage         The amount of damage to deal to regular targets
     * @param headshotDamage The amount of damage to deal to headshots
     */
    @DataObject
    public record Data(float damage, float headshotDamage) {

    }

}
