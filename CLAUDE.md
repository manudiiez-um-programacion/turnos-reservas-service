# CLAUDE.md

Guía para Claude Code al trabajar en este repositorio.

## Proyecto

`turnos-reservas-service` — trabajo final de **Programación 2** (Universidad de Mendoza).
Servicio de gestión de turnos y reservas.

**Estado actual: sin código.** El repo contiene únicamente `README.md` y `.github/CODEOWNERS`.
No hay stack, build ni dependencias definidas todavía. Antes de asumir lenguaje,
framework o estructura de carpetas, preguntar o confirmar con el usuario.

## Estructura

```
.
├── README.md            # Título + badge de evaluación de calidad (bloque autogenerado)
├── .github/CODEOWNERS   # Owner: @dqmdz-um
└── CLAUDE.md
```

## Evaluación de calidad

El `README.md` incluye un bloque delimitado por los comentarios
`<!-- calidad:inicio -->` y `<!-- calidad:fin -->` con badges de **Calidad (7 dimensiones)**,
**Aprobar** y **Destacar**. Ese bloque lo genera la herramienta de evaluación de la cátedra:
**no editarlo a mano**. Cualquier contenido propio del README va debajo de `calidad:fin`.

## Convenciones

- **Idioma**: documentación, comentarios y mensajes de commit en español.
- **Commits**: estilo convencional, como en el historial existente (`docs:`, `feat:`, `fix:`).
- **Rama principal**: `main`. Trabajar en ramas aparte y abrir PR en lugar de commitear
  directo a `main`.
- Commitear o pushear solo cuando el usuario lo pida explícitamente.

## Al agregar código

Cuando se defina el stack e implemente el servicio, actualizar este archivo con:
comandos de build/test/lint, cómo correr el servicio localmente, arquitectura y
modelo de dominio (turnos, reservas, disponibilidad).
