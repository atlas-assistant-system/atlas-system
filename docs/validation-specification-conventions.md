# Convenciones de validación y consultas: por qué no se adoptan Validator/Specification

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

## Hallazgo en el proyecto de referencia

GeneFlow.ApiNet2 tiene, en su Shared Kernel, dos mini-frameworks completos:

- `IValidator<T>`/`Validator<T>`/`RuleBuilder` — un framework de validación fluida
  estilo FluentValidation, con reglas por propiedad, condiciones (`Must`, `NotNull`,
  `Equal`...), reglas async y extracción de nombres de propiedad por reflexión sobre
  `Expression<Func<T, TProperty>>`.
- `ISpecification<T>`/`Specification<T>` — patrón Specification, con predicados
  componibles vía `&`, `|`, `!`, compilados a `Expression<Func<T, bool>>`.

Al buscar dónde se usan realmente en el resto del código (`Domain`, `Application`,
`Infrastructure`, `API`, `Tests`): **en ningún sitio**. Ni una sola clase implementa
`Validator<T>` ni `Specification<T>` fuera de su propio fichero de definición. Toda la
validación real del proyecto pasa por las factorías de Value Objects (`Result<T>`,
`StudyTitle.Create(...)`), y todo el filtrado real de consultas está escrito como LINQ
directo dentro de cada método de repositorio (`StudyRepository.GetPublishedAsync`,
por ejemplo). Son dos subsistemas construidos y nunca adoptados.

## Por qué no se portan a este stack

### Validation

- Redundante con lo que ya existe y sí se usa: los Value Objects validan en su
  factoría (`ddd-conventions.md`), las Guard clauses cubren invariantes
  (`error-conventions.md`). Un tercer mecanismo genérico de validación sería una vía
  paralela para hacer lo mismo que ya se hace en dos sitios, no algo complementario.
- El mecanismo de extracción de nombres de propiedad por reflexión sobre expresiones
  (`GetPropertyName<TProperty>(Expression<Func<T, TProperty>> ...)`) ni siquiera tiene
  equivalente directo en Java: una lambda de Java no es un árbol de expresión
  inspeccionable como en C#. Portarlo tal cual exigiría herramientas adicionales de
  bytecode para algo que el propio proyecto de referencia no llegó a necesitar.

### Specification

- Compila expresiones a un predicado LINQ evaluado en memoria o traducido por EF a
  SQL — pero ninguna consulta real del proyecto lo usa.
- Sin ORM ni LINQ-to-SQL, este patrón no tiene un modelo de ejecución claro aquí: una
  Specification compuesta habría que traducirla a mano a SQL igualmente, lo que anula
  la ventaja de tener un predicado componible.
- Ya se decidió en `repository-conventions.md` que las necesidades de consulta se
  resuelven con métodos explícitos y nombrados en el puerto de lectura
  (`findTodaysAppointments(...)`, no `findMatching(spec)`) — mismo criterio que
  sustituye por completo a Specification.

## Qué se usa en su lugar (ya decidido en otros documentos)

| Necesidad | Mecanismo | Documento |
|---|---|---|
| Validar la forma de un Value Object/agregado | Factoría + `Result` | `ddd-conventions.md` |
| Invariante que nunca debería violarse | Guard clause | `error-conventions.md` |
| Filtro de consulta | Método explícito en el puerto de lectura | `repository-conventions.md` |

## Si aparece un caso real de validación multi-campo en un Command

Ninguno de los mecanismos de arriba cubre bien un caso concreto: varios campos de un
mismo Command que se validan entre sí, antes incluso de construir el agregado (ej. "si
el tipo de asistencia es presencial, la sala es obligatoria"). Para eso no hace falta
un framework — una función dedicada, sin fluent API ni reflexión, que devuelve
`Result` como todo lo demás:

```java
// application/appointments/commands/ScheduleAppointment/ScheduleAppointmentValidation.java
final class ScheduleAppointmentValidation {

    private ScheduleAppointmentValidation() { }

    static Result<Void> validate(ScheduleAppointmentCommand command) {
        if (command.isInPerson() && command.room() == null) {
            return Result.failure(AppointmentErrors.ROOM_REQUIRED_FOR_IN_PERSON);
        }
        return Result.success();
    }
}
```

Se llama al principio de `handle(...)`, antes de tocar el dominio — un paso más del
mismo Command Handler, no una capa nueva.

## Pendiente / a definir más adelante

- Si con el tiempo aparecen muchos Commands con validación multi-campo repetitiva,
  valorar entonces (no antes) si conviene alguna micro-utilidad común — partiendo de
  casos reales observados, no por adelantado. Esta es precisamente la trampa en la que
  cayó el proyecto de referencia: construir el framework antes de tener el primer caso
  de uso real.
