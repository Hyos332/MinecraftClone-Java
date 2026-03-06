# JavaCraft - Primer hito técnico

En esta primera entrega desarrollé un prototipo jugable tipo Minecraft utilizando Java puro, sin librerías externas de render. El objetivo fue validar la arquitectura base del proyecto y dejar una plataforma sólida para iterar.

## Objetivo de esta fase

- Construir un núcleo funcional de juego (loop, render e input).
- Implementar un mundo de bloques con generación procedural.
- Habilitar navegación en primera persona e interacción básica con bloques.
- Definir una estructura de código mantenible para siguientes hitos.

## Alcance implementado

- Bucle de juego desacoplado en `update` y `render`.
- Renderizado 3D por software de caras visibles de voxels.
- Mundo generado proceduralmente a partir de ruido interpolado.
- Cámara en primera persona con movimiento y rotación.
- Raycast para romper y colocar bloques.
- HUD con información mínima de estado.

## Arquitectura actual

- `Main`: punto de entrada.
- `Game`: ciclo principal, orquestación de sistemas e interfaz.
- `World`: datos de bloques, generación procedural y raycast.
- `Renderer`: proyección y dibujado de geometría voxel.
- `Camera`: transformaciones de vista y control de navegación.
- `Input`: captura de teclado y ratón.
- `Vec3` y `BlockType`: utilidades base de dominio.

## Controles

- `W A S D`: movimiento horizontal
- `SPACE` / `SHIFT`: ascenso / descenso
- Flechas: rotación de cámara
- Click izquierdo: romper bloque
- Click derecho: colocar bloque

## Ejecución

```bash
./run.sh
```

## Requisitos

- `java` y `javac` disponibles en el sistema
- Recomendado: Java 21

## Limitaciones conocidas de este hito

- Sin físicas de supervivencia (gravedad, colisiones completas, salto).
- Sin sistema de chunks ni streaming de mundo.
- Sin inventario, crafting ni persistencia de guardado.

## Próximos hitos

- Integrar físicas básicas y colisiones robustas.
- Migrar mundo a chunks para escalar rendimiento.
- Implementar inventario/hotbar y ciclo de juego base.
