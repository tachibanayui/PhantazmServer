package org.phantazm.zombies.equipment.gun.effect;

import com.github.steanky.element.core.annotation.*;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.phantazm.zombies.equipment.gun.GunState;
import org.phantazm.zombies.equipment.gun.audience.AudienceProvider;

import java.util.Objects;

/**
 * A {@link GunEffect} that sends a message to an {@link Audience}.
 */
@Model("zombies.gun.effect.send_message")
@Cache(false)
public class SendMessageEffect implements GunEffect {

    private final Data data;
    private final AudienceProvider audienceProvider;

    /**
     * Creates a {@link SendMessageEffect}.
     *
     * @param data             The {@link SendMessageEffect}'s {@link Data}
     * @param audienceProvider The {@link SendMessageEffect}'s {@link AudienceProvider}
     */
    @FactoryMethod
    public SendMessageEffect(@NotNull Data data,
            @NotNull @Child("audience_provider") AudienceProvider audienceProvider) {
        this.data = Objects.requireNonNull(data, "data");
        this.audienceProvider = Objects.requireNonNull(audienceProvider, "audienceProvider");
    }

    @Override
    public void apply(@NotNull GunState state) {
        audienceProvider.provideAudience().ifPresent(audience -> audience.sendMessage(data.message()));
    }

    @Override
    public void tick(@NotNull GunState state, long time) {

    }

    /**
     * Data for a {@link SendMessageEffect}.
     *
     * @param audienceProvider A path to the {@link SendMessageEffect}'s {@link AudienceProvider}
     * @param message          The {@link Component} to send to the {@link Audience}
     */
    @DataObject
    public record Data(@NotNull @ChildPath("audience_provider") String audienceProvider, @NotNull Component message) {
    }
}
