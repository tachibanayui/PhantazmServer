package org.phantazm.zombies.mapeditor.client;

import com.github.steanky.ethylene.codec.yaml.YamlCodec;
import com.github.steanky.ethylene.core.ConfigCodec;
import io.github.cottonmc.cotton.gui.client.CottonClientScreen;
import io.netty.buffer.Unpooled;
import me.x150.renderer.event.RenderEvents;
import me.x150.renderer.render.Renderer3d;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.phantazm.commons.Namespaces;
import org.phantazm.messaging.MessageChannels;
import org.phantazm.messaging.packet.Packet;
import org.phantazm.messaging.packet.PacketHandler;
import org.phantazm.messaging.packet.c2p.MapDataVersionQueryPacket;
import org.phantazm.messaging.packet.c2p.MapDataVersionResponsePacket;
import org.phantazm.messaging.serialization.PacketSerializer;
import org.phantazm.messaging.serialization.PacketSerializers;
import org.phantazm.zombies.map.FileSystemMapLoader;
import org.phantazm.zombies.map.MapSettingsInfo;
import org.phantazm.zombies.mapeditor.client.packet.PacketByteBufDataReader;
import org.phantazm.zombies.mapeditor.client.packet.PacketByteBufDataWriter;
import org.phantazm.zombies.mapeditor.client.render.ObjectRenderer;
import org.phantazm.zombies.mapeditor.client.ui.MainGui;
import org.phantazm.zombies.mapeditor.client.ui.NewObjectGui;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * {@link ClientModInitializer} for the map editor.
 */
public class MapeditorClient implements ClientModInitializer {
    /**
     * The path string, relative to the Fabric configuration dir, which points to the editor folder.
     */
    private static final String MAPEDITOR_PATH = "mapeditor";

    @Override
    public void onInitializeClient() {
        ConfigCodec codec = new YamlCodec();
        Path defaultMapDirectory = FabricLoader.getInstance().getConfigDir().resolve(MAPEDITOR_PATH);

        Renderer renderer = new Renderer();
        RenderEvents.WORLD.register(renderer::rendered);
        RenderEvents.HUD.register(renderer::hudRender);

        EditorSession editorSession =
                new BasicEditorSession(renderer, new FileSystemMapLoader(defaultMapDirectory, codec),
                        defaultMapDirectory);
        editorSession.loadMapsFromDisk();

        UseBlockCallback.EVENT.register(editorSession::handleBlockUse);

        KeyBinding mapeditorBinding = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(TranslationKeys.KEY_MAPEDITOR_CONFIG, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M,
                        TranslationKeys.CATEGORY_MAPEDITOR_ALL));
        KeyBinding newObject = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(TranslationKeys.KEY_MAPEDITOR_CREATE, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N,
                        TranslationKeys.CATEGORY_MAPEDITOR_ALL));

        PacketSerializer clientToProxy = PacketSerializers.clientToProxySerializer(
                () -> new PacketByteBufDataWriter(new PacketByteBuf(Unpooled.buffer())),
                data -> new PacketByteBufDataReader(new PacketByteBuf(Unpooled.wrappedBuffer(data))));
        Identifier clientToProxyIdentifier = Identifier.of(Namespaces.PHANTAZM, MessageChannels.CLIENT_TO_PROXY);
        if (clientToProxyIdentifier != null) {
            ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> {
                byte[] data = clientToProxy.serializePacket(new MapDataVersionQueryPacket());
                sender.sendPacket(new CustomPayloadC2SPacket(clientToProxyIdentifier,
                        new PacketByteBuf(Unpooled.wrappedBuffer(data))));
            }));

            PacketHandler<PacketSender> clientToProxyHandler = new PacketHandler<>(clientToProxy) {
                @Override
                protected void handlePacket(@NotNull PacketSender packetSender, @NotNull Packet packet) {
                    if (packet instanceof MapDataVersionResponsePacket responsePacket) {
                        ClientPlayerEntity player = MinecraftClient.getInstance().player;

                        if (player != null) {
                            Text message;
                            if (responsePacket.version() == MapSettingsInfo.MAP_DATA_VERSION) {
                                message = Text.translatable(TranslationKeys.CHAT_MAPEDITOR_MAPDATA_VERSION_SYNC_SYNCED)
                                        .formatted(Formatting.GREEN);
                            }
                            else {
                                message = Text.translatable(TranslationKeys.CHAT_MAPEDITOR_MAPDATA_VERSION_SYNC_NOT_SYNCED)
                                        .formatted(Formatting.RED);
                            }
                            player.sendMessage(message);
                        }
                    }
                }

                @Override
                protected void sendToReceiver(@NotNull PacketSender packetSender, byte @NotNull [] data) {
                    packetSender.sendPacket(clientToProxyIdentifier, new PacketByteBuf(Unpooled.wrappedBuffer(data)));
                }
            };
            ClientPlayNetworking.registerGlobalReceiver(clientToProxyIdentifier,
                    (client, handler, buf, responseSender) -> {
                        clientToProxyHandler.handleData(responseSender, buf.getWrittenBytes());
                    });
        }

        ClientTickEvents.END_CLIENT_TICK.register(new ClientTickEvents.EndTick() {
            private BlockHitResult lastBlockLook;

            @Override
            public void onEndTick(MinecraftClient client) {
                ClientPlayerEntity player = client.player;
                if (player == null) {
                    return;
                }

                HitResult hitResult = client.crosshairTarget;
                if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHitResult = (BlockHitResult)hitResult;
                    if (lastBlockLook == null || !lastBlockLook.getBlockPos().equals(blockHitResult.getBlockPos())) {
                        editorSession.handleBlockLookChange(blockHitResult);
                        lastBlockLook = blockHitResult;
                    }
                }

                if ((hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) && lastBlockLook != null) {
                    editorSession.handleBlockLookMiss();
                    lastBlockLook = null;
                }

                if (mapeditorBinding.wasPressed()) {
                    MinecraftClient.getInstance().setScreen(new CottonClientScreen(new MainGui(editorSession)));
                }
                else if (newObject.wasPressed()) {
                    if (!editorSession.hasMap()) {
                        player.sendMessage(Text.translatable(TranslationKeys.GUI_MAPEDITOR_FEEDBACK_NO_ACTIVE_MAP),
                                true);
                        return;
                    }

                    if (!editorSession.hasSelection()) {
                        player.sendMessage(Text.translatable(TranslationKeys.GUI_MAPEDITOR_FEEDBACK_NO_SELECTION),
                                true);
                        return;
                    }

                    MinecraftClient.getInstance().setScreen(new CottonClientScreen(new NewObjectGui(editorSession)));
                }
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(unused -> editorSession.saveMapsToDisk());
    }

    private static class Renderer implements ObjectRenderer {
        private final Map<Key, RenderObject> renderObjects = new HashMap<>();
        private final Map<Key, TextObject> textObjects = new HashMap<>();

        private final Collection<RenderObject> objectValues = renderObjects.values();
        private final Collection<TextObject> textValues = textObjects.values();

        private boolean enabled;
        private boolean renderThroughWalls = false;

        private RenderObject[] bakedObjects;
        private TextObject[] bakedText;

        public void rendered(MatrixStack stack) {
            if (!enabled) {
                return;
            }

            if (renderThroughWalls) {
                Renderer3d.renderThroughWalls();
            }

            if (bakedObjects == null) {
                bakeObjects();
            }

            for (ObjectRenderer.RenderObject object : bakedObjects) {
                if (!object.shouldRender) {
                    //don't render objects whose rendering is disabled
                    continue;
                }

                boolean resetWallRender = false;
                if (!renderThroughWalls && object.renderThroughWalls) {
                    Renderer3d.renderThroughWalls();
                    resetWallRender = true;
                }

                switch (object.type) {
                    case FILLED -> {
                        for (int i = 0; i < object.bounds.length; i += 2) {
                            Renderer3d.renderFilled(stack, object.color, object.bounds[i], object.bounds[i + 1]);
                        }
                    }
                    case OUTLINE -> {
                        for (int i = 0; i < object.bounds.length; i += 2) {
                            Renderer3d.renderOutline(stack, object.color, object.bounds[i], object.bounds[i + 1]);
                        }
                    }
                }

                if (resetWallRender) {
                    Renderer3d.stopRenderThroughWalls();
                }
            }

            Renderer3d.stopRenderThroughWalls();
        }

        public void hudRender(MatrixStack stack) {
            if (!enabled) {
                return;
            }

            TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

            if (bakedText == null) {
                bakeText();
            }

            for (ObjectRenderer.TextObject textObject : bakedText) {
                renderer.draw(stack, textObject.text, textObject.x, textObject.y, textObject.color.getRGB());
            }
        }

        private void bakeObjects() {
            bakedObjects = objectValues.toArray(RenderObject[]::new);
        }

        private void bakeText() {
            bakedText = textValues.toArray(TextObject[]::new);
        }

        @Override
        public void removeObject(@NotNull Key key) {
            Objects.requireNonNull(key, "key");
            if (renderObjects.remove(key) != null) {
                bakedObjects = null;
            }
        }

        @Override
        public void removeText(@NotNull Key key) {
            Objects.requireNonNull(key, "key");
            if (textObjects.remove(key) != null) {
                bakedText = null;
            }
        }

        @Override
        public void removeObjectIf(@NotNull Predicate<? super Key> keyPredicate) {
            Objects.requireNonNull(keyPredicate, "keyPredicate");
            if (renderObjects.keySet().removeIf(keyPredicate)) {
                bakedObjects = null;
            }
        }

        @Override
        public void forEachObject(@NotNull Consumer<? super RenderObject> consumer) {
            Objects.requireNonNull(consumer, "object");
            for (RenderObject sample : objectValues) {
                consumer.accept(sample);
            }
        }

        @Override
        public void putObject(@NotNull RenderObject value) {
            Objects.requireNonNull(value, "value");

            if (tryUpdateInPlace(value, bakedObjects, renderObjects)) {
                renderObjects.put(value.key, value);
                bakedObjects = null;
            }
        }

        @Override
        public void putText(@NotNull TextObject value) {
            Objects.requireNonNull(value, "value");

            if (tryUpdateInPlace(value, bakedText, textObjects)) {
                textObjects.put(value.key, value);
                bakedText = null;
            }
        }

        private static <TObject extends Keyed> boolean tryUpdateInPlace(TObject newObject, TObject[] baked,
                Map<Key, TObject> map) {
            Key newKey = newObject.key();
            TObject oldObject = map.get(newKey);
            if (oldObject != null) {
                if (baked != null) {
                    int i = 0;
                    for (TObject object : baked) {
                        if (object.key().equals(newKey)) {
                            if (oldObject != newObject) {
                                map.put(newKey, newObject);
                            }

                            baked[i] = newObject;
                            return false;
                        }

                        i++;
                    }
                }
            }

            return true;
        }

        @Override
        public void setRenderThroughWalls(boolean renderThroughWalls) {
            this.renderThroughWalls = renderThroughWalls;
        }

        @Override
        public boolean hasObject(@NotNull Key key) {
            return renderObjects.containsKey(key);
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public int size() {
            return renderObjects.size();
        }

        @Override
        public void clear() {
            renderObjects.clear();
            textObjects.clear();

            bakedObjects = null;
            bakedText = null;
        }
    }
}
