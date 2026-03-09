package com.minecraftclone.render.voxel;

import com.minecraftclone.camera.FpsCamera;
import com.minecraftclone.resource.ResourceManager;
import com.minecraftclone.render.ShaderProgram;
import com.minecraftclone.world.Chunk;
import com.minecraftclone.world.VoxelWorld;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;

public final class VoxelWorldRenderer implements AutoCloseable {
    private static final float FOV_DEGREES = 75.0f;
    private static final float NEAR_PLANE = 0.05f;
    private static final float FAR_PLANE = 1200.0f;

    private final Map<Long, ChunkGpuMesh> gpuMeshes = new HashMap<>();
    private final ChunkMesher chunkMesher = new ChunkMesher();

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();

    private ShaderProgram shader;

    private int viewportWidth;
    private int viewportHeight;
    private int renderRadiusChunks = 8;

    public void init(int width, int height) {
        this.viewportWidth = Math.max(width, 1);
        this.viewportHeight = Math.max(height, 1);

        String vertexSource = ResourceManager.readText("/assets/shaders/voxel_world.vert");
        String fragmentSource = ResourceManager.readText("/assets/shaders/voxel_world.frag");
        shader = new ShaderProgram(vertexSource, fragmentSource);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_CULL_FACE);

        updateProjection();
    }

    public void resize(int width, int height) {
        this.viewportWidth = Math.max(width, 1);
        this.viewportHeight = Math.max(height, 1);
        updateProjection();
    }

    public void render(VoxelWorld world, FpsCamera camera, double worldTimeSeconds) {
        syncChunkMeshes(world);

        glViewport(0, 0, viewportWidth, viewportHeight);

        float dayWave = (float) ((Math.sin(worldTimeSeconds * 0.08) + 1.0) * 0.5);
        float skyR = 0.18f + dayWave * 0.18f;
        float skyG = 0.24f + dayWave * 0.30f;
        float skyB = 0.32f + dayWave * 0.40f;

        glClearColor(skyR, skyG, skyB, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.bind();
        shader.setUniform("uProjection", projection);
        shader.setUniform("uView", camera.getViewMatrix(view));

        Vector3f cameraPosition = camera.position();
        int cameraChunkX = floorDiv(fastFloor(cameraPosition.x), Chunk.SIZE_X);
        int cameraChunkZ = floorDiv(fastFloor(cameraPosition.z), Chunk.SIZE_Z);
        int renderRadiusSq = renderRadiusChunks * renderRadiusChunks;

        for (Chunk chunk : world.chunks()) {
            int dx = chunk.pos().x() - cameraChunkX;
            int dz = chunk.pos().z() - cameraChunkZ;
            if (dx * dx + dz * dz > renderRadiusSq) {
                continue;
            }

            ChunkGpuMesh mesh = gpuMeshes.get(chunk.pos().key());
            if (mesh != null) {
                mesh.render();
            }
        }

        shader.unbind();
    }

    @Override
    public void close() {
        for (ChunkGpuMesh mesh : gpuMeshes.values()) {
            mesh.close();
        }
        gpuMeshes.clear();

        if (shader != null) {
            shader.close();
            shader = null;
        }
    }

    private void syncChunkMeshes(VoxelWorld world) {
        Set<Long> aliveKeys = new HashSet<>(world.chunkCount() * 2 + 1);

        for (Chunk chunk : world.chunks()) {
            long key = chunk.pos().key();
            aliveKeys.add(key);

            ChunkGpuMesh mesh = gpuMeshes.get(key);
            if (mesh == null) {
                mesh = new ChunkGpuMesh();
                gpuMeshes.put(key, mesh);
                chunk.markDirty();
            }

            if (!chunk.isDirty()) {
                continue;
            }

            ChunkMeshData meshData = chunkMesher.build(chunk, world);
            mesh.upload(meshData.vertices(), meshData.vertexCount());
            chunk.clearDirty();
        }

        Iterator<Map.Entry<Long, ChunkGpuMesh>> iterator = gpuMeshes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ChunkGpuMesh> entry = iterator.next();
            if (aliveKeys.contains(entry.getKey())) {
                continue;
            }
            entry.getValue().close();
            iterator.remove();
        }
    }

    private void updateProjection() {
        float aspect = viewportWidth / (float) viewportHeight;
        projection.identity().perspective((float) Math.toRadians(FOV_DEGREES), aspect, NEAR_PLANE, FAR_PLANE);
    }

    private static int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static int floorDiv(int value, int divisor) {
        int q = value / divisor;
        int r = value % divisor;
        if (r != 0 && ((value ^ divisor) < 0)) {
            q--;
        }
        return q;
    }
}
