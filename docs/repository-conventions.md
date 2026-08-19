# Convenciones de repositorios y aislamiento de contextos

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

Este documento define la forma de los repositorios (puertos en `Application`,
implementaciones en `Infrastructure`, según `architecture.md`) y, sobre todo, las
consecuencias reales de la decisión ya tomada en `stack.md`: **SQLite, un fichero por
bounded context**. Extraído y adaptado — con una corrección deliberada, explicada más
abajo — de `IStudyRepository`/`StudyRepository`/`StudyContext` de GeneFlow.ApiNet2.

## Diferencia de partida con el proyecto de referencia

GeneFlow usa **una única base de datos PostgreSQL** con un *schema* distinto por
bounded context (`modelBuilder.HasDefaultSchema("studies")`) — aislamiento **lógico**,
por convención: nada impide técnicamente un JOIN entre `studies.*` y `orgs.*`, solo que
el equipo ha decidido no hacerlo.

Aquí el aislamiento es **físico**: cada bc tiene su propio fichero SQLite, su propia
conexión. No es que esté prohibido hacer un JOIN entre dos contextos — es que **no es
posible**, porque ni siquiera es la misma base de datos. Esto cambia varias cosas de
raíz respecto al patrón del proyecto de referencia.

## El repositorio: puerto estrecho, solo para el lado de escritura

Un repositorio persiste **un** Aggregate Root — nunca una entidad interna suelta, nunca
un value object. `IStudyRepository` en GeneFlow acumuló más de 20 métodos (paginación,
contadores para dashboards, soporte a *authorization behaviors*, un método para
reindexar) porque distintos consumidores fueron añadiendo lo que necesitaban al mismo
sitio. Aquí se evita eso a propósito: el repositorio solo tiene lo que un **Command**
necesita para cargar y persistir el agregado.

El Shared Kernel aporta `Repository<T, TId>` con las cinco operaciones básicas, y el
puerto del módulo lo extiende con lo que su agregado necesite de más:

```java
// application/appointments/ports/AppointmentRepository.java
public interface AppointmentRepository extends Repository<Appointment, AppointmentId> {
    AppointmentId nextId();
}
```

```java
// sharedkernel — las cinco operaciones genericas
public interface Repository<T extends AggregateRoot<TId>, TId> {
    Optional<T> get(TId id);
    List<T> getAll();
    void create(T aggregate);
    void update(T aggregate);
    void delete(T aggregate);
}
```

Dos detalles de esa firma que no son arbitrarios:

- **`delete` recibe el agregado, no el id.** Si borras por id no tienes el agregado, y
  sin agregado no hay `pendingEvents()` que publicar: un borrado nunca podría emitir su
  evento de dominio.
- **`create` y `update` están separados** en vez de un `save` que adivine. El caso de
  uso siempre sabe cuál es (acaba de pedir `nextId()`, o acaba de cargar el agregado), y
  la base comprueba que la sentencia afectó a alguna fila — un `update` sobre una fila
  inexistente falla en vez de no hacer nada en silencio.

> **Cuidado con `getAll()`.** Hidrata agregados **completos**, así que sirve para
> catálogos pequeños y para tests, no para pintar listados. Todo lo que sea lectura para
> pantalla va por el read model, según la separación de la sección siguiente. Si un
> `getAll()` acaba alimentando una vista, es señal de que falta una query.

Todo lo que sea lectura para pantallas, listados, contadores o filtros — lo que
consumen las **Queries** — va en un puerto de lectura aparte, no en el repositorio:

```java
// application/appointments/ports/AppointmentReadModel.java
public interface AppointmentReadModel {
    List<AppointmentSummary> findTodaysAppointments(UserId organizerId, LocalDate date);
}
```

> **Corrección sobre `cqrs-conventions.md`:** el ejemplo de `GetTodaysAppointmentsQueryHandler`
> de ese documento usaba `AppointmentRepository` para leer. Con esta convención ya
> definida, debería depender de `AppointmentReadModel` en su lugar — se corrige ahí
> también para que ambos documentos queden alineados.

## La implementación: `AbstractSqlRepository`

La base del kernel resuelve la fontanería JDBC (sentencias, cierre de recursos,
traducción de `SQLException` a `PersistenceException`, rastreo del agregado) y **genera
el SQL** a partir de los nombres de columna. El módulo aporta solo lo que es
conocimiento suyo: qué columnas hay y cómo se mapea su agregado.

```java
// infrastructure/appointments/persistence/SqliteAppointmentRepository.java
public final class SqliteAppointmentRepository
        extends AbstractSqlRepository<Appointment, AppointmentId> implements AppointmentRepository {

    public SqliteAppointmentRepository(Connection connection) {
        super(connection, "appointments", "id");
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "starts_at", "ends_at", "status");
    }

    @Override
    protected void bind(PreparedStatement statement, Appointment appointment) throws SQLException {
        statement.setLong(1, appointment.id().value());
        statement.setString(2, appointment.timeSlot().start().toString());
        statement.setString(3, appointment.timeSlot().end().toString());
        statement.setString(4, appointment.status().name());
    }

    @Override
    protected Appointment mapRow(ResultSet row) throws SQLException {
        return Appointment.rehydrate(...);
    }

    @Override
    protected Object idValue(AppointmentId id) {
        return id.value();
    }
}
```

Reglas del contrato:

- `columns()` incluye **todas** las columnas, la del id la primera, y `bind(...)` las
  liga en ese mismo orden. `INSERT` las usa todas; `UPDATE` las asigna todas y añade
  `WHERE id = ?` al final, que liga la base.
- El mapeo (`bind` / `mapRow`) se escribe **a mano, siempre**. No hay reflexión ni
  anotaciones: traducir entre agregado y tabla es conocimiento de dominio, y
  automatizarlo empuja hacia el dominio anémico que `rich-domain-conventions.md` existe
  para evitar.
- Los nombres de tabla y columna se validan contra `[A-Za-z_][A-Za-z0-9_]*`. Van
  concatenados al SQL (un identificador no puede ir en un parámetro), así que la
  validación es la barrera que impide que un nombre mal formado se convierta en
  inyección.

## Migraciones de esquema: una secuencia por contexto

Cada bounded context tiene su fichero SQLite, así que tiene **su propia secuencia de
migraciones**, independiente de la de los demás. `SchemaMigrator` las aplica al arrancar.

Los ficheros viven en los recursos del módulo y siguen la convención `V<numero>__<nombre>.sql`:

```
infrastructure/src/main/resources/db/appointments/
    V001__create_appointments.sql
    V002__add_reminder_column.sql
```

```java
var migrator = new SchemaMigrator(connection, clock);
migrator.migrate(Migrations.load(
    Main.class, "/db/appointments", "V001__create_appointments.sql", "V002__add_reminder_column.sql"));
```

Los ficheros se **enumeran explícitamente** en vez de escanear el classpath: escanear
recursos con JPMS es frágil, y una lista visible hace que añadir una migración sea un
cambio revisable en el diff.

### Las tres reglas que impone

- **Una migración aplicada no se edita jamás.** El migrador guarda un checksum SHA-256
  de cada script; si el contenido cambia después de haberse aplicado, **falla al
  arrancar** con un mensaje que lo dice. Editar una migración ya aplicada significa que
  tu base de datos y la de cualquier otro entorno divergen en silencio: el error ruidoso
  es infinitamente mejor.
- **No se cuelan versiones por debajo.** Si la última aplicada es la V3 y aparece una V2
  nueva, falla y pide renumerarla. Si no, según el orden en que se creen las bases de
  datos unas la tendrían y otras no.
- **Cada migración va en su propia transacción**, y se registra en `schema_version` solo
  si su SQL tuvo éxito. Si la V2 falla, la V1 queda aplicada y registrada, y al
  reintentar se retoma exactamente donde se quedó.

## Unit of Work: uno por bounded context, nunca cruza contextos

Cada bc tiene su propio Unit of Work, que envuelve **una única conexión** al fichero
SQLite de ese contexto. Los repositorios de ese mismo bc comparten la conexión —
pueden participar en la misma transacción. Un repositorio de otro bc **nunca** puede
compartir Unit of Work con este, porque apunta a un fichero distinto.

El Shared Kernel aporta `AbstractUnitOfWork`, que **impone el orden** persistir →
commit → publicar eventos. El módulo solo implementa los tres métodos que dependen de
SQLite:

```java
// infrastructure/appointments/persistence/SqliteAppointmentUnitOfWork.java
public final class SqliteAppointmentUnitOfWork extends AbstractUnitOfWork {

    private final Connection connection;
    private final AppointmentRepository appointments;

    public SqliteAppointmentUnitOfWork(
            Connection connection, PendingEventDispatcher dispatcher, SequenceGenerator sequences) {
        super(dispatcher);
        this.connection = connection;
        this.appointments = new SqliteAppointmentRepository(connection, sequences);
    }

    public AppointmentRepository appointments() {
        return appointments;
    }

    @Override
    protected void begin() {
        try { connection.setAutoCommit(false); } catch (SQLException e) { throw new PersistenceException(e); }
    }

    @Override
    protected void commit() {
        try { connection.commit(); } catch (SQLException e) { throw new PersistenceException(e); }
    }

    @Override
    protected void rollback() {
        try { connection.rollback(); } catch (SQLException e) { throw new PersistenceException(e); }
    }
}
```

Uso desde `Application`:

```java
return unitOfWork.execute(() -> {
    var appointment = Appointment.schedule(appointments.nextId(), timeSlot, organizerId, now);
    appointments.save(appointment);

    return Result.success(appointment.id());
});
```

`execute(...)` está declarado **`final`**: ningún módulo puede reordenar las fases ni
olvidarse de una. Su contrato:

| Lo que devuelve el trabajo | Transacción | Eventos |
|---|---|---|
| Valor normal o `Result` correcto | `commit()` | Se publican |
| `Result` en fallo | `rollback()` | **No** se publican |
| Excepción | `rollback()` y se relanza | **No** se publican |

Que un `Result.failure` provoque `rollback` es deliberado: un fallo de negocio no debe
dejar rastro persistido aunque el agregado se hubiera modificado antes de rechazar.

### El rastreo del agregado es automático

Toda escritura debe registrar el agregado para que sus eventos se publiquen tras el
commit. Si extiendes `AbstractSqlRepository`, **ya está hecho**: `create`, `update` y
`delete` llaman a `AggregateChanges.track(...)` por ti.

Solo si escribes un repositorio a mano (sin la base) tienes que acordarte:

```java
AggregateChanges.track(appointment);
```

Sin esa llamada, los eventos se quedan en `pendingEvents()` y **no se publica nada** —
el fallo silencioso clásico. Por eso `AggregateChanges.track(...)` lanza
`IllegalStateException` si se llama fuera de una unidad de trabajo: prefiere romper
ruidosamente a tragarse eventos. Como efecto secundario, cualquier escritura fuera de
una transacción falla en el acto.

El rastreo usa `ScopedValue`, así que el ámbito es por ejecución y no hay estado
compartido entre hilos. Rastrear dos veces el mismo agregado publica sus eventos una
sola vez.

Las unidades de trabajo **no se anidan**: `execute` dentro de otro `execute` lanza. Si
necesitas coordinar dos contextos, son dos transacciones separadas y la consistencia
entre ellas es eventual (ver más abajo).

### Entrega de eventos: inmediata u outbox

`AbstractUnitOfWork` no publica los eventos por su cuenta: delega en una `EventDelivery`,
y hay dos, elegidas en el composition root.

**`ImmediateEventDelivery`** — publica en memoria justo después del commit. Simple y
suficiente mientras los eventos solo se consumen dentro del mismo contexto. Su punto
débil es el que ya documentaba este documento: si el handler falla *después* del commit,
el evento se pierde y queda inconsistencia silenciosa.

**`OutboxEventDelivery`** — escribe los eventos serializados en la tabla
`outbox_messages` **dentro de la misma transacción** que el agregado, y los entrega
después del commit. Si la entrega falla, el mensaje sigue pendiente con su contador de
reintentos y su último error: no se pierde nada.

```java
new OutboxEventDelivery(
    new SqlOutboxStore(connection),
    new JacksonDomainEventSerializer(),
    new OutboxProcessor(store, serializer, publisher, clock));
```

La diferencia real está en el orden de las operaciones:

| | Immediate | Outbox |
|---|---|---|
| Escritura del evento | No se persiste | En la transacción del agregado |
| Si falla el handler | Excepción tras commit, evento perdido | Se marca el error, se reintenta |
| Coste | Ninguno | Una tabla y una escritura por evento |

Que la escritura ocurra **dentro** de la transacción es lo que hace que el mecanismo
funcione: o se guardan el agregado y sus eventos, o no se guarda ninguno de los dos.

`DomainEventSerializer` es un puerto que implementa cada módulo con Jackson jr — el
Shared Kernel no incorpora una librería JSON. `SqlOutboxStore.createTableIfMissing()`
crea el esquema; `OutboxProcessor.process(batchSize, maxRetryCount)` deja de reintentar
un mensaje cuando alcanza el máximo, para que un evento envenenado no bloquee la cola.

## Consecuencias del aislamiento físico

- **Sin JOIN entre contextos, nunca.** Si `Application` necesita datos de dos bcs a la
  vez (ej. una cita y el nombre del usuario organizador), se resuelve llamando a dos
  repositorios/read models distintos y combinando los resultados en memoria — nunca en
  SQL.
- **Sin transacción entre contextos, nunca.** Ya se dijo en `architecture.md` que "un
  agregado = un límite transaccional"; el aislamiento físico lo refuerza a nivel
  técnico: ni siquiera sería posible compartir una transacción SQLite entre dos
  ficheros distintos.
- **La consistencia entre contextos es eventual, vía Domain Events.** Se decidió que
  los eventos se despachan de forma síncrona en memoria (`architecture.md`) — pero
  "síncrono" aquí significa *mismo hilo, inmediatamente después de guardar*, **no**
  *misma transacción*. Si el bc que originó el evento ya hizo `commit()` y el handler
  que reacciona en otro bc falla al guardar, el primer cambio queda persistido y el
  segundo no — es una inconsistencia temporal real, no solo teórica. **Ésa es
  exactamente la razón de `OutboxEventDelivery`**: con outbox el evento queda
  persistido junto al agregado y se reintenta hasta entregarse, en vez de evaporarse.
  Para eventos que cruzan contextos, usa outbox.

## Pendiente / a definir más adelante

- **Idempotencia de los handlers.** El outbox garantiza entrega *al menos una vez*: si
  la entrega tiene éxito pero el `markProcessed` falla, el evento se reentrega. Los
  handlers que escriban deben tolerarlo, y aún no hay convención para ello.
- **Quién drena el outbox si el proceso muere** entre el commit y la entrega. Hoy solo
  se procesa al final de cada unidad de trabajo; falta decidir si hay un barrido
  periódico al arrancar o en segundo plano.
- **Limpieza de mensajes ya procesados** (hoy se quedan en la tabla para siempre).
- Convención de nombre de fichero por bc (ej. `appointments.db`) y dónde viven en
  disco.
- Si el `AppointmentReadModel` comparte la misma conexión/fichero que el repositorio de
  escritura (probable, es el mismo bc) o si en algún caso conviene una réplica de
  lectura — no parece necesario a esta escala, pero se deja constancia de que se
  descartó explícitamente, no por omisión.
- Patrón de mapeo `ResultSet ↔ Domain` (`AppointmentRowMapper` en el ejemplo) — mismo
  pendiente que ya señalaba `architecture.md`.
