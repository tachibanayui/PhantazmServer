package org.phantazm.zombies.command;

import com.github.steanky.element.core.key.KeyParser;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.phantazm.core.game.scene.RouteResult;
import org.phantazm.core.game.scene.Scene;
import org.phantazm.core.player.PlayerViewProvider;
import org.phantazm.zombies.map.MapInfo;
import org.phantazm.zombies.scene.ZombiesRouteRequest;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class ZombiesJoinCommand extends Command {
    public ZombiesJoinCommand(@NotNull Scene<ZombiesRouteRequest> router, @NotNull KeyParser keyParser,
            @NotNull Map<Key, MapInfo> maps, @NotNull PlayerViewProvider viewProvider) {
        super("join");

        Argument<String> mapKeyArgument = ArgumentType.String("map-key");

        Objects.requireNonNull(router, "router");
        Objects.requireNonNull(keyParser, "keyParser");
        Objects.requireNonNull(maps, "maps");
        Objects.requireNonNull(viewProvider, "viewProvider");

        mapKeyArgument.setSuggestionCallback((sender, context, suggestion) -> {
            for (Map.Entry<Key, MapInfo> entry : maps.entrySet()) {
                suggestion.addEntry(
                        new SuggestionEntry(entry.getKey().asString(), entry.getValue().settings().displayName()));
            }
        });
        addConditionalSyntax((sender, commandString) -> {
            if (sender instanceof Player) {
                return true;
            }

            sender.sendMessage(Component.text("You have to be a player to use that command!", NamedTextColor.RED));
            return false;
        }, (sender, context) -> {
            @Subst("test_map")
            String mapKeyString = context.get(mapKeyArgument);
            if (!keyParser.isValidKey(mapKeyString)) {
                sender.sendMessage(Component.text("Invalid key!", NamedTextColor.RED));
                return;
            }

            Key mapKey = keyParser.parseKey(mapKeyString);
            if (!maps.containsKey(mapKey)) {
                sender.sendMessage(Component.text("Invalid map!", NamedTextColor.RED));
                return;
            }

            RouteResult result = router.join(new ZombiesRouteRequest(mapKey,
                    () -> Collections.singleton(viewProvider.fromPlayer((Player)sender))));
            result.message().ifPresent(sender::sendMessage);
        }, mapKeyArgument);
    }

}