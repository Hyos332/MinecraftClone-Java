package com.minecraftclone.game;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.camera.FpsCamera;
import com.minecraftclone.engine.EngineContext;
import com.minecraftclone.engine.GameLogic;
import com.minecraftclone.input.InputManager;
import com.minecraftclone.render.voxel.VoxelWorldRenderer;
import com.minecraftclone.world.Chunk;
import com.minecraftclone.world.VoxelWorld;
import com.minecraftclone.world.raycast.RaycastHit;
import com.minecraftclone.world.raycast.VoxelRaycaster;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public final class PhaseTwoGame implements GameLogic {
    private static final int LOAD_RADIUS_CHUNKS = 7;
    private static final int UNLOAD_RADIUS_CHUNKS = 10;
    private static final float INTERACT_DISTANCE = 6.5f;

    private static final BlockType[] PLACEABLE_BLOCKS = {
            BlockType.DIRT,
            BlockType.STONE,
            BlockType.WOOD,
            BlockType.SAND,
            BlockType.LEAVES
    };

    private EngineContext context;
    private InputManager input;

    private VoxelWorld world;
    private VoxelWorldRenderer renderer;
    private VoxelRaycaster raycaster;
    private FpsCamera camera;

    private Vector3f spawnPosition;

    private int selectedBlockIndex;
    private double elapsedTimeSeconds;

    @Override
    public void init(EngineContext context) {
        this.context = context;
        this.input = context.input();

        this.world = new VoxelWorld(2_026_030_9L);
        this.renderer = new VoxelWorldRenderer();
        this.raycaster = new VoxelRaycaster();

        Vector3f bootstrap = new Vector3f(0.5f, 48.0f, 0.5f);
        world.updateStreaming(bootstrap, LOAD_RADIUS_CHUNKS, UNLOAD_RADIUS_CHUNKS);

        this.spawnPosition = calculateSpawnPosition(0, 0);
        this.camera = new FpsCamera(spawnPosition.x, spawnPosition.y, spawnPosition.z);

        renderer.init(context.window().width(), context.window().height());
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

        if (input.wasKeyPressed(GLFW_KEY_R)) {
            camera = new FpsCamera(spawnPosition.x, spawnPosition.y, spawnPosition.z);
        }

        updateBlockSelectionByScroll();

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
        camera.updateMovement(input, (float) fixedDeltaSeconds);
        world.updateStreaming(camera.position(), LOAD_RADIUS_CHUNKS, UNLOAD_RADIUS_CHUNKS);
    }

    @Override
    public void render(float alpha) {
        if (context.window().consumeResizeFlag()) {
            renderer.resize(context.window().width(), context.window().height());
        }

        renderer.render(world, camera, elapsedTimeSeconds);
    }

    @Override
    public void dispose() {
        if (renderer != null) {
            renderer.close();
        }
    }

    private void breakTargetBlock() {
        RaycastHit hit = raycaster.raycast(world, camera.position(), camera.front(), INTERACT_DISTANCE);
        if (hit == null) {
            return;
        }
        if (hit.blockY() <= 0) {
            return;
        }

        byte block = world.getBlock(hit.blockX(), hit.blockY(), hit.blockZ());
        if (block == BlockType.AIR.id()) {
            return;
        }

        world.setBlock(hit.blockX(), hit.blockY(), hit.blockZ(), BlockType.AIR.id());
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

        if (intersectsPlayerAabb(tx, ty, tz)) {
            return;
        }

        BlockType selected = PLACEABLE_BLOCKS[selectedBlockIndex];
        world.setBlock(tx, ty, tz, selected.id());
    }

    private boolean intersectsPlayerAabb(int blockX, int blockY, int blockZ) {
        Vector3f p = camera.position();

        float playerMinX = p.x - 0.30f;
        float playerMaxX = p.x + 0.30f;
        float playerMinY = p.y - 1.62f;
        float playerMaxY = p.y + 0.18f;
        float playerMinZ = p.z - 0.30f;
        float playerMaxZ = p.z + 0.30f;

        float blockMinX = blockX;
        float blockMaxX = blockX + 1.0f;
        float blockMinY = blockY;
        float blockMaxY = blockY + 1.0f;
        float blockMinZ = blockZ;
        float blockMaxZ = blockZ + 1.0f;

        return playerMinX < blockMaxX && playerMaxX > blockMinX
                && playerMinY < blockMaxY && playerMaxY > blockMinY
                && playerMinZ < blockMaxZ && playerMaxZ > blockMinZ;
    }

    private Vector3f calculateSpawnPosition(int worldX, int worldZ) {
        int y = Chunk.SIZE_Y - 2;
        while (y > 1 && world.getBlock(worldX, y, worldZ) == BlockType.AIR.id()) {
            y--;
        }
        return new Vector3f(worldX + 0.5f, y + 2.7f, worldZ + 0.5f);
    }

    private void updateBlockSelectionByScroll() {
        double scroll = input.scrollDeltaY();
        if (scroll == 0.0) {
            return;
        }

        if (scroll > 0.0) {
            selectedBlockIndex = (selectedBlockIndex + 1) % PLACEABLE_BLOCKS.length;
            return;
        }

        selectedBlockIndex--;
        if (selectedBlockIndex < 0) {
            selectedBlockIndex = PLACEABLE_BLOCKS.length - 1;
        }
    }
}
