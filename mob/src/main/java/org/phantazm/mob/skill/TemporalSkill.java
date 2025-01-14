package org.phantazm.mob.skill;

import com.github.steanky.element.core.annotation.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.phantazm.commons.MathUtils;
import org.phantazm.mob.PhantazmMob;

import java.util.UUID;

@Model("mob.skill.temporal")
@Cache(false)
public class TemporalSkill implements Skill {
    private final Data data;
    private final Skill delegate;

    private final boolean delegateNeedsTicking;
    private final Tag<Long> startTime;
    private final Tag<Long> actualDelay;

    @FactoryMethod
    public TemporalSkill(@NotNull Data data, @NotNull @Child("delegate") Skill delegate) {
        this.data = data;
        this.delegate = delegate;
        this.delegateNeedsTicking = delegate.needsTicking();

        UUID uuid = UUID.randomUUID();
        this.startTime = Tag.Long("start_time_" + uuid).defaultValue(-1L);
        this.actualDelay = Tag.Long("actual_delay_" + uuid).defaultValue(-1L);
    }

    @Override
    public void tick(long time, @NotNull PhantazmMob self) {
        if (delegateNeedsTicking) {
            delegate.tick(time, self);
        }

        Entity entity = self.entity();
        long startTime = entity.getTag(this.startTime);
        if (startTime < 0) {
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed / MinecraftServer.TICK_MS >= entity.getTag(this.actualDelay)) {
            delegate.end(self);

            entity.removeTag(this.startTime);
            entity.removeTag(actualDelay);
        }
    }

    @Override
    public void use(@NotNull PhantazmMob self) {
        delegate.use(self);

        Entity entity = self.entity();
        entity.setTag(startTime, System.currentTimeMillis());
        entity.setTag(actualDelay, MathUtils.randomInterval(data.minDuration, data.maxDuration));
    }

    @Override
    public boolean needsTicking() {
        return true;
    }

    @DataObject
    public record Data(long minDuration, int maxDuration, @NotNull @ChildPath("delegate") String delegate) {
    }
}
