package org.phantazm.core.game.scene;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

/**
 * An abstract base for {@link SceneProvider}s.
 */
public abstract class SceneProviderAbstract<TScene extends Scene<TRequest>, TRequest extends SceneJoinRequest>
        implements SceneProvider<TScene, TRequest> {
    private final Collection<TScene> scenes = new ArrayList<>();
    private final Collection<TScene> unmodifiableScenes = Collections.unmodifiableCollection(scenes);
    private final int maximumScenes;

    /**
     * Creates an abstract {@link SceneProvider}.
     *
     * @param maximumScenes The maximum number of {@link Scene}s in the provider.
     */
    public SceneProviderAbstract(int maximumScenes) {
        this.maximumScenes = maximumScenes;
    }

    @Override
    public @NotNull Optional<TScene> provideScene(@NotNull TRequest request) {
        return Optional.ofNullable(chooseScene(request).orElseGet(() -> {
            if (scenes.size() >= maximumScenes) {
                return null;
            }

            TScene newScene = createScene(request);
            scenes.add(newScene);

            return newScene;
        }));
    }

    @Override
    public @UnmodifiableView @NotNull Collection<TScene> getScenes() {
        return unmodifiableScenes;
    }

    @Override
    public void forceShutdown() {
        for (TScene scene : scenes) {
            scene.shutdown();
        }

        scenes.clear();
    }

    @Override
    public void tick(long time) {
        Iterator<TScene> iterator = scenes.iterator();

        while (iterator.hasNext()) {
            TScene scene = iterator.next();

            if (scene.isShutdown()) {
                cleanupScene(scene);
                iterator.remove();
            }
            else {
                scene.tick(time);
            }
        }
    }

    /**
     * Chooses a {@link Scene} to be used for a request. This should already be a {@link Scene} in the provider.
     *
     * @param request The request used to choose a {@link Scene}
     * @return An optional of a chosen {@link Scene}
     */
    protected abstract @NotNull Optional<TScene> chooseScene(@NotNull TRequest request);

    /**
     * Creates a {@link Scene}.
     *
     * @param request The join request which triggered the creation of the {@link Scene}
     * @return The new {@link Scene}
     */
    protected abstract @NotNull TScene createScene(@NotNull TRequest request);

    protected abstract void cleanupScene(@NotNull TScene scene);

}
