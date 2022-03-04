package com.github.phantazmnetwork.neuron.operation;

import com.github.phantazmnetwork.neuron.node.Node;
import org.jetbrains.annotations.NotNull;

public interface PathResult {
    @NotNull PathOperation getOperation();

    @NotNull Node getPath();
}
