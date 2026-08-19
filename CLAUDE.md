# Atlas

Lo que necesitas ver, cuando levantas la vista

## Sobre este proyecto

Pantalla ambiental permanente: reloj, estado y avisos sobre fondo de camara en espejo, con vistas conmutables y control sin contacto.

Este documento (`CLAUDE.md`) es el punto de entrada de contexto para trabajar en este repositorio: qué es el proyecto, cómo está construido y qué convenciones sigue. Mantenlo actualizado a medida que el proyecto evolucione — es la referencia principal para cualquiera (humano o agente) que retome el trabajo.

## Arquitectura y stack técnico

La arquitectura y las decisiones de stack de este proyecto están documentadas en:

- [docs/architecture.md](docs/architecture.md) — capas, patrones y organización del código
- [docs/stack.md](docs/stack.md) — lenguajes, frameworks, librerías y herramientas
- [docs/ddd-conventions.md](docs/ddd-conventions.md) — patrón concreto de cada building block de DDD (Value Objects, Entities, Aggregate Roots, Domain Events...)
- [docs/cqrs-conventions.md](docs/cqrs-conventions.md) — Commands, Queries, sus handlers y cómo se despachan
- [docs/id-conventions.md](docs/id-conventions.md) — tipado de IDs (prefijo legible para aggregate roots, UUID para entidades internas)
- [docs/error-conventions.md](docs/error-conventions.md) — Error/Result, catálogo de errores, Guard clauses y su traducción a HTTP
- [docs/rich-domain-conventions.md](docs/rich-domain-conventions.md) — checklist y verificación automática para evitar un dominio anémico
- [docs/repository-conventions.md](docs/repository-conventions.md) — forma de los repositorios y consecuencias del aislamiento físico entre contextos
- [docs/enum-conventions.md](docs/enum-conventions.md) — smart enums: comportamiento, persistencia por nombre y cuándo usar enum vs Value Object
- [docs/mapping-conventions.md](docs/mapping-conventions.md) — cómo se traduce el dominio a DTOs, sin lógica de negocio ni acceso a datos
- [docs/testing-conventions.md](docs/testing-conventions.md) — naming, estructura de los tests, mocks, cobertura y mutation testing
- [docs/validation-specification-conventions.md](docs/validation-specification-conventions.md) — por qué no se adoptan Validator/Specification y qué se usa en su lugar
- [docs/conventions.md](docs/conventions.md) — convenciones de código, naming, testing y flujo de trabajo

Estos documentos son la fuente de verdad técnica del proyecto. Cualquier decisión arquitectónica nueva debe reflejarse ahí.

## Los bounded contexts

El proyecto es **un único módulo JPMS** y cada bounded context es un subpaquete dentro de
cada anillo. La convención de `architecture.md` aplicada a varios contextos:

```
atlas/domain/presence/        atlas/domain/routines/
atlas/application/presence/   atlas/application/routines/
atlas/infrastructure/presence/ atlas/infrastructure/routines/
atlas/presentation/presence/  atlas/presentation/routines/
atlas/app/presence/           atlas/app/routines/       cableado de cada contexto
atlas/app/Application.java                              composition root que los monta
```

**Lo que está en `common` es genérico de verdad, no un cajón compartido.** `Json` y
`StaticResources` sí lo son. `Values`, `UiHandlers` y `DocsHandlers` **no**: sus métodos y sus
recursos son los de un contexto concreto (`floats` para descriptores faciales, `weekdays` para
rutinas), así que viven en `presentation/<contexto>/web/`. Fusionarlos era un cambio de
comportamiento silencioso: en `presence` un campo ausente lanza 400 y en `routines` devuelve
`null`.

Cada contexto conserva su propia base de datos en `data/` —el aislamiento entre contextos es
físico— y su propio bus de comandos y consultas.

## Estado actual

- **`presence` migrado** — identidad biométrica facial con prueba de vida e interacción por
  gesto. Cuatro capas completas, se monta en `/` y su SSE en `/events`.
- **`routines` migrado** — hábitos como cuota dentro de un periodo. Cuatro capas completas, se
  monta en `/routines` y su SSE en `/events/routines`.
- **594 tests en verde**, incluidos los de integración contra SQLite real y las reglas de
  ArchUnit.
- **El Shared Kernel es un subproyecto** (`sharedkernel/`, `sharedkernel-archunit/`), no una
  dependencia de `mavenLocal`. El build ya no necesita nada publicado a mano.
- **Pendiente** — migrar el contexto `appointments` (citas, recordatorios y calendario), que
  es además quien trae la UI espejo y la cámara. Y unificar la puerta de sesión: hoy la API de
  `presence` devuelve 401 sin sesión y la de `routines` responde abierta.
- **Conocido** — la UI de `routines` no es alcanzable: montado en `/routines`, ese path lo
  ocupa su endpoint de listado. Se resolverá al componer las pestañas del espejo.

## Notas de trabajo

<!-- Espacio libre para que el desarrollo de este proyecto añada contexto propio: decisiones tomadas, deuda técnica conocida, próximos pasos, etc. -->
