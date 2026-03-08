package com.minecraftclone.render;

public final class DebugGeometryFactory {
    private DebugGeometryFactory() {
    }

    public static float[] createGridWithAxes(int halfExtent, float spacing) {
        VertexBuilder vertices = new VertexBuilder(12_000);

        for (int i = -halfExtent; i <= halfExtent; i++) {
            float c = (i % 8 == 0) ? 0.45f : 0.30f;
            float x = i * spacing;
            float z = i * spacing;

            addLine(vertices, x, 0.0f, -halfExtent * spacing, x, 0.0f, halfExtent * spacing, c, c, c);
            addLine(vertices, -halfExtent * spacing, 0.0f, z, halfExtent * spacing, 0.0f, z, c, c, c);
        }

        addLine(vertices, -halfExtent * spacing, 0.01f, 0.0f, halfExtent * spacing, 0.01f, 0.0f, 0.95f, 0.20f, 0.20f);
        addLine(vertices, 0.0f, 0.01f, -halfExtent * spacing, 0.0f, 0.01f, halfExtent * spacing, 0.20f, 0.35f, 0.95f);
        addLine(vertices, 0.0f, 0.0f, 0.0f, 0.0f, halfExtent * spacing * 0.5f, 0.0f, 0.25f, 0.95f, 0.25f);

        return vertices.toArray();
    }

    private static void addLine(
            VertexBuilder out,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float r,
            float g,
            float b) {
        out.push(x0, y0, z0, r, g, b);
        out.push(x1, y1, z1, r, g, b);
    }

    private static final class VertexBuilder {
        private float[] data;
        private int size;

        private VertexBuilder(int initialCapacity) {
            this.data = new float[Math.max(initialCapacity, 64)];
        }

        private void push(float x, float y, float z, float r, float g, float b) {
            ensureCapacity(size + 6);
            data[size++] = x;
            data[size++] = y;
            data[size++] = z;
            data[size++] = r;
            data[size++] = g;
            data[size++] = b;
        }

        private void ensureCapacity(int required) {
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

        private float[] toArray() {
            float[] result = new float[size];
            System.arraycopy(data, 0, result, 0, size);
            return result;
        }
    }
}
