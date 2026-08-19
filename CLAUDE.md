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

## Estado actual

Proyecto en fase inicial de definición. Actualiza esta sección con el estado real (funcionalidades completas, en progreso, pendientes) a medida que avance el desarrollo.

## Notas de trabajo

<!-- Espacio libre para que el desarrollo de este proyecto añada contexto propio: decisiones tomadas, deuda técnica conocida, próximos pasos, etc. -->
