package com.github.phantazmnetwork.server.config.server;

import org.jetbrains.annotations.NotNull;

/**
 * Config for server info
 * @param serverIP The IP to run the server on
 * @param port The port to run the server on
 * @param optifineEnabled Whether optifine support is enabled
 * @param authType The type of authentication the server will use
 * @param velocitySecret The secret used for authentication
 */
public record ServerInfoConfig(
        @NotNull String serverIP,
        int port,
        boolean optifineEnabled,
        @NotNull AuthType authType,
        @NotNull String velocitySecret
) {

}