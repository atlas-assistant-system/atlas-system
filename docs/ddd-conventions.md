# Convenciones de DDD

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

Este documento define cómo se implementan en la práctica los building blocks de DDD
descritos en `architecture.md`, con el patrón concreto para cada uno. Los ejemplos están
extraídos y adaptados a Java 25 (sin framework) a partir de un proyecto Clean
Architecture + DDD maduro propio (GeneFlow.ApiNet2, un backend .NET) — con las
diferencias que se explican en cada sección.

> **Idioma del código:** el código (clases, métodos, paquetes) se escribe en **inglés**.
> Esta propia documentación se mantiene en español, como el resto de docs del harness.

## Value Objects

Un Value Object se modela como un **`record`** de Java, no como una clase con una base
`ValueObject` reimplementando igualdad estructural — el propio `record` ya da
`equals`/`hashCode`/`toString` basados en sus componentes de forma gratuita. Esta es la
diferencia principal frente al proyecto de referencia en C#, que sí necesita esa clase
base porque el lenguaje no tiene records con esa semántica hasta hace poco.

El Shared Kernel aporta dos interfaces (`sharedkernel.domain.ddd`):

- **`ValueObject`**: interfaz marcadora vacía. Todo VO la declara. No aporta
  comportamiento — existe como ancla para las reglas de ArchUnit ("todo lo que vive en
  `vos/` es un record que implementa `ValueObject`") y para documentar la intención.
- **`SingleValueObject<T> extends ValueObject`**: para VOs que envuelven un único valor.
  Declara `T value()`; un record cuyo componente se llame `value` la implementa sin
  escribir nada más, porque el accessor canónico ya satisface el método. Esto además
  fija por compilador la convención de nombre: si el componente no se llama `value`, no
  compila. Infrastructure y Presentación pueden así tratar cualquier VO de un solo
  valor de forma uniforme (`vo.value()`) sin reflexión.

```java
public record AppointmentTitle(String value) implements SingleValueObject<String> {

    public static Result<AppointmentTitle> create(String value) { /* ... */ }
}
```

Reglas:
- Un VO de un solo valor implementa `SingleValueObject<T>` y su componente se llama
  `value`; un VO de varios componentes implementa `ValueObject`.
- El constructor canónico es **privado**; la única forma de crear una instancia válida
  es a través de una factoría estática `create(...)` que devuelve `Result<T>`.
- Toda regla de negocio (longitud, formato, rango) se valida en la factoría, nunca fuera
  de ella — no puede existir una instancia inválida.
- Inmutable por definición (los records lo son).

```java
public record TimeSlot(LocalDateTime start, LocalDateTime end) implements ValueObject {

    public static Result<TimeSlot> create(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return Result.failure(AppointmentErrors.TIME_SLOT_REQUIRED);
        }
        if (!end.isAfter(start)) {
            return Result.failure(AppointmentErrors.INVALID_TIME_SLOT);
        }
        return Result.success(new TimeSlot(start, end));
    }

    public Duration duration() {
        return Duration.between(start, end);
    }
}
```

> El constructor canónico de un `record` público es técnicamente público a menos que se
> declare explícitamente `private TimeSlot(...)`. Declararlo así es obligatorio para
> forzar el paso por `create(...)`.

## Identificadores fuertemente tipados

Son un caso especial de Value Object: envuelven un único valor y existen para que el
compilador impida pasar un `UserId` donde se espera un `AppointmentId`. Viven en la
raíz del bounded context (`domain/<bc>/`), no dentro de `vos/` — son la identidad del
agregado, no un atributo suyo. Patrón completo (ID con prefijo legible para aggregate
roots, UUID para entidades internas, generación de la parte numérica) en
[id-conventions.md](id-conventions.md).

## Entidades (no raíz)

Objetos con identidad propia que viven **dentro** de un agregado, pero que no son su
raíz (ej. un recordatorio dentro de una cita). Se modelan como clases (no records,
porque son mutables) que extienden **`Entity<TId>`** del Shared Kernel
(`sharedkernel.domain.ddd`), que aporta el id y la igualdad por identidad
(`equals`/`hashCode` por clase + id, con guard de id no nulo). `AggregateRoot<TId>`
extiende a su vez `Entity<TId>` — la raíz es una entidad más, con la responsabilidad
añadida de los Domain Events.

Reglas:
- Constructor privado; se crean con una factoría de visibilidad **de paquete** (sin
  modificador — el equivalente Java de `internal` en C#), para que solo código del mismo
  paquete (en la práctica, el propio agregado raíz) pueda instanciarlas.
- Sus métodos de mutación también son de visibilidad de paquete: `Application` nunca
  llama a un método de una entidad interna directamente, siempre pasa por un método del
  agregado raíz.
- No reimplementan `equals`/`hashCode` — los heredan de `Entity`.

```java
public final class Attendee extends Entity<UserId> {

    private AttendeeStatus status;

    private Attendee(UserId userId, AttendeeStatus status) {
        super(userId);
        this.status = status;
    }

    static Attendee create(UserId userId, AttendeeStatus status) {
        return new Attendee(userId, status);
    }

    void changeStatus(AttendeeStatus newStatus) {
        this.status = newStatus;
    }

    public AttendeeStatus status() { return status; }
}
```

## Aggregate Roots

La raíz de un agregado es el único punto de entrada para modificar cualquier cosa
dentro de él (incluidas sus entidades internas). Patrón de cada método de
comportamiento:

1. Validar autorización/reglas de negocio → si falla, `Result.failure(...)` inmediato.
2. Mutar el estado interno.
3. Lanzar el/los Domain Event(s) correspondientes.
4. Devolver `Result.success()` (o `Result.success(value)` si aplica).

```java
public final class Appointment extends AggregateRoot<AppointmentId> {

    private TimeSlot timeSlot;
    private AppointmentStatus status;
    private final List<Attendee> attendees = new ArrayList<>();

    private Appointment(AppointmentId id, TimeSlot timeSlot, UserId organizerId) {
        super(id);
        this.timeSlot = timeSlot;
        this.status = AppointmentStatus.SCHEDULED;
        this.attendees.add(Attendee.create(organizerId, AttendeeStatus.CONFIRMED));
    }

    public static Result<Appointment> schedule(AppointmentId id, TimeSlot timeSlot, UserId organizerId) {
        var appointment = new Appointment(id, timeSlot, organizerId);
        appointment.registerEvent(new AppointmentScheduledEvent(id, timeSlot, organizerId));
        return Result.success(appointment);
    }

    public Result<Void> reschedule(TimeSlot newTimeSlot, UserId actor) {
        if (!canBeModifiedBy(actor)) {
            return Result.failure(AppointmentErrors.INSUFFICIENT_PERMISSIONS);
        }
        if (status == AppointmentStatus.CANCELLED) {
            return Result.failure(AppointmentErrors.CANNOT_MODIFY_CANCELLED);
        }
        this.timeSlot = newTimeSlot;
        registerEvent(new AppointmentRescheduledEvent(id(), newTimeSlot, actor));
        return Result.success();
    }

    public Result<Void> cancel(UserId actor) {
        if (!canBeModifiedBy(actor)) {
            return Result.failure(AppointmentErrors.INSUFFICIENT_PERMISSIONS);
        }
        this.status = AppointmentStatus.CANCELLED;
        registerEvent(new AppointmentCancelledEvent(id(), actor));
        return Result.success();
    }

    public List<Attendee> attendees() {
        return Collections.unmodifiableList(attendees);
    }

    private boolean canBeModifiedBy(UserId userId) { /* ... */ return true; }
}
```

Reglas del agregado:
- **Un agregado = un límite transaccional.** Cada operación de negocio modifica un solo
  agregado y se persiste como una unidad.
- **Las referencias entre agregados son siempre por Id**, nunca por objeto (`Appointment`
  referencia `UserId`, no un `User`). Cargar el agregado referenciado, si hace falta, es
  responsabilidad del `Command`/`Query` handler en `Application`.
- Las colecciones internas se exponen como **no modificables** — la única forma de
  cambiar su contenido es a través de métodos del agregado.
- Los repositorios (definidos en `application/<bc>/ports/`, según `architecture.md`)
  trabajan siempre a nivel de raíz de agregado — nunca hay un repositorio para
  `Attendee` suelto.

## Domain Events

Se modelan como `record` inmutables, con **nombre en pasado** (`AppointmentScheduledEvent`,
no `ScheduleAppointmentEvent`), y transportan solo lo que un handler necesita — ids y
primitivos, nunca una referencia al agregado completo.

`DomainEvent` declara `Instant occurredOn()`. Como con `SingleValueObject`, un record
cuyo componente se llame `occurredOn` la implementa sin escribir nada más:

```java
public record AppointmentScheduledEvent(
    AppointmentId appointmentId,
    TimeSlot timeSlot,
    UserId organizerId,
    Instant occurredOn
) implements DomainEvent {}
```

Se lanzan **dentro** del método de comportamiento del agregado, justo después de mutar
el estado (ver ejemplo de `Appointment` arriba), y se despachan de forma **síncrona, en
memoria**, tras persistir — según lo ya decidido en `architecture.md`.

`AggregateRoot.registerEvent` rechaza con `GuardException` cualquier evento sin
`occurredOn`: un evento sin marca de tiempo no llega a entrar en la lista de pendientes.

Quien drena `pendingEvents()` es `AbstractUnitOfWork`, después del commit — el agregado
solo los acumula. El detalle de ese mecanismo (y la obligación de que `save()` llame a
`AggregateChanges.track(...)`) está en
[repository-conventions.md](repository-conventions.md).

### De dónde sale el instante

El dominio **no lee el reloj del sistema ni recibe un `Clock`**: recibe el instante
como un valor más, en el método de comportamiento que lo necesita.

```java
public Result<Void> reschedule(TimeSlot newTimeSlot, UserId actor, Instant now) {
    ...
    registerEvent(new AppointmentRescheduledEvent(id(), newTimeSlot, actor, now));
    return Result.success();
}
```

Quien inyecta el `java.time.Clock` es el handler de `Application`, por constructor, y
resuelve `clock.instant()` antes de llamar al agregado. Razones:

- El método del agregado queda como **función pura de sus argumentos**: mismos
  parámetros, mismo resultado. Testable sin infraestructura y sin mocks.
- Un `Clock` guardado como campo del agregado sería estado de infraestructura dentro del
  dominio, y habría que reinyectarlo cada vez que el repositorio rehidrata el agregado.
- Si un método necesita la hora para una regla de negocio ("no se puede agendar en el
  pasado") y además para sellar el evento, ambos usan **el mismo instante** — no dos
  lecturas del reloj separadas por microsegundos.

En tests, el instante es una constante (`Instant.parse("...")`); no hace falta ni
`Clock.fixed` en la mayoría de los casos.

## Catálogo de errores de dominio y patrón Result

Cada agregado tiene una clase `<Aggregate>Errors` con constantes/factorías estáticas de
`Error`, y todo método de comportamiento del agregado devuelve `Result`/`Result<T>` en
vez de lanzar una excepción para un fallo de negocio esperable. La forma exacta de
`Error`/`Result`, el catálogo de errores, las Guard clauses y cómo un error llega hasta
la respuesta HTTP están detallados en [error-conventions.md](error-conventions.md).

## Domain Services

Se usan cuando una regla de negocio no pertenece naturalmente a una entidad, value
object o agregado concreto — típicamente porque involucra a más de un agregado a la
vez. Deben ser la excepción, no la norma: la mayoría de la lógica debería vivir en el
propio agregado.

## Por qué clases e interfaces, y no anotaciones ni AOP

Decisión deliberada: los building blocks se marcan con herencia (`Entity`,
`AggregateRoot`) e interfaces (`ValueObject`, `SingleValueObject`), nunca con
anotaciones (`@Entity`, `@ValueObject`) ni aspectos.

- Una anotación es metadato inerte: sin un framework que la procese (reflexión en
  runtime, annotation processor o weaving de AspectJ) no aporta ningún comportamiento,
  y este stack excluye los tres a propósito.
- Las interfaces participan en el sistema de tipos: se puede escribir código genérico
  sobre `SingleValueObject<T>` verificado por el compilador; sobre "lo anotado con X"
  no.
- Para cross-cutting concerns (logging, transacciones, auditoría) la respuesta no es
  AOP sino **decoradores explícitos** sobre `CommandHandler`/`QueryHandler`, cableados
  en el composition root — visibles, navegables y testeables.

## Lenguaje ubicuo

Los nombres de clases, métodos y Domain Events deben usar el vocabulario real del
dominio (el que usaría un experto de negocio), no términos técnicos genéricos. Un
Command se llama `ScheduleAppointment`, no `CreateAppointmentCommand`, porque
"schedule" es el verbo que realmente se usa en el dominio de una agenda — evita
nombres CRUD genéricos cuando el dominio tiene un verbo más preciso.

## Pendiente / a definir más adelante

- Si se adopta un patrón de auditoría común (`createdAt`/`updatedAt`/borrado lógico) a
  nivel de Shared Kernel, similar a `AuditableEntity`/`AggregateRoot` del proyecto de
  referencia, o se deja a discreción de cada agregado.
