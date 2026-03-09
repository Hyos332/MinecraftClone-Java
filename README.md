# JavaCraft - Fase 3 (Física + UI + Guardado)

Esta fase agrega la primera base de gameplay tipo sandbox/survival sobre el motor Java + LWJGL ya existente.

## Stack

- Java 21
- Gradle Wrapper 8.10.2
- LWJGL 3 (`GLFW`, `OpenGL`, `OpenAL`, `STB`)
- JOML

## Implementado en Fase 3

- Física del jugador con `AABB`:
  - gravedad
  - salto
  - colisiones por ejes X/Y/Z contra bloques sólidos
  - detección de suelo (`onGround`)
- Modos de juego:
  - `SURVIVAL` (física/colisiones)
  - `CREATIVE` (vuelo libre)
- UI 2D renderizada en OpenGL:
  - crosshair
  - hotbar visual de bloques seleccionables
  - indicador visual de modo de juego
- Persistencia de partida:
  - semilla del mundo
  - estado del jugador (posición, yaw, pitch, modo, slot activo)
  - bloques modificados por chunk
  - autosave periódico + guardado al salir + guardado manual

## Sistemas activos

- Mundo voxel por chunks (`16x96x16`)
- Generación procedural reproducible
- Meshing por chunk (solo caras visibles)
- Render voxel con shaders
- Raycasting DDA para romper/colocar
- Carga/descarga dinámica de chunks

## Controles

- `W A S D`: mover
- Ratón: mirar
- `SPACE`: saltar (`SURVIVAL`) / subir (`CREATIVE`)
- `LEFT SHIFT`: bajar (`CREATIVE`)
- `LEFT CTRL`: sprint
- Scroll: cambiar bloque activo de hotbar
- Click izquierdo: romper bloque
- Click derecho: colocar bloque
- `G`: alternar `SURVIVAL` / `CREATIVE`
- `F1`: capturar/liberar cursor
- `F5`: guardar partida manual
- `R`: respawn en spawn local
- `ESC`: salir

## Ejecutar

```bash
./run.sh
```

## Guardado

La partida se guarda en:

```text
worlds/main/
```

Archivos principales:

- `world.dat`
- `player.dat`
- `chunks.dat`

(`worlds/` está ignorado por Git para no ensuciar commits.)

## Próxima fase recomendada

- Inventario real (`ItemStack`, slots, stack limits)
- Recolección de drops (`ItemEntity`)
- Crafteo básico (recetas shaped/shapeless)
- Iluminación propagada por bloque y luz solar
