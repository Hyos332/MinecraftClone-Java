package com.minecraftclone.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ResourceManager {
    private ResourceManager() {
    }

    public static String readText(String path) {
        try (InputStream input = ResourceManager.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalArgumentException("Recurso no encontrado: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el recurso: " + path, e);
        }
    }
}
