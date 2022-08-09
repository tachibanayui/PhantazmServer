package com.github.phantazmnetwork.core;

import com.github.phantazmnetwork.commons.vector.Vec3I;
import com.github.phantazmnetwork.commons.vector.Vec3IBase;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.PreBlockChangeEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerChunkLoadEvent;
import net.minestom.server.event.player.PrePlayerStartDiggingEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.listener.PlayerDiggingListener;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Supports instance-wide client blocks.
 *
 * @see ClientBlockHandlerSource
 */
public class InstanceClientBlockHandler implements ClientBlockHandler {
    private final Reference<Instance> instance;
    private final Long2ObjectMap<ObjectOpenHashSet<PositionedBlock>> clientBlocks;

    /**
     * <p>Constructs a new instance of this class bound to the provided {@link Instance}. This will add a few necessary
     * listeners to the given {@link EventNode}.</p>
     *
     * <p>The lifetime of this object is free to exceed that of the Instance; no strong reference to it is retained.</p>
     *
     * @param instance   the instance this handler is bound to
     * @param globalNode the node to add block event listeners to
     */
    public InstanceClientBlockHandler(@NotNull Instance instance, @NotNull EventNode<Event> globalNode) {
        //weakref necessary as instance field will be captured by the expiration predicate
        this.instance = new WeakReference<>(Objects.requireNonNull(instance, "instance"));
        this.clientBlocks = new Long2ObjectOpenHashMap<>();

        Predicate<InstanceEvent> filter = event -> event.getInstance() == this.instance.get();
        Predicate<Object> expire = ignored -> {
            Instance current = this.instance.get();
            return current == null || !current.isRegistered();
        };

        globalNode.addListener(EventListener.builder(PlayerChunkLoadEvent.class).filter(filter).expireWhen(expire)
                .handler(this::onPlayerChunkLoad).build());
        globalNode.addListener(EventListener.builder(PreBlockChangeEvent.class).filter(filter).expireWhen(expire)
                .handler(this::onPreBlockChange).build());
        globalNode.addListener(EventListener.builder(PlayerBlockBreakEvent.class).filter(filter).expireWhen(expire)
                .handler(this::onPlayerBlockBreak).build());
        globalNode.addListener(EventListener.builder(PrePlayerStartDiggingEvent.class).filter(filter).expireWhen(expire)
                .handler(this::onPrePlayerStartDigging).build());
    }

    @Override
    public void setClientBlock(@NotNull Block type, int x, int y, int z) {
        Instance instance = this.instance.get();
        if (instance == null) {
            return;
        }

        int cx = ChunkUtils.getChunkCoordinate(x);
        int cz = ChunkUtils.getChunkCoordinate(z);
        long index = ChunkUtils.getChunkIndex(cx, cz);

        synchronized (clientBlocks) {
            ObjectOpenHashSet<PositionedBlock> positionedBlocks = clientBlocks.get(index);
            if (positionedBlocks == null) {
                clientBlocks.put(index, positionedBlocks = new ObjectOpenHashSet<>(6));
            }

            Vec3I pos = Vec3I.of(x, y, z);
            PositionedBlock block = positionedBlocks.get(pos);
            if (block == null) {
                positionedBlocks.add(new PositionedBlock(type, pos));
            }
            else {
                block.block = type;
            }

            Chunk chunk = instance.getChunk(cx, cz);
            if (chunk != null) {
                chunk.sendPacketToViewers(new BlockChangePacket(new Vec(x, y, z), type));
            }
        }
    }

    @Override
    public void clearClientBlocks() {
        Instance instance = this.instance.get();
        if (instance == null) {
            return;
        }

        synchronized (clientBlocks) {
            for (Long2ObjectMap.Entry<ObjectOpenHashSet<PositionedBlock>> entry : clientBlocks.long2ObjectEntrySet()) {
                long index = entry.getLongKey();
                int x = ChunkUtils.getChunkCoordX(index);
                int z = ChunkUtils.getChunkCoordZ(index);

                Chunk chunk = instance.getChunkAt(x, z);
                ObjectOpenHashSet<PositionedBlock> blocks = entry.getValue();
                if (chunk != null) {
                    for (PositionedBlock block : blocks) {
                        Block serverBlock = chunk.getBlock(block.getX(), block.getY(), block.getZ());
                        chunk.sendPacketToViewers(new BlockChangePacket(VecUtils.toPoint(block.position), serverBlock));
                    }
                }

                blocks.clear();
            }

            clientBlocks.clear();
        }
    }

    @Override
    public void removeClientBlock(int x, int y, int z) {
        Instance instance = this.instance.get();
        if (instance == null) {
            return;
        }

        int cx = ChunkUtils.getChunkCoordinate(x);
        int cz = ChunkUtils.getChunkCoordinate(z);
        long index = ChunkUtils.getChunkIndex(cx, cz);
        synchronized (clientBlocks) {
            ObjectOpenHashSet<PositionedBlock> blocks = clientBlocks.get(index);

            if (blocks != null) {
                //not sus, equals/hashCode is comparable between all Vec3I
                //noinspection SuspiciousMethodCalls
                if (blocks.remove(Vec3I.of(x, y, z))) {
                    Chunk chunk = instance.getChunk(cx, cz);

                    if (chunk != null) {
                        Block serverBlock = chunk.getBlock(x, y, z);

                        //make sure player gets the actual block
                        chunk.sendPacketToViewers(new BlockChangePacket(new Vec(x, y, z), serverBlock));
                    }
                }
            }
        }
    }

    private void onPrePlayerStartDigging(PrePlayerStartDiggingEvent event) {
        Point blockPosition = event.getBlockPosition();
        long index = ChunkUtils.getChunkIndex(blockPosition);
        synchronized (clientBlocks) {
            ObjectOpenHashSet<PositionedBlock> blocks = clientBlocks.get(index);

            if (blocks != null) {
                PositionedBlock block = blocks.get(VecUtils.toBlockInt(blockPosition));

                if (block != null) {
                    if (event.getResult().success()) {
                        return; //don't change
                    }

                    event.setResult(new PlayerDiggingListener.DiggingResult(block.block, false));
                }
            }
        }
    }

    private void onPlayerBlockBreak(PlayerBlockBreakEvent event) {
        Point blockPosition = event.getBlockPosition();
        long index = ChunkUtils.getChunkIndex(blockPosition);
        synchronized (clientBlocks) {
            ObjectOpenHashSet<PositionedBlock> blocks = clientBlocks.get(index);

            if (blocks != null) {
                //remove the client block; no need to send something else as it will be updated soon
                //noinspection SuspiciousMethodCalls
                blocks.remove(VecUtils.toBlockInt(blockPosition));
            }
        }
    }

    private void onPreBlockChange(PreBlockChangeEvent event) {
        Point blockPosition = event.blockPosition();
        long index = ChunkUtils.getChunkIndex(blockPosition);
        synchronized (clientBlocks) {
            ObjectOpenHashSet<PositionedBlock> blocks = clientBlocks.get(index);

            if (blocks != null) {
                //noinspection SuspiciousMethodCalls
                if (blocks.contains(VecUtils.toBlockInt(blockPosition))) {
                    //allow the server to update the block, but don't tell the client
                    event.setSyncClient(false);
                }
            }
        }
    }

    private void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        int cx = event.getChunkX();
        int cz = event.getChunkZ();

        BlockChangePacket[] packets = null;
        long index = ChunkUtils.getChunkIndex(cx, cz);
        synchronized (clientBlocks) {
            ObjectOpenHashSet<PositionedBlock> blocks = clientBlocks.get(index);

            if (blocks != null) {
                Iterator<PositionedBlock> blockIterator = blocks.iterator();
                packets = new BlockChangePacket[blocks.size()];
                for (int i = 0; i < packets.length; i++) {
                    PositionedBlock block = blockIterator.next();
                    packets[i] = new BlockChangePacket(VecUtils.toPoint(block.position), block.block);
                }
            }
        }

        if (packets != null) {
            event.getPlayer().sendPackets(packets);
        }
    }

    private static class PositionedBlock extends Vec3IBase {
        private final Vec3I position;
        private Block block;

        private PositionedBlock(Block block, Vec3I position) {
            this.block = block;
            this.position = position;
        }

        @Override
        public int getX() {
            return position.getX();
        }

        @Override
        public int getY() {
            return position.getY();
        }

        @Override
        public int getZ() {
            return position.getZ();
        }
    }
}
