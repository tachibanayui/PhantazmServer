package com.github.phantazmnetwork.server;

import com.github.phantazmnetwork.commons.Namespaces;
import com.github.phantazmnetwork.commons.vector.Vec3D;
import com.github.phantazmnetwork.core.BasicClientBlockHandlerSource;
import com.github.phantazmnetwork.core.ClientBlockHandler;
import com.github.phantazmnetwork.core.InstanceClientBlockHandler;
import com.github.phantazmnetwork.core.game.scene.fallback.SceneFallback;
import com.github.phantazmnetwork.core.gui.*;
import com.github.phantazmnetwork.core.hologram.Hologram;
import com.github.phantazmnetwork.core.hologram.InstanceHologram;
import com.github.phantazmnetwork.core.hologram.ViewableHologram;
import com.github.phantazmnetwork.core.instance.AnvilFileSystemInstanceLoader;
import com.github.phantazmnetwork.core.instance.InstanceLoader;
import com.github.phantazmnetwork.core.player.PlayerViewProvider;
import com.github.phantazmnetwork.neuron.bindings.minestom.chunk.NeuralChunk;
import com.github.phantazmnetwork.zombies.scene.ZombiesRouteRequest;
import com.github.phantazmnetwork.zombies.scene.ZombiesSceneProvider;
import com.github.phantazmnetwork.zombies.scene.ZombiesSceneRouter;
import com.github.phantazmnetwork.zombies.map.MapInfo;
import com.github.steanky.element.core.context.ContextManager;
import com.github.steanky.element.core.key.KeyParser;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

final class ZombiesTest {
    private static boolean holograms = false;

    private ZombiesTest() {
        throw new UnsupportedOperationException();
    }

    static void initialize(@NotNull EventNode<Event> global, @NotNull Map<Key, MapInfo> maps,
            @NotNull PlayerViewProvider viewProvider, @NotNull ContextManager contextManager,
            @NotNull KeyParser keyParser, @NotNull SceneFallback sceneFallback) {
        global.addListener(PlayerLoginEvent.class, event -> {
            if (holograms) {
                return;
            }

            holograms = true;

            Instance spawnInstance = event.getSpawningInstance();
            if (spawnInstance != null) {
                ClientBlockHandler tracker = new InstanceClientBlockHandler(spawnInstance, global);
                tracker.setClientBlock(Block.BARRIER, 1, 100, 1);

                Hologram hologram = new InstanceHologram(Vec3D.of(1, 101, 1), 0);
                hologram.add(Component.text("This hologram should be...").color(TextColor.color(255, 0, 0)));
                hologram.add(Component.text("...visible to everyone").color(TextColor.color(0, 255, 0)));
                hologram.add(Component.text("And be colored.").color(TextColor.color(0, 0, 255)));
                hologram.setInstance(spawnInstance);

                UUID steankUUID = UUID.fromString("6458e77a-f565-4374-9de7-c2a20be572f3");

                Hologram steankHologram =
                        new ViewableHologram(Vec3D.of(1, 105, 1), 0, player -> player.getUuid().equals(steankUUID));
                steankHologram.add(Component.text("This should only be visible to Steank"));

                Hologram everyoneElseHologram =
                        new ViewableHologram(Vec3D.of(5, 101, 5), 0, player -> !player.getUuid().equals(steankUUID));
                everyoneElseHologram.add(Component.text("This should be visible to everyone EXCEPT Steank"));

                steankHologram.setInstance(spawnInstance);
                everyoneElseHologram.setInstance(spawnInstance);
            }
        });

        global.addListener(EventListener.builder(PlayerChatEvent.class).ignoreCancelled(false).handler(event -> {
            if (event.getMessage().equals("P")) {
                Component message = Component.text("Play Pigstep", TextColor.color(0, 255, 0));
                Gui gui = Gui.builder(InventoryType.CHEST_6_ROW, new BasicSlotDistributor(1)).setDynamic(true).withItem(
                        GuiItem.builder().withItem(ItemStack.of(Material.MUSIC_DISC_PIGSTEP).withDisplayName(message))
                                .withUpdater(new ItemUpdater() {
                                    private static final Component[] FRAMES =
                                            new Component[] {Component.text("Play Pigstep", TextColor.color(0, 255, 0)),
                                                    Component.text("Play Pigstep.", TextColor.color(0, 0, 255)),
                                                    Component.text("Play Pigstep..", TextColor.color(255, 0, 0)),
                                                    Component.text("Play Pigstep...", TextColor.color(255, 255, 0)),
                                                    Component.text("Play Pigstep....", TextColor.color(255, 0, 255)),
                                                    Component.text("Play Pigstep.....", TextColor.color(0, 255, 255)),
                                                    Component.text("Play Pigstep......",
                                                            TextColor.color(255, 255, 255))};
                                    private long lastUpdate = 0;
                                    private int curFrame = 0;

                                    @Override
                                    public @NotNull ItemStack update(long time, @NotNull ItemStack current) {
                                        ItemStack newStack = current.withDisplayName(FRAMES[curFrame++]);
                                        if (curFrame == FRAMES.length) {
                                            curFrame = 0;
                                        }

                                        return newStack;
                                    }

                                    @Override
                                    public boolean hasUpdate(long time, @NotNull ItemStack current) {
                                        long sinceLastUpdate = time - lastUpdate;
                                        if (sinceLastUpdate > 250) {
                                            lastUpdate = time;
                                            return true;
                                        }

                                        return false;
                                    }
                                }).withClickHandler(((ClickHandler)(owner, player, slot, clickType) -> {
                                    player.sendMessage(Component.text("Playing pigstep."));
                                    pigstepRandomPitch(player);

                                    for (int i = 1; i < 50; i++) {
                                        player.scheduler()
                                                .scheduleTask(() -> pigstepRandomPitch(player), TaskSchedule.millis(100 * i),
                                                        TaskSchedule.stop());
                                    }

                                    player.closeInventory();
                                }).filter(GuiItem.ClickType.LEFT_CLICK)).build()).build();
                event.getPlayer().openInventory(gui);
            }
        }).build());

        global.addListener(PlayerTickEvent.class, event -> {
            Player player = event.getPlayer();
            Inventory inventory = player.getOpenInventory();

            if (inventory instanceof Gui gui) {
                gui.tick(System.currentTimeMillis());
            }
        });


        Key testMapKey = Key.key(Namespaces.PHANTAZM, "test_map");
        MapInfo testMap = maps.get(testMapKey);

        if (testMap != null) {
            InstanceLoader instanceLoader =
                    new AnvilFileSystemInstanceLoader(Path.of("./zombies/instances/"), NeuralChunk::new);
            ZombiesSceneProvider testProvider =
                    new ZombiesSceneProvider(1, testMap, MinecraftServer.getInstanceManager(), instanceLoader,
                            sceneFallback, global, Mob.getMobSpawner(), Mob.getMobStore(), Mob.getModels(),
                            new BasicClientBlockHandlerSource(
                                    instance -> new InstanceClientBlockHandler(instance, global)), contextManager,
                            keyParser, EquipmentFeature::createEquipmentCreator);
            ZombiesSceneRouter sceneRouter = new ZombiesSceneRouter(Map.of(testMapKey, testProvider));

            global.addListener(EventListener.builder(PlayerChatEvent.class).ignoreCancelled(false).handler(event -> {
                String msg = event.getMessage();
                Player player = event.getPlayer();
                Instance instance = player.getInstance();

                if (instance == null) {
                    return;
                }

                switch (msg) {
                    case "warp" -> {
                        sceneRouter.join(new ZombiesRouteRequest(testMapKey,
                                () -> Collections.singleton(viewProvider.fromPlayer(player))));
                    }
                }
            }).build());

            MinecraftServer.getSchedulerManager().scheduleTask(() -> {
                sceneRouter.tick(System.currentTimeMillis());
            }, TaskSchedule.immediate(), TaskSchedule.tick(1));
        }
    }

    private static void pigstepRandomPitch(Player player) {
        player.playSound(Sound.sound(Key.key("minecraft:music_disc.pigstep"), Sound.Source.RECORD, 100,
                (float)Math.random() * 2F), Sound.Emitter.self());
    }
}
