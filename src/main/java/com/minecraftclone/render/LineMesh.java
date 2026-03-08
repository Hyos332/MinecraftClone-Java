package com.minecraftclone.render;

import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_LINES;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glDrawArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class LineMesh implements AutoCloseable {
    private static final int FLOATS_PER_VERTEX = 6;
    private static final int STRIDE_BYTES = FLOATS_PER_VERTEX * Float.BYTES;

    private final int vaoId;
    private final int vboId;
    private final int vertexCount;

    public LineMesh(float[] vertices) {
        this.vertexCount = vertices.length / FLOATS_PER_VERTEX;
        this.vaoId = glGenVertexArrays();
        this.vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        try {
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(vertexBuffer);
        }

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, org.lwjgl.opengl.GL11.GL_FLOAT, false, STRIDE_BYTES, 0L);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, org.lwjgl.opengl.GL11.GL_FLOAT, false, STRIDE_BYTES, (long) 3 * Float.BYTES);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render() {
        glBindVertexArray(vaoId);
        glDrawArrays(GL_LINES, 0, vertexCount);
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }
}
