package com.minecraftclone.save;

import com.minecraftclone.player.GameMode;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public final class WorldSaveService {
    private static final int WORLD_VERSION = 1;
    private static final int CHUNKS_VERSION = 1;
    private static final int PLAYER_VERSION = 2;
    private static final int LEGACY_PLAYER_VERSION = 1;
    private static final int HOTBAR_SLOT_COUNT = 9;

    private final Path worldDirectory;
    private final Path worldFile;
    private final Path playerFile;
    private final Path modificationsFile;

    public WorldSaveService(Path worldDirectory) {
        this.worldDirectory = worldDirectory;
        this.worldFile = worldDirectory.resolve("world.dat");
        this.playerFile = worldDirectory.resolve("player.dat");
        this.modificationsFile = worldDirectory.resolve("chunks.dat");
    }

    public WorldLoadResult loadOrCreate(long fallbackSeed) {
        ensureDirectory();

        long seed = Files.exists(worldFile) ? readSeed() : fallbackSeed;
        PlayerSaveData player = Files.exists(playerFile) ? readPlayer() : null;
        Map<Long, Map<Integer, Byte>> modifications = Files.exists(modificationsFile)
                ? readModifications()
                : new HashMap<>();

        return new WorldLoadResult(seed, player, modifications);
    }

    public void save(long seed, PlayerSaveData player, Map<Long, Map<Integer, Byte>> modifications) {
        ensureDirectory();
        writeSeed(seed);
        writePlayer(player);
        writeModifications(modifications);
    }

    private long readSeed() {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(worldFile)))) {
            int version = in.readInt();
            if (version != WORLD_VERSION) {
                throw new IllegalStateException("Version de world.dat no soportada: " + version);
            }
            return in.readLong();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer world.dat", e);
        }
    }

    private PlayerSaveData readPlayer() {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(playerFile)))) {
            int version = in.readInt();
            if (version != LEGACY_PLAYER_VERSION && version != PLAYER_VERSION) {
                throw new IllegalStateException("Version de player.dat no soportada: " + version);
            }

            float x = in.readFloat();
            float y = in.readFloat();
            float z = in.readFloat();
            float yaw = in.readFloat();
            float pitch = in.readFloat();
            int modeOrdinal = in.readInt();
            int selectedBlock = in.readInt();

            GameMode mode = GameMode.values()[Math.max(0, Math.min(modeOrdinal, GameMode.values().length - 1))];

            if (version == LEGACY_PLAYER_VERSION) {
                return new PlayerSaveData(x, y, z, yaw, pitch, mode, selectedBlock, null, null);
            }

            int slotCount = in.readInt();
            byte[] blockIds = new byte[slotCount];
            int[] counts = new int[slotCount];
            for (int i = 0; i < slotCount; i++) {
                blockIds[i] = in.readByte();
                counts[i] = in.readInt();
            }

            return new PlayerSaveData(x, y, z, yaw, pitch, mode, selectedBlock, blockIds, counts);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer player.dat", e);
        }
    }

    private Map<Long, Map<Integer, Byte>> readModifications() {
        Map<Long, Map<Integer, Byte>> out = new HashMap<>();

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(modificationsFile)))) {
            int version = in.readInt();
            if (version != CHUNKS_VERSION) {
                throw new IllegalStateException("Version de chunks.dat no soportada: " + version);
            }

            int chunkEntries = in.readInt();
            for (int i = 0; i < chunkEntries; i++) {
                long key = in.readLong();
                int blockEntries = in.readInt();

                Map<Integer, Byte> map = new HashMap<>(Math.max(16, blockEntries * 2));
                for (int j = 0; j < blockEntries; j++) {
                    int localIndex = in.readInt();
                    byte blockId = in.readByte();
                    map.put(localIndex, blockId);
                }
                out.put(key, map);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer chunks.dat", e);
        }

        return out;
    }

    private void writeSeed(long seed) {
        writeAtomically(worldFile, out -> {
            out.writeInt(WORLD_VERSION);
            out.writeLong(seed);
        });
    }

    private void writePlayer(PlayerSaveData player) {
        if (player == null) {
            return;
        }
        writeAtomically(playerFile, out -> {
            out.writeInt(PLAYER_VERSION);
            out.writeFloat(player.x());
            out.writeFloat(player.y());
            out.writeFloat(player.z());
            out.writeFloat(player.yaw());
            out.writeFloat(player.pitch());
            out.writeInt(player.mode().ordinal());
            out.writeInt(player.selectedBlockIndex());

            byte[] ids = normalizeHotbarIds(player.hotbarBlockIds());
            int[] counts = normalizeHotbarCounts(player.hotbarCounts());
            out.writeInt(HOTBAR_SLOT_COUNT);
            for (int i = 0; i < HOTBAR_SLOT_COUNT; i++) {
                out.writeByte(ids[i]);
                out.writeInt(counts[i]);
            }
        });
    }

    private void writeModifications(Map<Long, Map<Integer, Byte>> modifications) {
        writeAtomically(modificationsFile, out -> {
            out.writeInt(CHUNKS_VERSION);
            out.writeInt(modifications.size());

            for (Map.Entry<Long, Map<Integer, Byte>> chunkEntry : modifications.entrySet()) {
                out.writeLong(chunkEntry.getKey());
                Map<Integer, Byte> perChunk = chunkEntry.getValue();
                out.writeInt(perChunk.size());

                for (Map.Entry<Integer, Byte> blockEntry : perChunk.entrySet()) {
                    out.writeInt(blockEntry.getKey());
                    out.writeByte(blockEntry.getValue());
                }
            }
        });
    }

    private void writeAtomically(Path target, DataWriter writer) {
        Path temp;
        try {
            temp = Files.createTempFile(worldDirectory, "tmp-", ".bin");
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear temporal de guardado", e);
        }

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)))) {
            writer.write(out);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo escribir archivo temporal", e);
        }

        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicError) {
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException fallbackError) {
                throw new IllegalStateException("No se pudo reemplazar archivo de guardado", fallbackError);
            }
        }
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(worldDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear directorio del mundo: " + worldDirectory, e);
        }
    }

    private static byte[] normalizeHotbarIds(byte[] raw) {
        byte[] out = new byte[HOTBAR_SLOT_COUNT];
        if (raw == null) {
            return out;
        }
        System.arraycopy(raw, 0, out, 0, Math.min(raw.length, HOTBAR_SLOT_COUNT));
        return out;
    }

    private static int[] normalizeHotbarCounts(int[] raw) {
        int[] out = new int[HOTBAR_SLOT_COUNT];
        if (raw == null) {
            return out;
        }
        for (int i = 0; i < Math.min(raw.length, HOTBAR_SLOT_COUNT); i++) {
            out[i] = Math.max(0, raw[i]);
        }
        return out;
    }

    @FunctionalInterface
    private interface DataWriter {
        void write(DataOutputStream out) throws IOException;
    }
}
