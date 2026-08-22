# Bounded context: `training`

Diseño cerrado del contexto de entrenamiento. Este documento es la fuente de verdad del
modelo, los casos de uso y las decisiones tomadas — el código se escribe a partir de aquí,
no al revés.

Estado: **ciclo 1 implementado y cableado**. El ciclo 2 (progresión) sigue diseñado sin implementar.

## Qué resuelve

Saber qué levantaste y si estás levantando más que antes. El espejo responde a tres
preguntas, en este orden de importancia:

1. ¿Qué toca hoy y cómo voy?
2. ¿Cuál es mi mejor marca en este ejercicio y cuándo la hice?
3. ¿Estoy moviendo más peso que la semana pasada?

## Decisiones de alcance

**El cumplimiento no es de aquí, es de `routines`.** "Ir al gimnasio 4 veces por semana" ya
es una `Routine` con `Schedule.over(WEEK)` y `Target(4, "sesiones")`, con su racha y su
progreso por periodo. "Hacer abdominales lunes, miércoles y viernes" ya es
`Schedule.daily({LU, MI, VI})`. Duplicar aquí `Schedule`, `Target` y `Streak` sería
mantener dos veces un código que ya funciona y que no tiene nada de entrenamiento.
`training` responde **qué hiciste y con cuánto peso**; `routines` responde **si lo
hiciste**. Los dos contextos no se conocen: quien mira las dos pestañas es la persona.

**Aquí sí se escribe desde el espejo**, como en `nutrition` y al revés que en `economy`.
Nada de esto nace fuera: los kilos que levantas no los publica ningún banco. Así que la
pestaña tiene formularios y la API queda abierta igualmente para un Atajo de iOS.

**Cero base de datos de ejercicios.** Es **Strong sin catálogo**, igual que `nutrition` es
"Fitia sin alimentos": no hay listado precargado de ejercicios, ni músculos, ni
ilustraciones, ni equipamiento. Tecleas "Press banca" una vez y ya está en tu catálogo.
Cargar un catálogo de terceros trae mantenimiento, idioma, licencia y una tabla de
sinónimos, y no responde a ninguna de las tres preguntas de arriba.

**El historial no se reescribe.** Al empezar un entreno, `WorkoutLog` se queda con una
**copia de las series previstas**. Editar la plantilla después no toca el pasado. Es la
misma familia de decisión que tomó `economy` al derivar `kind` del signo en vez de
guardarlo: la historia de un día se responde con la verdad de ese día, no con la de hoy.

**Sin 1RM estimado y sin prescripción.** El espejo no calcula un máximo teórico con Epley
ni te dice qué peso poner hoy. Registra y compara; no manda. Un 1RM es una estimación
disfrazada de medida, y "sube 2,5 kg" exige reglas de progresión por ejercicio y una
política de qué pasa al fallar una serie — un ciclo entero para una cifra que puedes
decidir tú mirando la anterior.

**Sin máquina de estados del entreno.** No hay `EN_CURSO / TERMINADO / DESCARTADO`. Un
`WorkoutLog` existe, tiene fecha y se le añaden o corrigen series; no hay nada que cerrar.
Es lo primero que pediría un módulo así y no aporta nada que no dé ya la fecha. Si entrenas
mañana y tarde, son dos logs del mismo día y no hace falta decidir cuál está "abierto".

**Sin gráficas.** La curva de evolución de peso de `nutrition` se implementó y se retiró.
La progresión aquí se lee como cifras y como lista —"hoy 70×8 · mejor 72,5×6 el 14 de
julio"—, que es lo que se ve a la distancia a la que se mira un espejo.

## Los ciclos

El contexto tiene tres agregados, pero a diferencia de `economy` **los tres son el mismo
ciclo**: una plantilla sin ejercicios no existe, y un entreno sin plantilla ni catálogo no
se puede registrar. Lo que sí se puede separar es la progresión, que es puramente de
lectura sobre datos que ya están.

| Ciclo | Alcance | Estado |
|---|---|---|
| 1 | `Exercise`, `Workout`, `WorkoutLog` — registrar y consultar | **implementado** |
| 2 | Progresión — mejor marca, histórico por ejercicio y volumen | diseñado abajo, sin implementar |
| 3 | Descanso entre series, notas por entreno, reordenar plantilla | esbozado al final |

Los ciclos 1 y 2 se especifican enteros aquí porque el 2 no añade dominio nuevo: son dos
consultas y un smart enum que ya existe en el 1. Cada uno es su propia entrega.

---

# Ciclo 1 — Catálogo, plantillas y entrenos

## El modelo de dominio

```
domain/training/
    Exercise.java          (AR, prefijo 'E')   qué se entrena
    Workout.java           (AR, prefijo 'W')   la plantilla
    WorkoutLog.java        (AR, prefijo 'T')   lo que hiciste un día
    entities/PlannedExercise.java   (UUID)     una línea de la plantilla
    entities/SetLog.java            (UUID)     una serie del entreno
    enums/Metric.java
    vos/Effort.java
    vos/ExerciseName.java
    vos/WorkoutName.java
    vos/SetCount.java
    vos/PlannedLine.java            (ExerciseId, SetCount, Effort)   entra en setPlan
    vos/PlannedSet.java             (ExerciseId, Effort)             sale de expand()
    events/...
    ExerciseErrors.java · WorkoutErrors.java · WorkoutLogErrors.java · TrainingErrors.java
```

Prefijos libres comprobados: `E`, `W` y `T` no colisionan con `A P M O I N G B L S R`
([id-conventions.md](id-conventions.md)). Las entidades internas llevan `UUID`, como
`Reminder` en `appointments`.

### `Effort`

El corazón del contexto. Cuatro enteros planos, en la línea de `Macros(protein, carbs,
fat)` de `nutrition`.

```java
public record Effort(int loadGrams, int reps, int seconds, int meters) implements ValueObject
```

- `NONE` — las cuatro a cero.
- `create(...)` → `Result<Effort>`, falla si alguna es negativa.
- `isZero()`, `loadKilograms()`.
- `ofKilograms(BigDecimal)` para la carga, con el mismo truco de escala que
  `nutrition.Weight`: `"72.5"` → `72500`. Los discos de 2,5 kg existen y `int` de kilos los
  perdería.

Con cuatro medidas entra todo lo que se entrena: press banca es `70000g · 8 reps`,
dominadas son `0g · 12 reps`, una plancha es `45 s`, correr es `5000 m`, un salto al cajón
es `600 mm`. **No se guarda ninguna quinta medida ni un saco de campos libres**: lo que
hace posible la progresión es que dos series sean comparables, y un blob no se compara con
otro blob.

La carga va en **gramos**, no en kilos con decimales, por la misma razón que el dinero va
en céntimos: sumar `BigDecimal` en cada consulta es caro y redondear en coma flotante es
incorrecto.

### `Metric`

Smart enum con comportamiento ([enum-conventions.md](enum-conventions.md)), no una
etiqueta. Cada ejercicio declara el suyo y **es inmutable**: cambiar la métrica de un
ejercicio con historial cambiaría el significado de todo lo registrado, así que no hay
comando para eso — se archiva el ejercicio y se crea otro.

| Valor | Manda | Mejor marca | Volumen |
|---|---|---|---|
| `LOAD` | la carga | mayor `loadGrams` | `Σ loadGrams × reps` |
| `REPS` | las repeticiones | mayor `reps` | `Σ reps` |
| `TIME` | los segundos | mayor `seconds` | `Σ seconds` |
| `DISTANCE` | los metros | mayor `meters` | `Σ meters` |

```java
public abstract long scoreOf(Effort effort);    // qué compara la mejor marca
public abstract long volumeOf(Effort effort);   // qué suma el volumen
public abstract String label();
```

Persistencia por nombre, nunca por `ordinal()`.

Consecuencia aceptada: en `LOAD`, la mejor marca es **la mayor carga**, sin normalizar por
repeticiones. `80 kg × 5` gana a `70 kg × 8` aunque discutiblemente sea peor entreno. La
alternativa era el 1RM estimado, descartado arriba. Por eso la UI enseña siempre la carga
**con sus reps al lado**: el dato completo se ve, la comparación no se inventa.

Y el volumen **solo suma dentro de la misma métrica**. No se pueden sumar kilos con
segundos, así que "el volumen de la semana" son kilos de los ejercicios `LOAD`, y los de
`TIME` van por su cuenta.

### `ExerciseName`, `WorkoutName`, `SetCount`

`ExerciseName` y `WorkoutName` son `SingleValueObject<String>`, no en blanco, máximo 60
caracteres, recortados. `SetCount` es un `int` entre 1 y 20 — no por rigor fisiológico,
sino porque `4×8` tecleado como `48×8` es un error que conviene que no llegue a la tabla.

### `Exercise`

Raíz de agregado. Es el catálogo, y sobre todo **es la clave de unión de la progresión**:
sin él, "mi histórico de press banca" se resolvería juntando cadenas de texto y `"Press
banca"` no casaría con `"press de banca"`.

```java
Result<Exercise> define(ExerciseId id, ExerciseName name, Metric metric, Instant now)
Result<Void>     rename(ExerciseName newName, Instant now)
Result<Void>     archive(Instant now)
Result<Void>     unarchive(Instant now)
```

- La métrica se fija al definir y no se cambia nunca.
- **Los ejercicios se archivan, no se borran.** Un ejercicio archivado no aparece al montar
  plantillas nuevas, pero el histórico lo conserva. Borrarlo dejaría series huérfanas, que
  es exactamente perder el pasado al ordenar el presente.
- Nombre único: lo sostiene un índice único en SQLite, no solo el handler — misma decisión
  que el plan activo único de `nutrition`.

Eventos: `ExerciseDefinedEvent`, `ExerciseRenamedEvent`, `ExerciseArchivedEvent`,
`ExerciseUnarchivedEvent`.

### `Workout` y `PlannedExercise`

La plantilla: "Día de empuje". Raíz de agregado con sus líneas dentro como entidades
internas, alcanzables solo a través de métodos de la raíz
([ddd-conventions.md](ddd-conventions.md)).

```java
public final class PlannedExercise extends Entity<PlannedExerciseId> {
    private final ExerciseId exerciseId;
    private final int position;
    private SetCount sets;
    private Effort target;      // "8 reps @ 70 kg"
}
```

Una línea es `4 × (8 reps @ 70 kg)`, **no cuatro filas**: así es como se escribe una rutina
en papel y así se teclea una vez en lugar de cuatro.

Tres nombres parecidos que conviene no confundir: **`PlannedExercise`** es la entidad
interna que vive en el agregado; **`PlannedLine`** es el VO que *entra* por `setPlan`;
**`PlannedSet`** es el VO que *sale* de `expand()`, ya desplegado a una serie por unidad.
La entidad guarda estado y tiene identidad; los dos VOs son datos de paso entre capas.

```java
Result<Workout> define(WorkoutId id, WorkoutName name, Instant now)
Result<Void>    rename(WorkoutName newName, Instant now)
Result<Void>    setPlan(List<PlannedLine> lines, Instant now)   // reemplaza el plan entero
Result<Void>    archive(Instant now)
List<PlannedSet> expand()                                        // 4×8 → cuatro PlannedSet
```

**`setPlan` reemplaza la lista completa** en vez de tener `addExercise`, `removeExercise`,
`changeExercise` y `moveExercise` por separado. Son cuatro comandos, cuatro rutas y cuatro
casos de test para lo que en la pantalla es un único gesto: abres la plantilla, la editas y
la guardas. La posición sale del orden de la lista que llega, así que reordenar es gratis y
no necesita comando propio.

- Se permite repetir el mismo ejercicio en una plantilla: press banca al principio y al
  final del día es un patrón real.
- Un `Workout` archivado no admite cambios, como `Plan` en `nutrition`.
- `expand()` es lo que consume `WorkoutLog` al empezar: convierte `4 × (8 reps @ 70 kg)` en
  cuatro `PlannedSet(exerciseId, effort)`. Vive en el dominio, no en el handler, porque es
  la regla que traduce plan a ejecución.

Eventos: `WorkoutDefinedEvent`, `WorkoutRenamedEvent`, `WorkoutPlanChangedEvent`,
`WorkoutArchivedEvent`.

### `WorkoutLog` y `SetLog`

Lo que de verdad hiciste un día.

```java
public final class SetLog extends Entity<SetLogId> {
    private final ExerciseId exerciseId;
    private final int position;
    private final Optional<Effort> planned;   // congelado al empezar; vacío en serie extra
    private Optional<Effort> actual;          // vacío mientras está pendiente
}
```

Las dos son opcionales y **al menos una tiene que estar**: sin plan ni ejecución, la serie
no es nada. Una serie recién expandida de la plantilla tiene `planned` y no `actual` — es
el guion pendiente. Una serie extra fuera del guion tiene `actual` y no `planned`.

Que `actual` sea opcional es lo que hace natural la pantalla: el entreno se abre con las
series previstas listadas y las vas rellenando conforme salen. Y es también lo que permite
la corrección honesta que querías: la cuarta serie se te cae a 6 reps y queda `planned = 8
reps @ 70`, `actual = 6 reps @ 70`.

```java
Result<WorkoutLog> start(WorkoutLogId id, Optional<WorkoutId> workoutId,
                         List<PlannedSet> plan, LocalDate performedOn,
                         LocalDate today, Instant now)
Result<Void> recordSet(SetLogId setId, Effort actual, Instant now)
Result<Void> addSet(SetLogId setId, ExerciseId exerciseId, Effort actual, Instant now)
Result<Void> removeSet(SetLogId setId, Instant now)
Result<Void> discard(Instant now)
```

- **No se fecha en el futuro**, igual que `Intake` en `nutrition`.
- Una serie con las cuatro medidas a cero no es una serie — el eco directo de "un consumo
  de cero calorías no es un consumo".
- `workoutId` es opcional: un entreno libre, sin plantilla, es válido y se registra
  añadiendo series sueltas.
- **No se guarda copia del nombre de la plantilla ni del ejercicio.** Solo se congela
  `planned`. Como plantillas y ejercicios se archivan y nunca se borran, la unión por ID
  siempre resuelve, y así corregir un nombre mal escrito arregla también el pasado —que es
  lo que quieres de un typo, al revés que de un cambio de plan.

Eventos: `WorkoutStartedEvent`, `SetRecordedEvent`, `SetRemovedEvent`,
`WorkoutLogDiscardedEvent`.

## Capa de aplicación

Trece comandos y seis consultas, con carpeta por caso de uso
([cqrs-conventions.md](cqrs-conventions.md)). Las otras dos consultas —las de progresión—
son del ciclo 2.

```
application/training/
    commands/defineexercise/      commands/renameexercise/
    commands/archiveexercise/     commands/unarchiveexercise/
    commands/defineworkout/       commands/renameworkout/
    commands/setworkoutplan/      commands/archiveworkout/
    commands/startworkoutlog/     commands/recordset/
    commands/addset/              commands/removeset/
    commands/discardworkoutlog/
    queries/listexercises/        queries/listworkouts/
    queries/getworkout/           queries/listworkoutlogs/
    queries/getworkoutlog/        queries/gettodayworkout/
    dto/                          ExerciseDto, WorkoutDto, PlannedExerciseDto,
                                  WorkoutLogDto, SetLogDto, EffortDto
    mappers/TrainingMapper.java
    ports/ExerciseRepository.java     ports/ExerciseReadModel.java
    ports/WorkoutRepository.java      ports/WorkoutReadModel.java
    ports/WorkoutLogRepository.java   ports/WorkoutLogReadModel.java
    ports/TrainingUnitOfWork.java     ports/IdGenerator.java
```

`StartWorkoutLogCommandHandler` es el único que toca dos agregados: lee el `Workout`, llama
a `expand()` y pasa el resultado a `WorkoutLog.start(...)`. Todo dentro de la misma unidad
de trabajo, y la traducción de plan a series vive en el dominio, no en el handler.

El generador de IDs de entidad interna es `UUID.randomUUID()` a través de un puerto
`IdGenerator`, para que los tests puedan fijarlo — misma necesidad que ya resuelve `Clock`
en el resto de handlers.

`EffortDto` viaja con la carga **en kilos como cadena** (`"72.5"`), igual que el importe de
`economy` viaja como `"12.50"`: la conversión a gramos es del borde, no de quien consume la
API.

## API HTTP

Montada en `/training` desde `TrainingApplication.wire(...)`, detrás de la guardia de
sesión de `presence` como el resto.

| Método | Ruta | Caso de uso |
|---|---|---|
| `POST` | `/training/exercises` | definir ejercicio |
| `GET` | `/training/exercises` | catálogo (`archived`) |
| `PUT` | `/training/exercises/{id}` | renombrar |
| `DELETE` | `/training/exercises/{id}` | archivar |
| `POST` | `/training/exercises/{id}/restore` | desarchivar |
| `POST` | `/training/workouts` | definir plantilla |
| `GET` | `/training/workouts` | listar plantillas |
| `GET` | `/training/workouts/{id}` | detalle con su plan |
| `PUT` | `/training/workouts/{id}` | renombrar |
| `PUT` | `/training/workouts/{id}/plan` | fijar el plan entero |
| `DELETE` | `/training/workouts/{id}` | archivar |
| `POST` | `/training/logs` | empezar un entreno |
| `GET` | `/training/logs` | histórico (`from`, `to`, `limit`) |
| `GET` | `/training/logs/{id}` | detalle con sus series |
| `DELETE` | `/training/logs/{id}` | descartar |
| `POST` | `/training/logs/{id}/sets` | serie extra |
| `PUT` | `/training/logs/{id}/sets/{setId}` | registrar o corregir serie |
| `DELETE` | `/training/logs/{id}/sets/{setId}` | quitar serie |
| `GET` | `/training/today` | el entreno de hoy |
| `GET` | `/training/docs` | Swagger UI |
| `GET` | `/training/openapi.json` | spec |

Errores traducidos con la tabla de [error-conventions.md](error-conventions.md). Los
mensajes que ve la persona van a `web-shared/error-messages.js`, no al `message` del
catálogo, que está en inglés y es para depurar — `ErrorMessagesTest` falla si algún código
nuevo se queda sin traducir.

**SSE en `/events/training`**, con un `TrainingEventsBroadcaster` suscrito a los eventos de
los tres agregados. El composition root lo añade a los hubs que se cierran con
`SessionClosedEvent` y `SessionExpiredEvent`, como los otros cuatro.

## Infraestructura

Base de datos propia en `data/training.db` — el aislamiento entre contextos es físico
([repository-conventions.md](repository-conventions.md)).

```
db-migrations/training/
    V001__create_exercises.sql          V002__index_exercises_by_name.sql
    V003__create_workouts.sql           V004__create_workout_exercises.sql
    V005__create_workout_logs.sql       V006__create_set_logs.sql
    V007__index_set_logs_by_exercise.sql
    V008__index_workout_logs_by_date.sql
    V009__create_sequences.sql
```

```sql
CREATE TABLE exercises (
    id       INTEGER PRIMARY KEY,
    name     TEXT    NOT NULL,
    metric   TEXT    NOT NULL,          -- por nombre, nunca ordinal
    archived INTEGER NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX ux_exercises_name ON exercises (name);

CREATE TABLE workouts (
    id       INTEGER PRIMARY KEY,
    name     TEXT    NOT NULL,
    archived INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE workout_exercises (
    id          TEXT    PRIMARY KEY,    -- UUID, entidad interna
    workout_id  INTEGER NOT NULL REFERENCES workouts (id),
    exercise_id INTEGER NOT NULL REFERENCES exercises (id),
    position    INTEGER NOT NULL,
    sets        INTEGER NOT NULL,
    load_g      INTEGER NOT NULL DEFAULT 0,
    reps        INTEGER NOT NULL DEFAULT 0,
    seconds     INTEGER NOT NULL DEFAULT 0,
    meters      INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE workout_logs (
    id           INTEGER PRIMARY KEY,
    workout_id   INTEGER REFERENCES workouts (id),   -- NULL = entreno libre
    performed_on TEXT    NOT NULL,                   -- ISO-8601, se ordena como texto
    started_at   TEXT    NOT NULL
);

CREATE TABLE set_logs (
    id              TEXT    PRIMARY KEY,   -- UUID
    log_id          INTEGER NOT NULL REFERENCES workout_logs (id),
    exercise_id     INTEGER NOT NULL REFERENCES exercises (id),
    position        INTEGER NOT NULL,
    planned_load_g  INTEGER, planned_reps INTEGER,
    planned_seconds INTEGER, planned_meters INTEGER,
    load_g          INTEGER, reps INTEGER, seconds INTEGER, meters INTEGER
);
```

**Las cuatro columnas de un `Effort` van juntas: o las cuatro con valor o las cuatro
`NULL`.** `NULL` significa "esta serie no tiene plan" o "esta serie está pendiente", que es
justo la opcionalidad de `SetLog`. No hay columna de bandera aparte porque permitiría que
bandera y valores discrepasen, y entonces habría que decidir cuál gana — el mismo
razonamiento por el que `economy` no guarda `kind`.

Las entidades internas se persisten como en `appointments`: al actualizar la raíz se borran
sus hijos y se reinsertan. Es una transacción sobre una tabla local con decenas de filas;
la alternativa —diffear la colección— es más código para ahorrar una escritura que no duele.

Los índices son los de las consultas reales: `set_logs (exercise_id)` para la progresión y
`workout_logs (performed_on)` para el histórico y el entreno de hoy.

## UI espejo

Nueva pestaña **Entrenamiento**, en `src/main/resources/web-core/training.{css,js}`, en el
mismo lenguaje visual que el resto: fondo negro, sin cajas, tipografía en `rem`, jerarquía
por tamaño y opacidad. Como se aprendió en `nutrition`, **la pestaña tiene que caber sin
scroll**.

- **El entreno de hoy** como protagonista: la lista de series previstas, cada una con su
  carga y reps, y se rellenan tocándolas. Las hechas se apagan; la siguiente pendiente
  destaca.
- **Debajo de cada ejercicio, su referencia**: "la última vez, 67,5 × 8". Es la única cifra
  del ciclo 2 que aparece durante el entreno, y es la que hace que no tengas que recordar
  nada.
- **Selector de plantilla** cuando no hay entreno abierto hoy, más un "entreno libre".
- **Catálogo y plantillas** en una vista secundaria: se tocan poco y no compiten por el
  sitio con lo de hoy.

La vista se refresca sola oyendo `/events/training`, así que registrar una serie desde el
móvil se ve en el espejo sin recargar.

## Cableado

`TrainingApplication.wire(renderer, databaseDirectory, clock, hasActiveSession)`, con la
sobrecarga sin guardia (`() -> true`) para montarlo suelto en sus tests, igual que los
demás. `Application` lo monta en `/training`, su SSE en `/events/training`, y lo añade a
`closeContextStreams()`.

## Testing

Siguiendo [testing-conventions.md](testing-conventions.md):

- **Dominio**: `EffortTest`, `MetricTest` (paramétrico sobre las cuatro métricas, marca y
  volumen), `ExerciseTest`, `WorkoutTest` (incluido `expand()`), `WorkoutLogTest`, y los
  `*IdTest` de los tres prefijos.
- **Aplicación**: un test por grupo de handlers con repositorios en memoria y `Clock` fijo.
- **Integración SQLite**: `SqliteExercisePersistenceIT`, `SqliteWorkoutPersistenceIT`
  (incluido el borrado y reinserción de las líneas del plan) y `SqliteWorkoutLogPersistenceIT`.
- **`HttpApiIT`**: el recorrido completo — definir dos ejercicios, montar una plantilla,
  empezar un entreno, registrar series, corregir una, añadir una extra, y comprobar que
  editar la plantilla **no** cambia el `planned` del entreno ya hecho. Esa última es la
  prueba de la decisión de alcance más importante del contexto.
- **ArchUnit** cubre el contexto sin cambios: las reglas van por paquete.
- **PIT** sobre `atlas.domain.training.*` con el umbral del 90% del proyecto.

---

# Ciclo 2 — Progresión

No añade dominio: `Metric` ya sabe puntuar y sumar. Son dos consultas y su lectura.

```
queries/getexerciseprogress/   ExerciseProgressQuery + Handler
queries/getvolume/             GetVolumeQuery + Handler
ports/ExercisePerformance.java     (exerciseId, best, bestOn, last, lastOn)
ports/VolumePoint.java             (periodo, metric, total)
dto/ExerciseProgressDto.java       dto/VolumePointDto.java
```

| Método | Ruta | Caso de uso |
|---|---|---|
| `GET` | `/training/progress/{exerciseId}` | mejor marca, última vez e histórico |
| `GET` | `/training/volume` | volumen por periodo (`from`, `to`, `period`) |

**La agregación se hace en Java, no en SQL**, al revés que en `economy`. La razón es que la
regla de qué puntúa y qué suma vive en `Metric`, y reproducirla como un `CASE` en SQL sería
tener la misma decisión escrita en dos sitios que pueden divergir en silencio. El coste es
leer las series del ejercicio pedido en lugar de un `SUM`.

*Techo conocido: un año de entrenamiento duro son unas 15.000 series, que caben de sobra en
memoria. Si algún día son cientos de miles, la agregación baja a SQL y `Metric` gana un
`volumeExpression()` que genere el `CASE` desde el mismo sitio.*

Solo cuentan las series con `actual` presente: el guion pendiente no es progreso.

---

# Ciclo 3 — Esbozo

- **Descanso entre series**: cronómetro y descanso objetivo por línea de plantilla. Es la
  primera cosa que se echa de menos entrenando, y no toca nada de lo de arriba: una columna
  más en `workout_exercises` y un temporizador en la vista.
- **Nota por entreno**: "dormí fatal", "cambié a mancuernas". Un `Optional<Note>` en
  `WorkoutLog`, como el de `Intake`.
- **Reordenar la plantilla arrastrando**: hoy el orden se cambia guardando la lista en otro
  orden, que funciona pero es tosco en una pantalla táctil.

# Descartado a propósito

- **Cumplimiento, calendario y rachas** — es `routines`, ya funciona. Ver arriba.
- **1RM estimado y sugerencia de carga** — el espejo registra, no manda. Ver arriba.
- **Base de datos de ejercicios, músculos y equipamiento** — mantenimiento e idioma a
  cambio de nada que responda a las tres preguntas.
- **Gráficas de progresión** — la de peso de `nutrition` se implementó y se retiró.
- **Superseries, dropsets y RPE** — cada uno es un concepto nuevo en `SetLog` y una regla
  nueva en `Metric`. Si hacen falta, entran cuando duela no tenerlos.
- **Importar de Strong, Hevy o Garmin** — sin credenciales de terceros ni formatos ajenos,
  igual que `economy` no habla con el banco. Si algún día hace falta, es un adaptador sobre
  los mismos comandos y el dominio no se entera.
- **Planes periodizados** (mesociclos, semanas de descarga) — exige un calendario de
  plantillas y una noción de "semana N del bloque". Es un contexto entero, no una columna.


---

# Lo que cambió al implementarlo

Cosas que el diseño no había previsto y que se decidieron con el código delante:

- **`TrainingRequests` es un único fichero**, no seis records de request. Los seis cuerpos
  que acepta el contexto son variaciones de las mismas cuatro medidas; separarlos era más
  ficheros para el mismo trabajo.
- **El id de las entidades internas entra como `Supplier`/`IdGenerator`**, no como una lista
  paralela. `WorkoutLog.start` recibe un `Supplier<SetLogId>` porque despliega N series de
  golpe, y casar una lista de ids con una de series a mano habría creado una invariante de
  tamaño que nadie vigila.
- **El límite de carga se comprueba antes de escalar el `BigDecimal`**. Un
  `{"load":"1E+999999999"}` materializaría mil millones de dígitos en el `setScale` y nunca
  llegaría al guard. Es el mismo fallo que sigue teniendo `nutrition.Weight.ofKilograms`.
- **El nombre único de un ejercicio sobrevive al archivado**, y eso es deliberado: el índice
  de `exercises.name` no distingue archivados, así que archivar y volver a crear con el mismo
  nombre da 409. Tiene su test en `HttpApiIT`.
- **`SqliteWorkoutLogReadModel` lee las series en dos consultas y las une en memoria**, en vez
  de una consulta por entreno. Un mes son decenas de cabeceras y cientos de series.
- **`Effort.plus` se descartó**: no lo usaba nadie. El volumen suma `long` a través de
  `Metric.volumeOf`, no `Effort`s.
