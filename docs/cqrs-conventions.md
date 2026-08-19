# Convenciones de CQRS

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

Este documento define cómo se implementa en la práctica el CQRS explícito de la capa
`Application` descrito en `architecture.md`: Commands, Queries, sus handlers, cómo se
despachan sin ningún framework, y cómo encajan los Domain Events. Los ejemplos siguen
en Java 25 sobre el dominio `Appointment` usado en `ddd-conventions.md`.

> **Diferencia clave frente al proyecto de referencia (GeneFlow.ApiNet2):** GeneFlow usa
> MediatR para despachar Commands/Queries y para aplicar comportamientos transversales
> automáticos (autenticación, límites, membresía) vía interfaces marcador que MediatR
> descubre por reflexión. Aquí no hay framework ni contenedor de DI, así que el
> despacho y el cableado de handlers son **explícitos**: un `CommandBus`/`QueryBus`
> minimalista, registrado a mano en el composition root.

## Commands

Representan una intención de cambio. Se modelan como `record` inmutables con sufijo
`Command`, y viven junto a su handler en su propia carpeta:
`application/<bc>/commands/<Nombre>/`.

```java
public record ScheduleAppointmentCommand(
    UserId organizerId,
    LocalDateTime start,
    LocalDateTime end
) implements Command<Result<AppointmentDto>> { }
```

## Command Handlers

Un handler por Command. Responsabilidad: construir/cargar el agregado, invocar su método
de comportamiento, persistir a través del repositorio (puerto de
`application/<bc>/ports/`) y despachar los eventos pendientes.

**Un handler no valida nada.** Ni los datos de entrada, ni reglas de negocio, ni
invariantes. No hay comprobaciones de nulos, de rangos ni de formato en `application`.
Todo eso vive en el dominio, en dos sitios y solo en dos:

- **La factoría del Value Object** (`TimeSlot.create(...)`) devuelve `Result` cuando el
  dato de entrada puede venir mal del exterior.
- **El método de comportamiento del agregado** (`Appointment.schedule(...)`) hace cumplir
  las reglas de negocio, y sus **guard clauses** protegen los invariantes que ningún
  camino legítimo debería violar.

Lo único que hace el handler con la validación es **propagar el fallo**: si un
`Result` viene en fallo, lo devuelve tal cual y corta. Ese `if (isFailure()) return` no
es validación — es cortocircuito.

La razón de que sea una regla y no una preferencia: una validación escrita en el handler
solo protege a **ese** caso de uso. La misma regla escrita en el agregado protege a todos
los que existan hoy y a los que se escriban después, incluidos los tests y la carga desde
base de datos. Duplicarla en el handler además invita a que las dos versiones se separen
con el tiempo, y entonces la de fuera manda sobre la de dentro sin que nadie lo note.

```java
public final class ScheduleAppointmentCommandHandler
    implements CommandHandler<ScheduleAppointmentCommand, Result<AppointmentDto>> {

    private final AppointmentRepository appointments;
    private final DomainEventPublisher events;

    public ScheduleAppointmentCommandHandler(AppointmentRepository appointments, DomainEventPublisher events) {
        this.appointments = appointments;
        this.events = events;
    }

    @Override
    public Result<AppointmentDto> handle(ScheduleAppointmentCommand command) {
        var timeSlotResult = TimeSlot.create(command.start(), command.end());
        if (timeSlotResult.isFailure()) {
            return Result.failure(timeSlotResult.error());
        }

        var appointmentResult = Appointment.schedule(
            appointments.nextId(), timeSlotResult.value(), command.organizerId());
        if (appointmentResult.isFailure()) {
            return Result.failure(appointmentResult.error());
        }

        var appointment = appointmentResult.value();
        appointments.save(appointment);

        appointment.pendingEvents().forEach(events::publish);
        appointment.clearEvents();

        return Result.success(AppointmentMapper.toDto(appointment));
    }
}
```

> `AppointmentMapper` — ver [mapping-conventions.md](mapping-conventions.md) para la
> convención completa de mapping.

> Sin ORM no hay un "change tracker" que descubra automáticamente qué agregados tienen
> eventos pendientes (a diferencia de `StudyUnitOfWork` en GeneFlow, que los recoge de
> todas las entidades rastreadas por EF antes de guardar). Aquí el propio handler es
> explícito: guarda el agregado y, si se guardó con éxito, publica y limpia sus eventos.

## Queries

Representan una petición de lectura, sin efectos secundarios. Mismo patrón de carpeta:
`application/<bc>/queries/<Nombre>/`. Pueden saltarse el modelo de dominio si conviene
leer directamente una proyección.

```java
public record GetTodaysAppointmentsQuery(
    UserId userId
) implements Query<Result<List<AppointmentDto>>> { }

public final class GetTodaysAppointmentsQueryHandler
    implements QueryHandler<GetTodaysAppointmentsQuery, Result<List<AppointmentDto>>> {

    private final AppointmentReadModel appointments;
    private final Clock clock;

    public GetTodaysAppointmentsQueryHandler(AppointmentReadModel appointments, Clock clock) {
        this.appointments = appointments;
        this.clock = clock;
    }

    @Override
    public Result<List<AppointmentDto>> handle(GetTodaysAppointmentsQuery query) {
        var results = appointments.findTodaysAppointments(query.userId(), LocalDate.now(clock));
        return Result.success(results.stream().map(AppointmentMapper::toDto).toList());
    }
}
```

> Las Queries dependen de un puerto de **lectura** (`AppointmentReadModel`), separado
> del repositorio de escritura (`AppointmentRepository`) que usan los Commands — ver
> `repository-conventions.md` para por qué se separan y qué va en cada uno.

Tanto Commands como Queries devuelven siempre `Result<T>` — nunca lanzan una excepción
para un fallo de negocio esperable (ver `ddd-conventions.md`, sección "El patrón
Result").

## Contratos base (Shared Kernel)

Viven en `sharedkernel/application/cqrs/`, son genéricos y no conocen ningún bc:

```java
public interface Command<R> { }

public interface CommandHandler<C extends Command<R>, R> {
    R handle(C command);
}

public interface Query<R> { }

public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
```

## El despacho: `CommandBus` / `QueryBus`

Sin contenedor de DI que resuelva handlers por reflexión, el bus es un registro
explícito `Class → Handler`, cableado en el composition root del proyecto (no en
Shared Kernel — cada proyecto registra sus propios handlers).

```java
public interface CommandBus {
    <R> R dispatch(Command<R> command);
}

public final class SimpleCommandBus implements CommandBus {

    private final Map<Class<?>, CommandHandler<?, ?>> handlers = new HashMap<>();

    public <C extends Command<R>, R> void register(Class<C> commandType, CommandHandler<C, R> handler) {
        handlers.put(commandType, handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R dispatch(Command<R> command) {
        var handler = (CommandHandler<Command<R>, R>) handlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalStateException("No handler registered for " + command.getClass());
        }
        return handler.handle(command);
    }
}
```

`QueryBus` es el mismo patrón, separado de `CommandBus` porque conceptualmente son
buses distintos (aunque su implementación sea casi idéntica).

En el composition root:

```java
var appointmentBus = new SimpleCommandBus();
appointmentBus.register(ScheduleAppointmentCommand.class, new ScheduleAppointmentCommandHandler(appointmentRepository, eventPublisher));
appointmentBus.register(CancelAppointmentCommand.class, new CancelAppointmentCommandHandler(appointmentRepository, eventPublisher));
```

## Comportamientos transversales (autenticación, autorización...)

GeneFlow resuelve esto con *pipeline behaviors* de MediatR: un Command implementa una
interfaz marcador vacía (ej. `IRequireAuthentication`) y un behavior genérico,
descubierto automáticamente por el contenedor de DI, envuelve la ejecución del handler
sin que este tenga que llamarlo. Replicar ese automatismo en Java sin DI exigiría
reflexión genérica no trivial (inspeccionar interfaces marcador en tiempo de ejecución y
construir el `Result<T>` de fallo de forma genérica).

**Propuesta para este stack (a confirmar):** en lugar de ese automatismo, cada handler
llama explícitamente a un servicio reutilizable al principio de `handle(...)` — más
código repetido, pero explícito, sin reflexión y sin magia:

```java
@Override
public Result<AppointmentDto> handle(ScheduleAppointmentCommand command) {
    if (!authorization.isAuthenticated(command.organizerId())) {
        return Result.failure(CommonErrors.NOT_AUTHENTICATED);
    }
    // ...resto del handler
}
```

Si el repetirlo en cada handler resulta demasiado ruidoso en la práctica, la alternativa
es un `CommandBus` con una lista fija de "gates" (comprobaciones) que se ejecutan en
orden antes de invocar el handler — sin reflexión, cada gate implementado a mano para el
subconjunto de Commands que lo necesitan. Se deja como decisión abierta hasta ver cómo
de repetitivo resulta en la práctica.

## Domain Event Handlers (reacciones en Application)

Un handler de aplicación por cada reacción a un evento (no por evento — un mismo evento
puede tener varias reacciones independientes). Viven en `application/<bc>/events/`.

```java
public interface DomainEventHandler<E extends DomainEvent> {
    void handle(E event);
}

public final class NotifyAttendeesOnAppointmentScheduledHandler
    implements DomainEventHandler<AppointmentScheduledEvent> {

    private final NotificationSender notifications;

    public NotifyAttendeesOnAppointmentScheduledHandler(NotificationSender notifications) {
        this.notifications = notifications;
    }

    @Override
    public void handle(AppointmentScheduledEvent event) {
        try {
            notifications.send(event.organizerId(), "Appointment scheduled");
        } catch (Exception e) {
            // Un efecto secundario que falla nunca debe tumbar el Command que lo originó.
        }
    }
}
```

`DomainEventPublisher` (Shared Kernel) permite múltiples handlers por tipo de evento:

```java
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}

public final class SimpleDomainEventPublisher implements DomainEventPublisher {

    private final Map<Class<?>, List<DomainEventHandler<?>>> handlers = new HashMap<>();

    public <E extends DomainEvent> void subscribe(Class<E> eventType, DomainEventHandler<E> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(DomainEvent event) {
        for (var handler : handlers.getOrDefault(event.getClass(), List.of())) {
            ((DomainEventHandler<DomainEvent>) handler).handle(event);
        }
    }
}
```

## Naming

- Commands: verbo en imperativo + sustantivo del dominio — `ScheduleAppointment`,
  `CancelAppointment`. Nunca `CreateAppointmentCommand` genérico si el dominio tiene un
  verbo más preciso (ver "Lenguaje ubicuo" en `ddd-conventions.md`).
- Queries: `Get<Algo>` para una única respuesta, `List<Algo>`/`GetTodays<Algo>` etc. para
  colecciones — describen la pregunta, no una acción.
- Handlers: `<Nombre>CommandHandler` / `<Nombre>QueryHandler`, siempre en la misma
  carpeta que su Command/Query.

## Pendiente / a definir más adelante

- Confirmar si los comportamientos transversales van por llamada explícita en cada
  handler o por una lista fija de gates en el `CommandBus` (ver sección de arriba).
- Catálogo real de comportamientos transversales que necesita este proyecto (autenticación,
  ¿autorización por rol?, ¿límites de uso?) — en GeneFlow son específicos de su dominio
  de negocio (SaaS con planes de pago) y no aplican tal cual.
- Si `CommandBus`/`QueryBus` son interfaces de Shared Kernel (contrato genérico) con la
  implementación (`SimpleCommandBus`) también en Shared Kernel, o si cada proyecto
  implementa la suya — de momento se asume que la implementación genérica también es
  Shared Kernel, por ser código sin lógica de negocio.
