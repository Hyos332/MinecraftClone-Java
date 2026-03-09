# JavaCraft - Fase 4 (Inventario + Drops + Pickup)

Esta fase convierte la base sandbox en un loop jugable más cercano a supervivencia: ahora hay inventario real de hotbar, drops de bloques como entidades y recolección al acercarse.

## Stack

- Java 21
- Gradle Wrapper 8.10.2
- LWJGL 3 (`GLFW`, `OpenGL`, `OpenAL`, `STB`)
- JOML

## Implementado en Fase 4

- Hotbar/inventario funcional (9 slots):
  - stacks por bloque (`ItemStack`) con límite (`64`)
  - selección con rueda y teclas `1..9`
  - consumo de ítems al colocar en `SURVIVAL`
- Entidades de ítem (`ItemEntity`):
  - spawn al romper bloques en `SURVIVAL`
  - física simple (gravedad/rebote/fricción)
  - tiempo de vida
  - pickup por proximidad
- Integración de render para entidades de ítem:
  - cubos pequeños renderizados encima del mundo
- UI mejorada de hotbar:
  - slots con contenido real del inventario
  - barra visual de cantidad por stack
- Guardado/carga ampliado:
  - estado de hotbar (bloque y cantidad por slot) en `player.dat`
  - compatibilidad con `player.dat` legacy de fase anterior

## Sistemas activos

- Mundo voxel por chunks (`16x96x16`)
- Generación procedural reproducible
- Meshing por chunk (caras visibles)
- Render voxel + entidades + UI
- Raycasting DDA para romper/colocar
- Física de jugador con colisiones AABB
- Carga/descarga dinámica de chunks
- Autosave periódico + guardado manual

## Controles

- `W A S D`: mover
- Ratón: mirar
- `SPACE`: saltar (`SURVIVAL`) / subir (`CREATIVE`)
- `LEFT SHIFT`: bajar (`CREATIVE`)
- `LEFT CTRL`: sprint
- Scroll: cambiar slot de hotbar
- `1..9`: seleccionar slot directo
- Click izquierdo: romper bloque
- Click derecho: colocar bloque
- `G`: alternar `SURVIVAL` / `CREATIVE`
- `F1`: capturar/liberar cursor
- `F5`: guardar manual
- `R`: volver a spawn local
- `ESC`: salir

## Ejecutar

```bash
./run.sh
```

## Guardado

Directorio de partida:

```text
worlds/main/
```

Archivos:

- `world.dat`
- `player.dat`
- `chunks.dat`

## Próxima fase recomendada

- Inventario completo (beyond hotbar) + pantalla de inventario
- `ItemEntity` optimizada (merge de drops cercanos)
- Crafteo básico (`shaped` / `shapeless`)
- Estructura base de `Entity`/`EntityManager` para mobs
