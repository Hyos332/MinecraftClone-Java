# JavaCraft - Fase 2 (Mundo Voxel + Chunks en Java)

En esta fase implementé el primer vertical slice voxel real sobre la base de Fase 1. El juego ahora renderiza un mundo de bloques por chunks, con generación procedural, raycasting e interacción de construcción/destrucción.

## Stack técnico

- Java 21
- Gradle Wrapper 8.10.2
- LWJGL 3 (`GLFW`, `OpenGL`, `OpenAL`, `STB`)
- JOML

## Qué implementé en Fase 2

- Sistema de bloques (`BlockType`) con propiedades de render/sólido/transparencia.
- Mundo por chunks (`16x96x16`) con carga/descarga dinámica alrededor del jugador.
- Generación procedural reproducible por semilla:
  - altura base
  - variación continental/detalle
  - cuevas simples
  - minerales básicos (carbón/hierro)
  - árboles básicos
- Meshing por chunk con solo caras visibles.
- Render voxel OpenGL por chunk con `VAO/VBO` y shader propio.
- Raycasting DDA para apuntar bloques.
- Interacción:
  - click izquierdo: romper bloque
  - click derecho: colocar bloque en cara adyacente
  - validación para no colocar dentro del jugador
- Streaming de chunks en runtime (sin recargar todo el mundo).

## Controles actuales

- `W A S D`: mover
- `SPACE`: subir
- `LEFT SHIFT`: bajar
- `LEFT CTRL`: sprint
- Ratón: mirar
- Scroll: cambiar bloque a colocar
- Click izquierdo: romper bloque
- Click derecho: colocar bloque
- `F1`: capturar/liberar cursor
- `R`: reset de posición
- `ESC`: salir

## Ejecutar

```bash
./run.sh
```

`run.sh` usa por defecto `~/.gradle` para que la caché de Gradle no ensucie el repositorio.

## Estructura relevante

- `com.minecraftclone.engine`: loop, ventana y ciclo de vida
- `com.minecraftclone.camera`: cámara FPS
- `com.minecraftclone.input`: input manager
- `com.minecraftclone.block`: catálogo de bloques
- `com.minecraftclone.world`: mundo/chunks
- `com.minecraftclone.world.gen`: generación procedural
- `com.minecraftclone.world.raycast`: raycasting voxel
- `com.minecraftclone.render.voxel`: meshing + render de chunks
- `com.minecraftclone.game.PhaseTwoGame`: integración de gameplay fase 2

## Próximo objetivo (Fase 3)

- Colisiones/física sólida del jugador (AABB real)
- Inventario/hotbar funcional con `ItemStack`
- Persistencia de chunks modificados en disco
- UI sobre el mundo (crosshair/hotbar/selección)
