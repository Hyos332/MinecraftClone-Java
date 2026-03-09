package com.minecraftclone.game;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.camera.FpsCamera;
import com.minecraftclone.engine.EngineContext;
import com.minecraftclone.engine.GameLogic;
import com.minecraftclone.input.InputManager;
import com.minecraftclone.player.GameMode;
import com.minecraftclone.player.PlayerController;
import com.minecraftclone.render.voxel.VoxelWorldRenderer;
import com.minecraftclone.save.PlayerSaveData;
import com.minecraftclone.save.WorldLoadResult;
import com.minecraftclone.save.WorldSaveService;
import com.minecraftclone.ui.UiRenderer;
import com.minecraftclone.world.Chunk;
import com.minecraftclone.world.VoxelWorld;
import com.minecraftclone.world.raycast.RaycastHit;
import com.minecraftclone.world.raycast.VoxelRaycaster;
import java.nio.file.Path;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_G;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public final class PhaseThreeGame implements GameLogic {
    private static final long DEFAULT_SEED = 2_026_030_9L;
    private static final int LOAD_RADIUS_CHUNKS = 8;
    private static final int UNLOAD_RADIUS_CHUNKS = 12;
    private static final float INTERACT_DISTANCE = 6.5f;
    private static final double AUTOSAVE_INTERVAL_SECONDS = 25.0;

    private static final BlockType[] HOTBAR_BLOCKS = {
            BlockType.DIRT,
            BlockType.STONE,
            BlockType.WOOD,
            BlockType.SAND,
            BlockType.LEAVES,
            BlockType.GRASS,
            BlockType.IRON_ORE,
            BlockType.COAL_ORE,
            BlockType.WATER
    };

    private EngineContext context;
    private InputManager input;

    private WorldSaveService saveService;

    private VoxelWorld world;
    private VoxelWorldRenderer worldRenderer;
    private UiRenderer uiRenderer;
    private VoxelRaycaster raycaster;

    private FpsCamera camera;
    private PlayerController player;

    private Vector3f spawnPosition;

    private int selectedBlockIndex;
    private double elapsedTimeSeconds;
    private double autosaveTimerSeconds;

    @Override
    public void init(EngineContext context) {
        this.context = context;
        this.input = context.input();

        this.saveService = new WorldSaveService(Path.of("worlds", "main"));

        WorldLoadResult loaded = saveService.loadOrCreate(DEFAULT_SEED);

        this.world = new VoxelWorld(loaded.seed());
        this.world.loadModifications(loaded.chunkModifications());

        PlayerSaveData playerSave = loaded.player();
        Vector3f bootstrapPosition = playerSave != null
                ? new Vector3f(playerSave.x(), playerSave.y(), playerSave.z())
                : new Vector3f(0.5f, 48.0f, 0.5f);

        world.updateStreaming(bootstrapPosition, LOAD_RADIUS_CHUNKS, UNLOAD_RADIUS_CHUNKS);

        this.spawnPosition = calculateSpawnPosition(0, 0);

        if (playerSave != null) {
            this.camera = new FpsCamera(playerSave.x(), playerSave.y(), playerSave.z());
            this.camera.setRotation(playerSave.yaw(), playerSave.pitch());
            this.selectedBlockIndex = clamp(playerSave.selectedBlockIndex(), 0, HOTBAR_BLOCKS.length - 1);
        } else {
            this.camera = new FpsCamera(spawnPosition.x, spawnPosition.y, spawnPosition.z);
            this.selectedBlockIndex = 0;
        }

        this.player = new PlayerController(camera);
        if (playerSave != null) {
            this.player.setMode(playerSave.mode());
        }

        this.worldRenderer = new VoxelWorldRenderer();
        this.worldRenderer.init(context.window().width(), context.window().height());

        this.uiRenderer = new UiRenderer();
        this.raycaster = new VoxelRaycaster();
    }

    @Override
    public void handleFrameInput(double frameDeltaSeconds) {
        elapsedTimeSeconds += frameDeltaSeconds;

        if (input.wasKeyPressed(GLFW_KEY_ESCAPE)) {
            context.window().requestClose();
        }

        if (input.wasKeyPressed(GLFW_KEY_F1)) {
            input.setCursorCaptured(!input.isCursorCaptured());
        }

        if (input.wasKeyPressed(GLFW_KEY_G)) {
            player.toggleMode();
        }

        if (input.wasKeyPressed(GLFW_KEY_R)) {
            camera.setPosition(spawnPosition);
            if (player.mode() == GameMode.SURVIVAL) {
                player.setMode(GameMode.SURVIVAL);
            }
        }

        if (input.wasKeyPressed(GLFW_KEY_F5)) {
            saveNow();
        }

        updateHotbarSelectionByScroll();

        if (input.isCursorCaptured()) {
            camera.applyMouseLook(input.mouseDeltaX(), input.mouseDeltaY());
        }

        if (input.wasMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            breakTargetBlock();
        }

        if (input.wasMousePressed(GLFW_MOUSE_BUTTON_RIGHT)) {
            placeSelectedBlock();
        }
    }

    @Override
    public void fixedUpdate(double fixedDeltaSeconds) {
        player.update(input, world, (float) fixedDeltaSeconds);
        world.updateStreaming(camera.position(), LOAD_RADIUS_CHUNKS, UNLOAD_RADIUS_CHUNKS);

        autosaveTimerSeconds += fixedDeltaSeconds;
        if (autosaveTimerSeconds >= AUTOSAVE_INTERVAL_SECONDS) {
            autosaveTimerSeconds = 0.0;
            saveNow();
        }
    }

    @Override
    public void render(float alpha) {
        if (context.window().consumeResizeFlag()) {
            worldRenderer.resize(context.window().width(), context.window().height());
        }

        worldRenderer.render(world, camera, elapsedTimeSeconds);
        uiRenderer.render(context.window().width(), context.window().height(), HOTBAR_BLOCKS, selectedBlockIndex, player.mode());
    }

    @Override
    public void dispose() {
        saveNow();

        if (uiRenderer != null) {
            uiRenderer.close();
        }
        if (worldRenderer != null) {
            worldRenderer.close();
        }
    }

    private void breakTargetBlock() {
        RaycastHit hit = raycaster.raycast(world, camera.position(), camera.front(), INTERACT_DISTANCE);
        if (hit == null || hit.blockY() <= 0) {
            return;
        }

        byte block = world.getBlock(hit.blockX(), hit.blockY(), hit.blockZ());
        if (block != BlockType.AIR.id()) {
            world.setBlock(hit.blockX(), hit.blockY(), hit.blockZ(), BlockType.AIR.id());
        }
    }

    private void placeSelectedBlock() {
        RaycastHit hit = raycaster.raycast(world, camera.position(), camera.front(), INTERACT_DISTANCE);
        if (hit == null) {
            return;
        }

        int tx = hit.blockX() + hit.normalX();
        int ty = hit.blockY() + hit.normalY();
        int tz = hit.blockZ() + hit.normalZ();

        if (ty <= 0 || ty >= Chunk.SIZE_Y) {
            return;
        }

        if (world.getBlock(tx, ty, tz) != BlockType.AIR.id()) {
            return;
        }

        if (player.intersectsBlockAabb(tx, ty, tz)) {
            return;
        }

        world.setBlock(tx, ty, tz, HOTBAR_BLOCKS[selectedBlockIndex].id());
    }

    private Vector3f calculateSpawnPosition(int worldX, int worldZ) {
        int y = Chunk.SIZE_Y - 2;
        while (y > 1 && world.getBlock(worldX, y, worldZ) == BlockType.AIR.id()) {
            y--;
        }
        return new Vector3f(worldX + 0.5f, y + 2.7f, worldZ + 0.5f);
    }

    private void updateHotbarSelectionByScroll() {
        double scroll = input.scrollDeltaY();
        if (scroll == 0.0) {
            return;
        }

        if (scroll > 0.0) {
            selectedBlockIndex = (selectedBlockIndex + 1) % HOTBAR_BLOCKS.length;
        } else {
            selectedBlockIndex--;
            if (selectedBlockIndex < 0) {
                selectedBlockIndex = HOTBAR_BLOCKS.length - 1;
            }
        }
    }

    private void saveNow() {
        if (saveService == null || world == null || camera == null || player == null) {
            return;
        }

        PlayerSaveData playerSave = new PlayerSaveData(
                camera.position().x,
                camera.position().y,
                camera.position().z,
                camera.yaw(),
                camera.pitch(),
                player.mode(),
                selectedBlockIndex);

        saveService.save(world.seed(), playerSave, world.snapshotModifications());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
