# JavaCraft - README inicial

En este primer avance construí un prototipo de juego tipo Minecraft usando Java puro, sin librerías externas. Mi objetivo en esta fase fue validar una base jugable sobre la que pueda seguir iterando.

## Lo que hice

- Implementé una ventana de juego con bucle principal (`update` + `render`) a 60 FPS.
- Creé un mundo de bloques (voxels) con generación procedural de terreno.
- Programé una cámara en primera persona con movimiento y rotación.
- Añadí interacción con bloques mediante raycast: romper y colocar bloques.
- Separé el código en clases base para mantener una estructura clara (`Game`, `World`, `Renderer`, `Camera`, `Input`, etc.).

## Controles actuales

- `W A S D`: moverme
- `SPACE` / `SHIFT`: subir y bajar
- Flechas: mirar
- Click izquierdo: romper bloque
- Click derecho: colocar bloque

## Cómo ejecutar

```bash
./run.sh
```

## Requisitos

- Java instalado (`java` y `javac`)
- Recomendado: Java 21

## Estado del proyecto

Este repositorio corresponde al primer hito. Ya tengo una base funcional y el siguiente paso es evolucionarlo con físicas, chunks e inventario.
