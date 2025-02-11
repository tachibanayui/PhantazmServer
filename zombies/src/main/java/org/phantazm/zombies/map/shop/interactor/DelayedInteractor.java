package org.phantazm.zombies.map.shop.interactor;

import com.github.steanky.element.core.annotation.*;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.phantazm.zombies.map.shop.PlayerInteraction;

import java.util.Objects;

@Model("zombies.map.shop.interactor.delayed")
@Cache(false)
public class DelayedInteractor extends InteractorBase<DelayedInteractor.Data> {
    private final ShopInteractor target;

    private PlayerInteraction interaction;
    private long startTime;

    @FactoryMethod
    public DelayedInteractor(@NotNull Data data, @NotNull @Child("target") ShopInteractor target) {
        super(data);
        this.target = Objects.requireNonNull(target, "target");
    }

    @Override
    public boolean handleInteraction(@NotNull PlayerInteraction interaction) {
        if (this.interaction == null || data.resetOnInteract) {
            this.startTime = System.currentTimeMillis();
            this.interaction = interaction;
        }

        return true;
    }

    @Override
    public void tick(long time) {
        super.tick(time);

        if (interaction != null) {
            long elapsedTimeMs = time - startTime;
            int elapsedTicks = (int)(elapsedTimeMs / MinecraftServer.TICK_MS);

            if (elapsedTicks >= data.delayTicks) {
                target.handleInteraction(interaction);
                interaction = null;
            }
        }
    }

    @DataObject
    record Data(@ChildPath("target") String targetPath, int delayTicks, boolean resetOnInteract) {

    }
}
