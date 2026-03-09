package com.minecraftclone.render.entity;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.camera.FpsCamera;
import com.minecraftclone.entity.ItemEntity;
import com.minecraftclone.render.ShaderProgram;
import com.minecraftclone.resource.ResourceManager;
import java.nio.FloatBuffer;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class ItemEntityRenderer implements AutoCloseable {
    private static final float FOV_DEGREES = 75.0f;
    private static final float NEAR_PLANE = 0.05f;
    private static final float FAR_PLANE = 1200.0f;

    private static final float[][][] FACE_VERTICES = {
            {{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}},
            {{1, 0, 1}, {0, 0, 1}, {0, 1, 1}, {1, 1, 1}},
            {{0, 0, 1}, {0, 0, 0}, {0, 1, 0}, {0, 1, 1}},
            {{1, 0, 0}, {1, 0, 1}, {1, 1, 1}, {1, 1, 0}},
            {{0, 0, 1}, {1, 0, 1}, {1, 0, 0}, {0, 0, 0}},
            {{0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}}
    };

    private static final float[] FACE_SHADE = {0.83f, 0.83f, 0.72f, 0.72f, 0.58f, 1.0f};
    private static final int[] TRI = {0, 2, 1, 0, 3, 2};

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();

    private final ShaderProgram shader;

    private final int vaoId;
    private final int vboId;

    private int viewportWidth;
    private int viewportHeight;

    public ItemEntityRenderer(int width, int height) {
        this.viewportWidth = Math.max(width, 1);
        this.viewportHeight = Math.max(height, 1);

        String vertex = ResourceManager.readText("/assets/shaders/voxel_world.vert");
        String fragment = ResourceManager.readText("/assets/shaders/voxel_world.frag");
        this.shader = new ShaderProgram(vertex, fragment);

        this.vaoId = glGenVertexArrays();
        this.vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        int stride = 6 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, (long) 3 * Float.BYTES);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        updateProjection();
    }

    public void resize(int width, int height) {
        this.viewportWidth = Math.max(width, 1);
        this.viewportHeight = Math.max(height, 1);
        updateProjection();
    }

    public void render(List<ItemEntity> entities, FpsCamera camera) {
        if (entities.isEmpty()) {
            return;
        }

        VertexBuilder builder = new VertexBuilder(entities.size() * 6 * 6 * 6);

        for (ItemEntity entity : entities) {
            appendEntityCube(builder, entity);
        }

        float[] vertices = builder.toArray();
        if (vertices.length == 0) {
            return;
        }

        glViewport(0, 0, viewportWidth, viewportHeight);
        glEnable(GL_CULL_FACE);

        shader.bind();
        shader.setUniform("uProjection", projection);
        shader.setUniform("uView", camera.getViewMatrix(view));

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        try {
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        } finally {
            MemoryUtil.memFree(buffer);
        }

        glDrawArrays(GL_TRIANGLES, 0, builder.vertexCount());

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        shader.unbind();
    }

    @Override
    public void close() {
        shader.close();
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }

    private void appendEntityCube(VertexBuilder out, ItemEntity entity) {
        BlockType type = entity.blockType();
        Vector3f position = entity.position();

        float bob = (float) Math.sin(entity.ageSeconds() * 5.0f) * 0.04f;
        float s = 0.20f;

        float minX = position.x - s;
        float minY = position.y - s + bob;
        float minZ = position.z - s;

        for (int faceIndex = 0; faceIndex < FACE_VERTICES.length; faceIndex++) {
            float shade = FACE_SHADE[faceIndex];
            float r = type.r() * shade;
            float g = type.g() * shade;
            float b = type.b() * shade;

            for (int triVertex : TRI) {
                float[] v = FACE_VERTICES[faceIndex][triVertex];
                out.push(
                        minX + v[0] * s * 2.0f,
                        minY + v[1] * s * 2.0f,
                        minZ + v[2] * s * 2.0f,
                        r,
                        g,
                        b);
            }
        }
    }

    private void updateProjection() {
        float aspect = viewportWidth / (float) viewportHeight;
        projection.identity().perspective((float) Math.toRadians(FOV_DEGREES), aspect, NEAR_PLANE, FAR_PLANE);
    }

    private static final class VertexBuilder {
        private float[] data;
        private int size;

        private VertexBuilder(int initialCapacity) {
            data = new float[Math.max(initialCapacity, 96)];
        }

        private void push(float x, float y, float z, float r, float g, float b) {
            ensure(size + 6);
            data[size++] = x;
            data[size++] = y;
            data[size++] = z;
            data[size++] = r;
            data[size++] = g;
            data[size++] = b;
        }

        private int vertexCount() {
            return size / 6;
        }

        private float[] toArray() {
            float[] out = new float[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }

        private void ensure(int required) {
            if (required <= data.length) {
                return;
            }
            int next = data.length;
            while (next < required) {
                next *= 2;
            }
            float[] grown = new float[next];
            System.arraycopy(data, 0, grown, 0, size);
            data = grown;
        }
    }
}
