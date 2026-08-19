# Convenciones para garantizar un dominio enriquecido

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

`architecture.md` ya declara la regla ("toda regla de negocio vive en `Domain`, nunca en
`Application` ni en `Infrastructure`") y `ddd-conventions.md` ya muestra el patrón rico
para cada building block. Este documento consolida ambas cosas en una checklist
accionable, con ejemplos de lo que NO hay que hacer, y cómo verificarlo automáticamente
con ArchUnit — para que la regla no dependa solo de que alguien se acuerde de seguirla.

## El problema: dominio anémico

Un **dominio anémico** (Anemic Domain Model) es aquel en el que las entidades y
agregados son solo bolsas de datos — getters y setters públicos, sin comportamiento —
y toda la lógica de negocio vive fuera, típicamente en los Command/Query handlers de
`Application`. El resultado: el propio tipo no puede garantizar sus invariantes, porque
cualquier código con acceso al objeto puede ponerlo en un estado inconsistente
saltándose las reglas.

```java
// MAL — dominio anémico: Appointment es una bolsa de datos, la regla de negocio
// vive en el handler.
public final class Appointment {
    private AppointmentStatus status;
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public AppointmentStatus getStatus() { return status; }
}

public final class CancelAppointmentCommandHandler
    implements CommandHandler<CancelAppointmentCommand, Result<Void>> {

    public Result<Void> handle(CancelAppointmentCommand command) {
        var appointment = appointments.findById(command.appointmentId());
        // La regla de negocio ("no se puede cancelar dos veces") vive aquí,
        // no en Appointment. Cualquier otro handler podría olvidarse de comprobarlo.
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            return Result.failure(AppointmentErrors.ALREADY_CANCELLED);
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointments.save(appointment);
        return Result.success();
    }
}
```

```java
// BIEN — dominio enriquecido: la regla vive en Appointment. El handler solo orquesta.
public final class Appointment extends AggregateRoot<AppointmentId> {
    private AppointmentStatus status;

    public Result<Void> cancel(UserId actor) {
        if (status == AppointmentStatus.CANCELLED) {
            return Result.failure(AppointmentErrors.ALREADY_CANCELLED);
        }
        status = AppointmentStatus.CANCELLED;
        registerEvent(new AppointmentCancelledEvent(id(), actor));
        return Result.success();
    }
}

public final class CancelAppointmentCommandHandler
    implements CommandHandler<CancelAppointmentCommand, Result<Void>> {

    public Result<Void> handle(CancelAppointmentCommand command) {
        var appointment = appointments.findById(command.appointmentId());
        var result = appointment.cancel(command.actor());
        if (result.isFailure()) {
            return result;
        }
        appointments.save(appointment);
        appointment.pendingEvents().forEach(events::publish);
        appointment.clearEvents();
        return Result.success();
    }
}
```

Es literalmente la misma regla de negocio en los dos ejemplos — lo único que cambia es
**dónde vive**. Esa es toda la diferencia entre un dominio anémico y uno enriquecido.

## Señales de alerta

Si alguna de estas aparece durante el desarrollo, es una señal de que se está
deslizando hacia un dominio anémico:

- Un método público `setX(...)` en una Entity o Aggregate Root.
- Un Command Handler con un `if`/`switch` que evalúa el **estado del dominio** (no
  infraestructura, no autorización de aplicación) para decidir si algo es válido.
- Un Value Object cuyo constructor no valida nada — solo envuelve un valor sin poder
  rechazar un estado inválido.
- Una clase de dominio con constructor público que permite construir el objeto en un
  estado incompleto o inconsistente.
- Lógica de negocio duplicada en dos Command Handlers distintos porque nadie la puso en
  el agregado la primera vez.
- Un test que solo comprueba "el setter cambió el campo" en vez de comprobar el
  resultado de una operación de negocio con nombre de intención.
- Lógica condicional repetida en varios sitios que compara contra los valores de un
  mismo enum (`if (status == SCHEDULED || status == CONFIRMED)` en más de un Command
  Handler) en vez de vivir como método del propio enum — ver
  [enum-conventions.md](enum-conventions.md).

## Reglas de diseño

Ya cubiertas en detalle en `ddd-conventions.md`, aquí como checklist:

- **Constructor privado + factoría estática** en Value Objects, Entities y Aggregate
  Roots — nunca se puede construir un objeto de dominio en estado inválido.
- **Mutadores con nombre de intención de negocio** (`cancel()`, `reschedule()`,
  `addAttendee()`), nunca `setX(...)`. El nombre del método debe poder leerse como una
  frase que un experto de negocio reconocería.
- **Cero setters públicos**, sin excepción, en `domain/`.
- **Los Command Handlers son orquestación pura**: cargar el agregado (o construirlo),
  invocar un método de negocio, persistir, publicar eventos. Ninguna decisión de
  negocio adicional en el handler.
- **Las Queries devuelven DTOs de solo lectura**, nunca el agregado de dominio — así no
  hay tentación de mutarlo fuera de sus propios métodos.

## Verificación automática con ArchUnit

Los puntos anteriores que son mecánicos (no requieren juicio) se pueden hacer fallar el
build si se violan. Reglas propuestas (sintaxis a validar contra la versión concreta de
ArchUnit que se fije en `stack.md`):

```java
@ArchTest
static final ArchRule domain_classes_should_not_expose_public_setters =
    noMethods()
        .that().arePublic()
        .and().haveNameMatching("set[A-Z].*")
        .should().beDeclaredInClassesThat().resideInAPackage("..domain..");

@ArchTest
static final ArchRule aggregate_roots_should_have_only_private_constructors =
    classes()
        .that().areAssignableTo(AggregateRoot.class)
        .should().haveOnlyPrivateConstructors();
```

Lo que ArchUnit **no puede verificar** por sí solo: si una regla de negocio concreta
vive donde debe. Eso es "if hay lógica condicional en un handler, ¿es negocio o es
orquestación técnica?" — requiere juicio humano (o del agente que revise el código), de
ahí la checklist de abajo.

## Checklist de revisión

Antes de dar por terminado un agregado o un caso de uso:

- [ ] ¿Hay algún `setX(...)` público en una clase de `domain/`? → eliminarlo, sustituir
      por un método de intención.
- [ ] ¿El Command Handler tiene algún `if` que compruebe una regla de negocio en vez de
      delegarla al agregado? → mover la comprobación dentro del método del agregado.
- [ ] ¿Se puede construir el Value Object/Entity/Aggregate en un estado inválido desde
      fuera de su propio paquete? → revisar visibilidad de constructores y factorías.
- [ ] ¿La misma regla de negocio aparece en más de un Command Handler? → señal de que
      falta un método en el agregado que la centralice.

## Pendiente / a definir más adelante

- Fijar versión concreta de ArchUnit (pendiente también en `stack.md`) para validar la
  sintaxis exacta de las reglas propuestas arriba.
- Decidir si las reglas de ArchUnit se activan desde el primer agregado real o se
  añaden cuando haya más de uno (para evitar sobre-ingeniería antes de tener casos
  reales que las justifiquen).
