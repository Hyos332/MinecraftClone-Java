package com.minecraftclone;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Renderer {
    private static final double FOV_RADIANS = Math.toRadians(75.0);
    private static final double NEAR_PLANE = 0.05;
    private static final double FAR_PLANE = 48.0;

    private static final Color SKY_TOP = new Color(102, 179, 255);
    private static final Color SKY_BOTTOM = new Color(188, 224, 255);
    private static final Color FOG_COLOR = new Color(182, 212, 242);

    private static final Vec3 LIGHT_DIRECTION = new Vec3(0.35, 1.0, 0.25).normalized();

    private static final int[] FACE_NX = {0, 0, -1, 1, 0, 0};
    private static final int[] FACE_NY = {0, 0, 0, 0, -1, 1};
    private static final int[] FACE_NZ = {-1, 1, 0, 0, 0, 0};

    private static final double[][][] FACE_VERTICES = {
            {{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}},
            {{1, 0, 1}, {0, 0, 1}, {0, 1, 1}, {1, 1, 1}},
            {{0, 0, 1}, {0, 0, 0}, {0, 1, 0}, {0, 1, 1}},
            {{1, 0, 0}, {1, 0, 1}, {1, 1, 1}, {1, 1, 0}},
            {{0, 0, 1}, {1, 0, 1}, {1, 0, 0}, {0, 0, 0}},
            {{0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}}
    };

    public void render(Graphics2D g, int width, int height, World world, Camera camera) {
        paintBackground(g, width, height);

        double focalLength = width / (2.0 * Math.tan(FOV_RADIANS / 2.0));
        Vec3 cameraPos = camera.getPosition();

        List<Face> faces = new ArrayList<>(8_192);

        for (int x = 0; x < world.getSizeX(); x++) {
            for (int y = 0; y < world.getSizeY(); y++) {
                for (int z = 0; z < world.getSizeZ(); z++) {
                    byte block = world.getBlock(x, y, z);
                    if (block == BlockType.AIR) {
                        continue;
                    }

                    for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
                        int nx = FACE_NX[faceIndex];
                        int ny = FACE_NY[faceIndex];
                        int nz = FACE_NZ[faceIndex];

                        if (world.isSolid(x + nx, y + ny, z + nz)) {
                            continue;
                        }

                        Vec3 normal = new Vec3(nx, ny, nz);
                        Vec3 faceCenter = new Vec3(
                                x + 0.5 + nx * 0.5,
                                y + 0.5 + ny * 0.5,
                                z + 0.5 + nz * 0.5);

                        Vec3 toCamera = cameraPos.subtract(faceCenter);
                        if (normal.dot(toCamera) <= 0.0) {
                            continue;
                        }

                        int[] xs = new int[4];
                        int[] ys = new int[4];
                        double depth = 0.0;
                        boolean clipped = false;

                        for (int i = 0; i < 4; i++) {
                            double wx = x + FACE_VERTICES[faceIndex][i][0];
                            double wy = y + FACE_VERTICES[faceIndex][i][1];
                            double wz = z + FACE_VERTICES[faceIndex][i][2];

                            Vec3 cameraSpace = camera.worldToCamera(new Vec3(wx, wy, wz));

                            if (cameraSpace.z() <= NEAR_PLANE || cameraSpace.z() >= FAR_PLANE) {
                                clipped = true;
                                break;
                            }

                            double projected = focalLength / cameraSpace.z();
                            xs[i] = (int) Math.round(width * 0.5 + cameraSpace.x() * projected);
                            ys[i] = (int) Math.round(height * 0.5 - cameraSpace.y() * projected);

                            depth += cameraSpace.z();
                        }

                        if (clipped || isOutsideScreen(xs, ys, width, height)) {
                            continue;
                        }

                        double avgDepth = depth / 4.0;
                        Color faceColor = shadeColor(blockColor(block), normal, avgDepth);
                        faces.add(new Face(xs, ys, avgDepth, faceColor));
                    }
                }
            }
        }

        faces.sort(Comparator.comparingDouble(Face::depth).reversed());

        for (Face face : faces) {
            g.setColor(face.color());
            g.fillPolygon(face.xs(), face.ys(), 4);

            g.setColor(new Color(0, 0, 0, 55));
            g.drawPolygon(face.xs(), face.ys(), 4);
        }
    }

    private void paintBackground(Graphics2D g, int width, int height) {
        g.setPaint(new GradientPaint(0, 0, SKY_TOP, 0, height, SKY_BOTTOM));
        g.fillRect(0, 0, width, height);

        g.setColor(new Color(106, 138, 84, 80));
        g.fillRect(0, height / 2, width, height / 2);
    }

    private Color shadeColor(Color base, Vec3 normal, double depth) {
        double light = 0.35 + 0.65 * Math.max(0.0, normal.dot(LIGHT_DIRECTION));
        double fog = clamp((depth - 10.0) / (FAR_PLANE - 10.0), 0.0, 1.0);

        int r = (int) Math.round(base.getRed() * light * (1.0 - fog) + FOG_COLOR.getRed() * fog);
        int g = (int) Math.round(base.getGreen() * light * (1.0 - fog) + FOG_COLOR.getGreen() * fog);
        int b = (int) Math.round(base.getBlue() * light * (1.0 - fog) + FOG_COLOR.getBlue() * fog);

        return new Color(clamp(r, 0, 255), clamp(g, 0, 255), clamp(b, 0, 255));
    }

    private Color blockColor(byte block) {
        return switch (block) {
            case BlockType.GRASS -> new Color(106, 170, 74);
            case BlockType.DIRT -> new Color(130, 95, 63);
            case BlockType.STONE -> new Color(138, 138, 148);
            default -> Color.BLACK;
        };
    }

    private static boolean isOutsideScreen(int[] xs, int[] ys, int width, int height) {
        boolean allLeft = true;
        boolean allRight = true;
        boolean allAbove = true;
        boolean allBelow = true;

        for (int i = 0; i < xs.length; i++) {
            if (xs[i] >= 0) {
                allLeft = false;
            }
            if (xs[i] < width) {
                allRight = false;
            }
            if (ys[i] >= 0) {
                allAbove = false;
            }
            if (ys[i] < height) {
                allBelow = false;
            }
        }

        return allLeft || allRight || allAbove || allBelow;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Face(int[] xs, int[] ys, double depth, Color color) {
    }
}
