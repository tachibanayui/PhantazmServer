package com.github.phantazmnetwork.api;

import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Standard implementation of {@link ClientBlockHandlerSource}.
 */
public class BasicClientBlockHandlerSource implements ClientBlockHandlerSource {
    private final Function<Instance, ClientBlockHandler> blockHandlerFunction;
    private final Map<Instance, ClientBlockHandler> map;

    /**
     * Creates a new instance of this class given the provided {@link ClientBlockHandler}-producing function. This
     * function must never return null.
     * @param handlerFunction the handler function, which should never return null
     */
    public BasicClientBlockHandlerSource(@NotNull Function<Instance, ClientBlockHandler> handlerFunction) {
        this.blockHandlerFunction = Objects.requireNonNull(handlerFunction, "handlerFunction");
        this.map = new WeakHashMap<>();
    }

    @Override
    public @NotNull ClientBlockHandler forInstance(@NotNull Instance instance) {
        return map.computeIfAbsent(Objects.requireNonNull(instance, "instance"), world ->
                Objects.requireNonNull(this.blockHandlerFunction.apply(world), "handler"));
    }
}
