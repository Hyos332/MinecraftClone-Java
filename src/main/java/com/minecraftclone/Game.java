package com.minecraftclone;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import javax.swing.JFrame;

public final class Game extends Canvas implements Runnable {
    private static final double FIXED_STEP = 1.0 / 60.0;

    private final Input input;
    private final World world;
    private final Camera camera;
    private final Renderer renderer;

    private JFrame frame;
    private Thread gameThread;
    private volatile boolean running;

    private int fps;
    private int frameCounter;
    private long fpsTimerStart;

    public Game(int width, int height) {
        setPreferredSize(new Dimension(width, height));

        this.input = new Input();
        this.world = new World(40, 18, 40, 20260306L);
        this.camera = new Camera(20.0, 12.0, 8.0);
        this.renderer = new Renderer();

        addKeyListener(input);
        addMouseListener(input);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;
        setupWindow();
        gameThread = new Thread(this, "javacraft-loop");
        gameThread.start();
    }

    private void setupWindow() {
        frame = new JFrame("JavaCraft - Prototipo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        requestFocus();
    }

    @Override
    public void run() {
        long previous = System.nanoTime();
        double accumulator = 0.0;
        fpsTimerStart = System.currentTimeMillis();

        while (running) {
            long now = System.nanoTime();
            double deltaSeconds = (now - previous) / 1_000_000_000.0;
            previous = now;
            accumulator += deltaSeconds;

            while (accumulator >= FIXED_STEP) {
                update(FIXED_STEP);
                accumulator -= FIXED_STEP;
            }

            render();

            frameCounter++;
            long elapsed = System.currentTimeMillis() - fpsTimerStart;
            if (elapsed >= 1000L) {
                fps = frameCounter;
                frameCounter = 0;
                fpsTimerStart += 1000L;
            }

            try {
                Thread.sleep(2L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }

    private void update(double dt) {
        camera.update(input, world, dt);

        if (input.consumeLeftClick()) {
            interact(false);
        }
        if (input.consumeRightClick()) {
            interact(true);
        }
    }

    private void interact(boolean place) {
        World.RayHit hit = world.raycast(camera.getPosition(), camera.getForwardVector(), 7.0, 0.04);
        if (hit == null) {
            return;
        }

        if (place) {
            int tx = hit.blockX() + hit.normalX();
            int ty = hit.blockY() + hit.normalY();
            int tz = hit.blockZ() + hit.normalZ();

            if (!world.inBounds(tx, ty, tz)) {
                return;
            }

            if (world.getBlock(tx, ty, tz) == BlockType.AIR) {
                world.setBlock(tx, ty, tz, BlockType.DIRT);
            }
            return;
        }

        if (hit.blockY() > 0) {
            world.setBlock(hit.blockX(), hit.blockY(), hit.blockZ(), BlockType.AIR);
        }
    }

    private void render() {
        BufferStrategy strategy = getBufferStrategy();
        if (strategy == null) {
            createBufferStrategy(3);
            return;
        }

        Graphics2D g = (Graphics2D) strategy.getDrawGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        renderer.render(g, getWidth(), getHeight(), world, camera);
        drawHud(g);

        g.dispose();
        strategy.show();
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawHud(Graphics2D g) {
        int width = getWidth();
        int height = getHeight();

        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(12, 12, 390, 88, 12, 12);

        g.setColor(Color.WHITE);
        Vec3 position = camera.getPosition();
        g.drawString("FPS: " + fps, 24, 36);
        g.drawString(String.format("Pos: x=%.2f y=%.2f z=%.2f", position.x(), position.y(), position.z()), 24, 56);
        g.drawString("WASD mover | SPACE/SHIFT subir-bajar", 24, 76);
        g.drawString("Flechas mirar | Click izq romper | Click der colocar", 24, 96);

        int cx = width / 2;
        int cy = height / 2;
        g.setStroke(new BasicStroke(2.0f));
        g.setColor(new Color(0, 0, 0, 150));
        g.drawLine(cx - 10, cy, cx + 10, cy);
        g.drawLine(cx, cy - 10, cx, cy + 10);
        g.setColor(new Color(255, 255, 255, 220));
        g.drawLine(cx - 8, cy, cx + 8, cy);
        g.drawLine(cx, cy - 8, cx, cy + 8);
    }
}
