package org.phantazm.mob.skill;

import com.github.steanky.element.core.annotation.*;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.phantazm.mob.PhantazmMob;
import org.phantazm.mob.target.TargetSelector;

import java.util.Objects;

/**
 * A {@link Skill} that plays a {@link Sound}.
 */
@Model("mob.skill.play_sound")
@Cache(false)
public class PlaySoundSkill implements Skill {
    private final Data data;
    private final TargetSelector<? extends Audience> selector;

    /**
     * Creates a {@link PlaySoundSkill}.
     *
     * @param selector The {@link TargetSelector} used to select {@link Audience}s
     */
    @FactoryMethod
    public PlaySoundSkill(@NotNull Data data, @NotNull @Child("selector") TargetSelector<? extends Audience> selector) {
        this.data = Objects.requireNonNull(data, "data");
        this.selector = Objects.requireNonNull(selector, "selector");
    }

    @Override
    public void use(@NotNull PhantazmMob self) {
        selector.selectTarget(self).ifPresent(audience -> {
            if (data.followAudience()) {
                audience.playSound(data.sound(), Sound.Emitter.self());
            }
            else {
                Instance instance = self.entity().getInstance();
                if (instance != null) {
                    instance.playSound(data.sound, self.entity().getPosition());
                }
            }
        });
    }

    @Override
    public void tick(long time, @NotNull PhantazmMob self) {

    }

    @DataObject
    public record Data(@NotNull @ChildPath("selector") String selector, @NotNull Sound sound, boolean followAudience) {

        public Data {
            Objects.requireNonNull(selector, "selector");
            Objects.requireNonNull(sound, "sound");
        }

    }

}