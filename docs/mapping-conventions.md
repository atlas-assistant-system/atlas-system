# Convenciones de mapping

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

Extraído y adaptado de `StudyMappings.cs` (GeneFlow.ApiNet2): cómo se traduce un
agregado de `Domain` a los DTOs que consume `Presentación`, dónde vive ese código, y
las reglas para que no se convierta en una vía lateral de lógica de negocio ni de
acceso a datos.

## Sin extension methods

GeneFlow escribe el mapping como *extension methods* — `study.ToDto()`, como si fuera
un método del propio `Study`. Java no tiene extension methods, así que aquí es una
clase estática de métodos puros: `AppointmentMapper.toDto(appointment)`. La ubicación
ya está fijada en `architecture.md`: `application/<bc>/mappers/`.

## Regla base: función pura, un solo sentido

Un mapper **lee** un agregado (u otro objeto de dominio) y devuelve un DTO nuevo. Nunca
al revés, nunca con efectos secundarios:

- No accede a un repositorio ni a ningún puerto — todo lo que necesita se le pasa como
  parámetro. Si necesita datos de otro agregado o de otro bounded context, quien llama
  al mapper (el Command/Query handler) ya los ha resuelto antes.
- No muta el objeto de dominio que recibe.
- No hay `fromDto(...)` / mapeo inverso DTO → Domain. Reconstruir o modificar un
  agregado siempre pasa por su factoría o sus métodos de comportamiento (`Appointment.schedule(...)`,
  `appointment.cancel(...)`), nunca por un mapper — así se garantiza que toda regla de
  negocio y validación se ejecuta (ver `rich-domain-conventions.md`). Un mapper que
  "reconstruye" un agregado a partir de un DTO se salta esa garantía por completo.

## Ejemplo básico

```java
// application/appointments/mappers/AppointmentMapper.java
public final class AppointmentMapper {

    private AppointmentMapper() { }

    public static AppointmentDto toDto(Appointment appointment) {
        return new AppointmentDto(
            appointment.id().toString(),
            appointment.organizerId().toString(),
            appointment.timeSlot().start(),
            appointment.timeSlot().end(),
            appointment.status().name(),
            appointment.attendees().stream().map(AppointmentMapper::toDto).toList()
        );
    }

    public static AttendeeDto toDto(Attendee attendee) {
        return new AttendeeDto(attendee.userId().toString(), attendee.status().name());
    }

    public static List<AppointmentDto> toDtos(Collection<Appointment> appointments) {
        return appointments.stream().map(AppointmentMapper::toDto).toList();
    }
}
```

Nótese `appointment.status().name()` en vez de un par `Status`/`StatusId` como hace
GeneFlow — coherente con lo ya decidido en `enum-conventions.md`: sin `Id` numérico por
defecto, `name()` es el identificador natural.

## Variante ligera para listados

Igual que `ToSummaryDto()` en GeneFlow: un DTO con menos campos para vistas de lista,
donde traer todo (`AppointmentDto` completo, con la lista de asistentes) sería
desperdiciar trabajo.

```java
public static AppointmentSummaryDto toSummaryDto(Appointment appointment) {
    return new AppointmentSummaryDto(
        appointment.id().toString(),
        appointment.timeSlot().start(),
        appointment.status().name(),
        appointment.attendees().size()
    );
}
```

## Enriquecimiento con datos de otro agregado o contexto

`StudyMappings.ToDto` acepta diccionarios de lookup opcionales (`userLookup`,
`profileLookup`) para resolver nombres de usuario sin ir a la base de datos desde el
mapper — es la aplicación directa de la regla ya fijada en `repository-conventions.md`
("combinar en memoria, nunca con un JOIN entre contextos"). El propio Query handler es
quien construye esos mapas antes de llamar al mapper:

```java
public static AppointmentDto toDto(Appointment appointment, Map<UserId, User> organizers) {
    var organizer = organizers.get(appointment.organizerId());
    var organizerName = organizer != null ? organizer.displayName() : null;

    return new AppointmentDto(
        appointment.id().toString(),
        appointment.organizerId().toString(),
        organizerName,
        appointment.timeSlot().start(),
        appointment.timeSlot().end(),
        appointment.status().name(),
        appointment.attendees().stream().map(AppointmentMapper::toDto).toList()
    );
}
```

```java
// En el Query handler — no en el mapper:
var appointments = this.appointments.findByOrganizer(query.organizerId());
var organizerIds = appointments.stream().map(Appointment::organizerId).distinct().toList();
var organizers = users.findByIds(organizerIds); // otro puerto, posiblemente de otro bc
return Result.success(appointments.stream()
    .map(a -> AppointmentMapper.toDto(a, organizers))
    .toList());
```

## Derivaciones legítimas vs lógica de negocio nueva

`StudyMappings` calcula `CurrentUserPermissionsDto` a partir del rol del miembro
actual — no es un campo 1:1 del agregado, es una forma leída para la presentación. Es
una derivación legítima **siempre que se apoye en una regla que el dominio ya expone**:

```java
// BIEN — usa una regla que Attendee/AppointmentRole ya expone, el mapper no inventa nada.
public static boolean canCancel(Appointment appointment, UserId viewer) {
    return appointment.attendeeRoleOf(viewer)
        .map(AttendeeRole::canCancelAppointment)
        .orElse(false);
}
```

```java
// MAL — el mapper decide una regla de negocio nueva que no vive en ningún sitio del
// dominio. Si mañana la regla cambia, hay que acordarse de que también vive aquí.
public static boolean canCancel(Appointment appointment, UserId viewer) {
    return appointment.organizerId().equals(viewer) && appointment.status() != CANCELLED;
}
```

La diferencia no siempre es obvia — el criterio práctico es: si la lógica del "MAL"
apareciera también en un Command Handler (por ejemplo, al validar si `cancel(...)` está
permitido), es una señal clara de que la regla debería vivir en el agregado/enum y el
mapper (y el handler) deberían limitarse a preguntárselo.

## DTOs como `record`

Los DTOs de salida (`AppointmentDto`, `AppointmentSummaryDto`, `AttendeeDto`) se
definen como `record` de Java, igual que los Value Objects, Domain Events, Commands y
Queries — inmutables, sin lógica, solo forma. Viven en `application/<bc>/dto/`.

```java
public record AppointmentDto(
    String id,
    String organizerId,
    LocalDateTime start,
    LocalDateTime end,
    String status,
    List<AttendeeDto> attendees
) { }
```

## Pendiente / a definir más adelante

- Si a partir de cierto número de campos conviene un builder en vez de un constructor
  de record con muchos parámetros posicionales (`AppointmentDto` con 6 campos ya es
  manejable; `StudyDto` en GeneFlow tiene más de 15).
- Si los mappers de un mismo bc se agrupan en una única clase (`AppointmentMapper` para
  todo el bc, como `StudyMappings`) o se separan uno por tipo de agregado/entidad — de
  momento se sigue el patrón de GeneFlow: uno por bc.
