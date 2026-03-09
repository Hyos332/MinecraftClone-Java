# JavaCraft - Fase 1 (Motor Base en Java + LWJGL)

En esta fase reestructuré el proyecto como base técnica real para un clon de Minecraft hecho en Java. Dejé atrás el prototipo AWT y migré a una arquitectura de juego con GLFW/OpenGL vía LWJGL.

## Alcance de Fase 1

- Motor base con `game loop` profesional:
  - `handleFrameInput` por frame
  - `fixedUpdate` desacoplado (UPS fijo)
  - `render` separado
- Ventana y contexto OpenGL con `GLFW`.
- Input manager unificado (teclado, ratón, scroll, captura de cursor).
- Cámara FPS con mouse look y movimiento libre.
- Renderer OpenGL inicial con:
  - shaders reales (vertex/fragment)
  - malla de depuración (grid + ejes)
  - proyección perspectiva + matriz de vista
- Build reproducible con Gradle Wrapper (`./gradlew`).

## Stack técnico

- Java 21
- Gradle 8.10.2 (wrapper)
- LWJGL 3 (GLFW, OpenGL, OpenAL, STB)
- JOML (matemática 3D)

## Estructura actual

- `com.minecraftclone.bootstrap`: entrada de aplicación
- `com.minecraftclone.engine`: loop, ventana, configuración y ciclo de vida
- `com.minecraftclone.input`: manejo de input
- `com.minecraftclone.camera`: cámara en primera persona
- `com.minecraftclone.render`: shader, malla y renderer base
- `com.minecraftclone.game`: escena/juego de fase 1
- `com.minecraftclone.resource`: carga de recursos

## Controles

- `W A S D`: movimiento horizontal
- `SPACE`: subir
- `LEFT SHIFT`: bajar
- `LEFT CTRL`: sprint
- Ratón: rotar cámara
- `F1`: capturar/liberar cursor
- `R`: reiniciar posición de cámara
- `ESC`: salir

## Ejecución

```bash
./run.sh
```

O directamente:

```bash
GRADLE_USER_HOME=.gradle-home ./gradlew run
```

## Estado para próxima fase

La base está lista para comenzar Fase 2: mundo voxel/chunks, bloques, meshing y raycasting sobre datos reales de mundo.
