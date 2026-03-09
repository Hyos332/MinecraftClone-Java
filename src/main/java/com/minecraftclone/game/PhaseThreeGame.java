package com.minecraftclone.game;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.camera.FpsCamera;
import com.minecraftclone.engine.EngineContext;
import com.minecraftclone.engine.GameLogic;
import com.minecraftclone.entity.ItemEntityManager;
import com.minecraftclone.input.InputManager;
import com.minecraftclone.inventory.HotbarInventory;
import com.minecraftclone.player.GameMode;
import com.minecraftclone.player.PlayerController;
import com.minecraftclone.render.entity.ItemEntityRenderer;
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

import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
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

    private EngineContext context;
    private InputManager input;

    private WorldSaveService saveService;

    private VoxelWorld world;
    private VoxelWorldRenderer worldRenderer;
    private ItemEntityRenderer itemEntityRenderer;
    private UiRenderer uiRenderer;
    private VoxelRaycaster raycaster;

    private ItemEntityManager itemEntities;
    private HotbarInventory hotbar;

    private FpsCamera camera;
    private PlayerController player;

    private Vector3f spawnPosition;

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
        } else {
            this.camera = new FpsCamera(spawnPosition.x, spawnPosition.y, spawnPosition.z);
        }

        this.player = new PlayerController(camera);
        if (playerSave != null) {
            this.player.setMode(playerSave.mode());
        }

        this.hotbar = new HotbarInventory();
        if (playerSave != null) {
            hotbar.load(playerSave.hotbarBlockIds(), playerSave.hotbarCounts());
            hotbar.setSelectedIndex(clamp(playerSave.selectedBlockIndex(), 0, HotbarInventory.SLOT_COUNT - 1));
            if (isHotbarEmpty()) {
                hotbar.giveStarterKit();
            }
        } else {
            hotbar.giveStarterKit();
        }

        this.itemEntities = new ItemEntityManager();

        this.worldRenderer = new VoxelWorldRenderer();
        this.worldRenderer.init(context.window().width(), context.window().height());
        this.itemEntityRenderer = new ItemEntityRenderer(context.window().width(), context.window().height());

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

        updateHotbarSelectionByKeys();
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
        float dt = (float) fixedDeltaSeconds;

        player.update(input, world, dt);
        world.updateStreaming(camera.position(), LOAD_RADIUS_CHUNKS, UNLOAD_RADIUS_CHUNKS);
        itemEntities.update(world, hotbar, camera.position(), player.mode(), dt);

        autosaveTimerSeconds += fixedDeltaSeconds;
        if (autosaveTimerSeconds >= AUTOSAVE_INTERVAL_SECONDS) {
            autosaveTimerSeconds = 0.0;
            saveNow();
        }
    }

    @Override
    public void render(float alpha) {
        if (context.window().consumeResizeFlag()) {
            int width = context.window().width();
            int height = context.window().height();
            worldRenderer.resize(width, height);
            itemEntityRenderer.resize(width, height);
        }

        worldRenderer.render(world, camera, elapsedTimeSeconds);
        itemEntityRenderer.render(itemEntities.entities(), camera);
        uiRenderer.render(context.window().width(), context.window().height(), hotbar, player.mode());
    }

    @Override
    public void dispose() {
        saveNow();

        if (uiRenderer != null) {
            uiRenderer.close();
        }
        if (itemEntityRenderer != null) {
            itemEntityRenderer.close();
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

        byte blockId = world.getBlock(hit.blockX(), hit.blockY(), hit.blockZ());
        BlockType blockType = BlockType.fromId(blockId);
        if (blockType == BlockType.AIR) {
            return;
        }

        world.setBlock(hit.blockX(), hit.blockY(), hit.blockZ(), BlockType.AIR.id());

        if (player.mode() == GameMode.SURVIVAL && blockType != BlockType.WATER) {
            itemEntities.spawnDrop(blockType, 1, hit.blockX(), hit.blockY(), hit.blockZ());
        }
    }

    private void placeSelectedBlock() {
        RaycastHit hit = raycaster.raycast(world, camera.position(), camera.front(), INTERACT_DISTANCE);
        if (hit == null) {
            return;
        }

        BlockType selectedBlock = hotbar.selectedBlockType();
        if (selectedBlock == null) {
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

        if (player.mode() == GameMode.SURVIVAL && !hotbar.consumeSelected(1)) {
            return;
        }

        world.setBlock(tx, ty, tz, selectedBlock.id());
    }

    private Vector3f calculateSpawnPosition(int worldX, int worldZ) {
        int y = Chunk.SIZE_Y - 2;
        while (y > 1 && world.getBlock(worldX, y, worldZ) == BlockType.AIR.id()) {
            y--;
        }
        return new Vector3f(worldX + 0.5f, y + 2.7f, worldZ + 0.5f);
    }

    private void updateHotbarSelectionByKeys() {
        for (int key = GLFW_KEY_1; key <= GLFW_KEY_9; key++) {
            if (!input.wasKeyPressed(key)) {
                continue;
            }
            hotbar.setSelectedIndex(key - GLFW_KEY_1);
            return;
        }
    }

    private void updateHotbarSelectionByScroll() {
        double scroll = input.scrollDeltaY();
        if (scroll == 0.0) {
            return;
        }

        if (scroll > 0.0) {
            hotbar.selectNext();
        } else {
            hotbar.selectPrevious();
        }
    }

    private void saveNow() {
        if (saveService == null || world == null || camera == null || player == null || hotbar == null) {
            return;
        }

        Vector3f position = camera.position();

        PlayerSaveData playerSave = new PlayerSaveData(
                position.x,
                position.y,
                position.z,
                camera.yaw(),
                camera.pitch(),
                player.mode(),
                hotbar.selectedIndex(),
                hotbar.snapshotBlockIds(),
                hotbar.snapshotCounts());

        saveService.save(world.seed(), playerSave, world.snapshotModifications());
    }

    private boolean isHotbarEmpty() {
        for (int i = 0; i < HotbarInventory.SLOT_COUNT; i++) {
            if (!hotbar.slot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
