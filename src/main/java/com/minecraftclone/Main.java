package com.minecraftclone;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Game game = new Game(1280, 720);
        game.start();
    }
}
