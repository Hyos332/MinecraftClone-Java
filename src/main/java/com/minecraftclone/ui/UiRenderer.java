package com.minecraftclone.ui;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.inventory.HotbarInventory;
import com.minecraftclone.inventory.ItemStack;
import com.minecraftclone.player.GameMode;
import com.minecraftclone.render.ShaderProgram;
import com.minecraftclone.resource.ResourceManager;
import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
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

public final class UiRenderer implements AutoCloseable {
    private final ShaderProgram shader;

    private final int vaoId;
    private final int vboId;

    public UiRenderer() {
        String vertexSource = ResourceManager.readText("/assets/shaders/ui.vert");
        String fragmentSource = ResourceManager.readText("/assets/shaders/ui.frag");
        this.shader = new ShaderProgram(vertexSource, fragmentSource);

        this.vaoId = glGenVertexArrays();
        this.vboId = glGenBuffers();

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, vboId);

        int stride = 6 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L);

        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, stride, (long) 2 * Float.BYTES);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render(int width, int height, HotbarInventory hotbar, GameMode mode) {
        VertexBuilder builder = new VertexBuilder(4096);

        addCrosshair(builder, width, height);
        addHotbar(builder, width, height, hotbar, hotbar.selectedIndex());
        addModeIndicator(builder, width, height, mode);

        float[] vertices = builder.toArray();

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();

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

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void close() {
        shader.close();
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
    }

    private void addCrosshair(VertexBuilder out, int width, int height) {
        float cx = width * 0.5f;
        float cy = height * 0.5f;

        addRectPx(out, width, height, cx - 9.0f, cy - 1.0f, 18.0f, 2.0f, 0f, 0f, 0f, 0.55f);
        addRectPx(out, width, height, cx - 1.0f, cy - 9.0f, 2.0f, 18.0f, 0f, 0f, 0f, 0.55f);
        addRectPx(out, width, height, cx - 7.0f, cy, 14.0f, 1.0f, 1f, 1f, 1f, 0.90f);
        addRectPx(out, width, height, cx, cy - 7.0f, 1.0f, 14.0f, 1f, 1f, 1f, 0.90f);
    }

    private void addHotbar(VertexBuilder out, int width, int height, HotbarInventory hotbar, int selectedIndex) {
        int slots = HotbarInventory.SLOT_COUNT;
        float slotSize = 38.0f;
        float spacing = 6.0f;
        float barWidth = slots * slotSize + (slots - 1) * spacing;

        float startX = width * 0.5f - barWidth * 0.5f;
        float y = height - 64.0f;

        addRectPx(out, width, height, startX - 10.0f, y - 10.0f, barWidth + 20.0f, slotSize + 20.0f,
                0.05f, 0.05f, 0.06f, 0.48f);

        for (int i = 0; i < slots; i++) {
            float x = startX + i * (slotSize + spacing);
            boolean selected = i == selectedIndex;

            float border = selected ? 2.8f : 1.4f;
            float br = selected ? 1.0f : 0.75f;
            float bg = selected ? 1.0f : 0.75f;
            float bb = selected ? 0.88f : 0.75f;

            addRectPx(out, width, height, x - border, y - border, slotSize + border * 2.0f, slotSize + border * 2.0f,
                    br, bg, bb, selected ? 0.95f : 0.50f);

            addRectPx(out, width, height, x, y, slotSize, slotSize,
                    0.12f, 0.12f, 0.14f, 0.88f);

            ItemStack stack = hotbar.slot(i);
            BlockType block = stack.isEmpty() ? BlockType.AIR : stack.blockType();
            int count = stack.count();

            if (stack.isEmpty()) {
                addRectPx(out, width, height, x + 8.0f, y + 8.0f, slotSize - 16.0f, slotSize - 16.0f,
                        0.25f, 0.25f, 0.28f, 0.45f);
                continue;
            }

            addRectPx(out, width, height, x + 8.0f, y + 8.0f, slotSize - 16.0f, slotSize - 16.0f,
                    block.r(), block.g(), block.b(), 1.0f);

            float fill = clamp(count / (float) ItemStack.MAX_STACK_SIZE, 0.0f, 1.0f);
            addRectPx(out, width, height, x + 6.0f, y + slotSize - 6.0f, slotSize - 12.0f, 3.0f,
                    0.12f, 0.12f, 0.14f, 0.92f);
            addRectPx(out, width, height, x + 6.0f, y + slotSize - 6.0f, (slotSize - 12.0f) * fill, 3.0f,
                    0.94f, 0.96f, 0.98f, 0.95f);
        }
    }

    private void addModeIndicator(VertexBuilder out, int width, int height, GameMode mode) {
        float x = 16.0f;
        float y = 16.0f;
        addRectPx(out, width, height, x, y, 106.0f, 20.0f,
                0.03f, 0.03f, 0.04f, 0.65f);

        if (mode == GameMode.CREATIVE) {
            addRectPx(out, width, height, x + 4.0f, y + 4.0f, 98.0f, 12.0f,
                    0.21f, 0.65f, 0.92f, 0.92f);
        } else {
            addRectPx(out, width, height, x + 4.0f, y + 4.0f, 98.0f, 12.0f,
                    0.88f, 0.34f, 0.30f, 0.92f);
        }
    }

    private void addRectPx(VertexBuilder out, int viewportWidth, int viewportHeight,
                           float px, float py, float pw, float ph,
                           float r, float g, float b, float a) {
        float x0 = toNdcX(px, viewportWidth);
        float y0 = toNdcY(py, viewportHeight);
        float x1 = toNdcX(px + pw, viewportWidth);
        float y1 = toNdcY(py + ph, viewportHeight);

        out.push(x0, y0, r, g, b, a);
        out.push(x1, y0, r, g, b, a);
        out.push(x1, y1, r, g, b, a);

        out.push(x0, y0, r, g, b, a);
        out.push(x1, y1, r, g, b, a);
        out.push(x0, y1, r, g, b, a);
    }

    private static float toNdcX(float pixelX, int width) {
        return (pixelX / width) * 2.0f - 1.0f;
    }

    private static float toNdcY(float pixelY, int height) {
        return 1.0f - (pixelY / height) * 2.0f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class VertexBuilder {
        private float[] data;
        private int size;

        private VertexBuilder(int initialCapacity) {
            this.data = new float[Math.max(initialCapacity, 96)];
        }

        private void push(float x, float y, float r, float g, float b, float a) {
            ensure(size + 6);
            data[size++] = x;
            data[size++] = y;
            data[size++] = r;
            data[size++] = g;
            data[size++] = b;
            data[size++] = a;
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
