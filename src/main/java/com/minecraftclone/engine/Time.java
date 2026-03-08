package com.minecraftclone.engine;

public final class Time {
    public double nowSeconds() {
        return System.nanoTime() * 1.0e-9;
    }
}
