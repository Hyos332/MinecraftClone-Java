package com.minecraftclone.render.voxel;

import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
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
import static org.lwjgl.opengl.GL30.glDrawArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class ChunkGpuMesh implements AutoCloseable {
    private static final int FLOATS_PER_VERTEX = 6;
    private static final int STRIDE = FLOATS_PER_VERTEX * Float.BYTES;

    private final int vaoId;
    private final int vboId;

    private int vertexCount;

    public ChunkGpuMesh() {
        this.vaoId = glGenVertexArrays();
        this.vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE, 0L);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, STRIDE, (long) 3 * Float.BYTES);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void upload(float[] vertices, int vertexCount) {
        this.vertexCount = vertexCount;

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        try {
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        } finally {
            MemoryUtil.memFree(buffer);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void render() {
        if (vertexCount == 0) {
            return;
        }
        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }
}
