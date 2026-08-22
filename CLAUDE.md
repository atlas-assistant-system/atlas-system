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
- [docs/economy-context.md](docs/economy-context.md) — diseño del contexto `economy`; el ciclo 1 (movimientos y saldo) ya está implementado, los ciclos 2 y 3 siguen esbozados
- [docs/nutrition-context.md](docs/nutrition-context.md) — diseño del contexto `nutrition`; el ciclo 1 (plan y consumo diario) está completo y cableado, y el ciclo 2 (peso y evolución) se implementó y se retiró
- [docs/training-context.md](docs/training-context.md) — diseño del contexto `training`; el ciclo 1 (catálogo, plantillas y entrenos) está completo y cableado, el ciclo 2 (progresión) está diseñado sin implementar

Estos documentos son la fuente de verdad técnica del proyecto. Cualquier decisión arquitectónica nueva debe reflejarse ahí.

## Los bounded contexts

El proyecto es **un único módulo JPMS** y cada bounded context es un subpaquete dentro de
cada anillo. La convención de `architecture.md` aplicada a varios contextos:

```
atlas/domain/sharedkernel/          appointments/  presence/  routines/  economy/  nutrition/  training/
atlas/application/sharedkernel/     appointments/  presence/  routines/  economy/  nutrition/  training/
atlas/infrastructure/sharedkernel/  appointments/  presence/  routines/  economy/  nutrition/  training/
atlas/presentation/sharedkernel/    appointments/  presence/  routines/  economy/  nutrition/  training/
atlas/app/appointments/  presence/  routines/  economy/  nutrition/  training/   cableado de cada contexto
atlas/app/Application.java                                composition root que los monta
```

**Lo que está en `common` es genérico de verdad, no un cajón compartido.** `Json`,
`StaticResources` y `SessionGuard` sí lo son — `SessionGuard` solo conoce un `BooleanSupplier` y
un 401, y lo usan `appointments` y `economy`. `Values`, `UiHandlers` y `DocsHandlers` **no**: sus
métodos y sus recursos son los de un contexto concreto (`floats` para descriptores faciales,
`weekdays` para rutinas, `amount` en euros para movimientos), así que viven en
`presentation/<contexto>/web/`. Fusionarlos era un cambio de comportamiento silencioso: en
`presence` un campo ausente lanza 400 y en `routines` devuelve `null`.

Cada contexto conserva su propia base de datos en `data/` —el aislamiento entre contextos es
físico— y su propio bus de comandos y consultas.

## Estado actual

- **`core` compuesto** — es dueño de `/` y de la interfaz espejo. Tras autenticarse ofrece
  `Inicio`, `Agenda`, `Rutinas`, `Economía`, `Nutrición` y `Entrenamiento` como pestañas de una
  sola aplicación;
  consume los
  módulos por sus APIs públicas, sin introducir dependencias entre sus dominios.
- **`appointments` migrado** — citas, recordatorios y calendario. Se monta en `/appointments`,
  conserva sus rutas auxiliares de recordatorios y documentación, y su SSE va en `/events`. Su
  API está detrás de la guardia de sesión de `presence`.
- **`presence` migrado** — identidad biométrica facial con prueba de vida e interacción por
  gesto. El desafío sigue siendo el `FIST` fijo: primero calibra la pose neutral, comprueba el
  gesto sin bloquear la percepción facial y después agrega las tres mejores capturas frontales.
  `real` y `live` son telemetría, no una puerta por frame; la calidad operativa exige 340 px y
  rechaza manos superpuestas al rostro. El onboarding guarda cinco poses como plantillas del
  mismo perfil y permite añadir variantes posteriores (por ejemplo, con o sin gafas). Se monta
  en sus propios prefijos (`/profiles`, `/sessions`, `/authentication`...) y su SSE en
  `/events/presence`; `/presence/sandbox` mide por separado acción, calidad, señales pasivas,
  oclusión y recuperación.
- **`routines` migrado** — hábitos como cuota dentro de un periodo. Cuatro capas completas, se
  monta en `/routines` y su SSE en `/events/routines`.
- **`economy` implementado (ciclo 1)** — movimientos, saldo y desglose por categoría. Cuatro
  capas completas, se monta en `/economy` tras la guardia de sesión de `presence`, y su SSE en
  `/events/economy`. **El espejo no inventa lo que viene de fuera, pero sí captura lo que nace en
  él**: los movimientos son el reflejo del banco y solo entran por HTTP (un Atajo de iOS, un
  script, curl), mientras que presupuestos y objetivos de ahorro no existen en ninguna otra parte
  —los decides mirando el propio desglose— y se fijan desde la vista. En ambos casos la vista se
  refresca sola oyendo los eventos. El importe viaja como
  cadena en euros (`"12.50"`) y se guarda en céntimos con signo, sin columna de tipo: `kind` se
  deriva al leer con `MovementKind.of(...)`, que es la única definición del signo en el sistema.
  Los ciclos 2 (presupuesto) y 3 (objetivos de ahorro) siguen sin empezar.
- **`nutrition` implementado (ciclo 1)** — plan de nutrición y consumo diario. Es **Fitia sin
  alimentos**: no hay base de datos nutricional ni códigos de barras, se teclean los macros. Cuatro
  capas completas, se monta en `/nutrition` tras la guardia de sesión de `presence`, y su SSE en
  `/events/nutrition`. **Aquí sí se escribe desde el espejo**, al revés que en `economy`: nada de
  esto nace fuera —tú decides las calorías—, así que la pestaña tiene formularios y la API queda
  abierta igualmente para un Atajo de iOS. **Las calorías se teclean, no se derivan**
  —ni en el consumo ni en el plan—: lo que sabes de lo que comes es la cifra de la etiqueta, no el
  desglose, y el alcohol no es ninguno de los tres macros. Pueden discrepar de los macros y nadie
  los concilia: mandan las tecleadas. El objetivo sí sigue derivado, de
  `Goal.of(pesoInicial, pesoObjetivo)`, y no es un campo elegible. Solo hay un plan activo a la
  vez, y quien lo sostiene es un índice parcial de SQLite, no solo el handler.
  El **ciclo 2** —`WeighIn`, el progreso contra el plan y la gráfica de peso— se implementó y
  **se retiró**: la tabla la tira `V011__drop_weigh_ins.sql` y `V006` se queda, porque una
  migración aplicada no se edita. Del peso solo sobreviven el de partida y el objetivo del plan,
  que son de donde sale `Goal`.
- **`training` implementado (ciclo 1)** — catálogo de ejercicios, plantillas y entrenos. Es
  **Strong sin catálogo de ejercicios**: los tecleas tú. Cuatro capas completas, se monta en
  `/training` tras la guardia de sesión de `presence`, y su SSE en `/events/training`.
  **El cumplimiento no está aquí, está en `routines`**: "ir al gimnasio 4 veces por semana"
  ya es una `Routine` con `Schedule.over(WEEK)` y `Target(4, "sesiones")`, con su racha.
  `training` responde *qué hiciste y con cuánto peso*; `routines`, *si lo hiciste*. Los dos
  contextos no se conocen.
  **Una serie se mide de cuatro formas y solo cuatro** —carga en gramos, reps, segundos,
  metros— en un `Effort` plano, y `Metric` (smart enum: `LOAD`, `REPS`, `TIME`, `DISTANCE`)
  es la **única** definición de qué compara la mejor marca y qué suma el volumen, para que
  no puedan divergir. La métrica de un ejercicio es inmutable: cambiarla con historial
  detrás cambiaría el significado de lo registrado, así que se archiva y se crea otro.
  **El historial no se reescribe**: al empezar un entreno, las series previstas se copian y
  se congelan en `SetLog.planned`, así que editar la plantilla después no toca junio. Un
  `SetLog` lleva `planned` y `actual` opcionales y al menos uno presente: sin `actual` es
  el guion pendiente, sin `planned` es una serie fuera de guion.
  **No hay máquina de estados del entreno** —nada que cerrar— y si entrenas mañana y tarde
  son dos logs del mismo día. Ejercicios y plantillas se archivan, nunca se borran, y el
  nombre único de un ejercicio sobrevive al archivado para que el histórico siga uniendo.
  El **ciclo 2** (progresión: mejor marca, histórico por ejercicio y volumen) está diseñado
  en `docs/training-context.md` y sin implementar; no añade dominio, son dos consultas
  sobre `Metric`.
- **1796 tests en verde**, incluidos los de integración contra SQLite real y las reglas de
  ArchUnit.
- **Mutation testing con PIT** sobre `atlas.domain.*` (excluido el kernel), umbral del 90%: hoy
  el dominio está al 95%, `economy` al 97%, `nutrition` entre el 94% y el 100% y `training`
  al 96%. No cuelga de `check` porque son ~30 s — se lanza a
  mano con `gradle pitest`. Ojo con las versiones: el plugin 1.15.0 no vale con Gradle 9 y PIT
  1.19.4 no lee bytecode de Java 25.
- **El Shared Kernel es un contexto más**, repartido por sus anillos igual que los demás
  (`atlas.domain.sharedkernel`, `atlas.application.sharedkernel`...). Ya no es una dependencia
  externa ni un subproyecto: el build no necesita nada publicado a mano, y `mavenLocal` no
  interviene. Las reglas de ArchUnit lo excluyen por el patrón `..sharedkernel..`, y viven en
  `src/test/java/atlas/architecture/rules/` — no pueden ser un subproyecto porque importan
  `ValueObject` del propio kernel y se formaría un ciclo.
- **La puerta de sesión ya es única** — `appointments`, `economy`, `routines`, `nutrition`,
  `training` y `presence` devuelven 401 sin sesión. Los cinco primeros comparten el `SessionGuard` de
  `presentation/common/web/` y consultan la sesión de `presence` en memoria mediante un contrato
  booleano cableado en el composition root; no hay HTTP interno ni rutas proxy entre contextos.
  Cada `wire(...)` conserva una sobrecarga sin guardia (`() -> true`) para montar el contexto
  suelto en sus tests. **Los cinco SSE (`/events`, `/events/routines`, `/events/economy`,
  `/events/nutrition`, `/events/training`) van tras la misma guardia**, y como esta solo mira en el handshake mientras
  el stream dura mucho más, el composition root se suscribe a `SessionClosedEvent` y
  `SessionExpiredEvent` de `presence` para cerrar los hubs de los cinco contextos al cerrarse la
  sesión. El de `presence` no se toca: es el
  que la vista necesita para enterarse.
- Las UIs independientes de `appointments` y `routines` se retiraron: la presentación de ambos
  módulos vive ahora en `core`; `routines` conserva su documentación en `/routines/docs`.
- **El JS compartido vive en `web-presence/` y se sirve bajo `/presence/`**: `face-quality.js`
  (umbrales y calidad facial), `gestures.js` (geometría de la mano, umbrales del reconocedor y
  el detector del desafío) y `enrollment.js` (las cinco poses y el bucle de captura). Los tres
  son módulos IIFE puros que cada página engancha desestructurando; `enrollment.bind` recibe de
  quien lo usa el `faceStatus` y el acceso a la pose neutral. **El sandbox mide este mismo
  código, no una copia** — antes tenía su propia versión de la geometría y ya había derivado.
- **Los mensajes de error que ve la persona están en un único sitio**:
  `web-shared/error-messages.js`, servido por `ErrorMessagesHandler` a `/assets/errors.js` (core)
  y `/errors.js` (presence). El `message` del catálogo de dominio está en inglés y es para
  depurar: ningún JS debe mostrarlo. `ErrorMessagesTest` compara los códigos literales de los
  `*Errors.java` con las claves del fichero y falla si alguno se queda sin traducir.

## Notas de trabajo

<!-- Espacio libre para que el desarrollo de este proyecto añada contexto propio: decisiones tomadas, deuda técnica conocida, próximos pasos, etc. -->
