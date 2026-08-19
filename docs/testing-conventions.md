# Convenciones de testing

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

Este documento es la instanciación de las convenciones base de testing ya
establecidas (skills `test-conventions`/`unit-testing`, agnósticas de dominio y de
framework), ajustadas a que aquí no hay Spring ni ningún framework de aplicación, y
ampliadas con un par de prácticas observadas en el proyecto de referencia
(GeneFlow.ApiNet2): estructura de test que refuerza la separación de módulos JPMS, y
mutation testing con umbrales concretos sobre `Domain`.

> "Sin framework" se refiere al código de producción. Las herramientas de test
> (JUnit, AssertJ, Mockito, JaCoCo, PIT) son librerías de testing, no frameworks de
> aplicación — no contradicen `stack.md`.

## Estructura: un único `src/test/java`, espejo del código

Los tests viven en un solo `src/test/java` que **replica la estructura de paquetes del
código**: primero el anillo, dentro el bounded context y dentro el tipo de elemento.

Como todo comparte source set, nada impide técnicamente que un test de `domain` importe
`infrastructure`. **No se hace**: un test de dominio que necesita infraestructura está
diciendo que la lógica se ha escapado del dominio, y esa es la señal a atender, no el
import a añadir.

```
src/test/java/<paquete>/domain/appointments/
  AppointmentTest.java
  vos/TimeSlotTest.java
  enums/AppointmentStatusTest.java

src/test/java/<paquete>/application/appointments/
  commands/ScheduleAppointmentCommandHandlerTest.java
  queries/GetTodaysAppointmentsQueryHandlerTest.java

src/test/java/<paquete>/infrastructure/appointments/
  persistence/SqliteAppointmentRepositoryIT.java

src/test/java/<paquete>/architecture/
  ArchitectureTest.java     Reglas compartidas de ArchUnit

src/test/java/<paquete>/support/builders/
```

## Naming

Formato `shouldExpectedBehaviorWhenCondition`, describiendo comportamiento — no
`Handle_WithValidData_ShouldCreateStudy` al estilo GeneFlow (mismo espíritu, formato
distinto; se sigue la convención ya establecida en `test-conventions`).

- Bien: `shouldFailWhenTimeSlotIsInThePast()`, `shouldRaiseScheduledEventWhenAppointmentIsCreated()`.
- Mal: `test1`, `testSchedule`, `shouldWork`.

Sufijos: `Test` para unit, `IT` para integration (contra SQLite real, sin mocks de
persistencia).

## Given / When / Then

```java
@Test
void shouldFailWhenEndIsBeforeStart() {
    // Given
    var start = LocalDateTime.of(2026, 3, 1, 10, 0);
    var end = start.minusHours(1);

    // When
    var result = TimeSlot.create(start, end);

    // Then
    assertThat(result.isFailure()).isTrue();
    assertThat(result.error()).isEqualTo(AppointmentErrors.INVALID_TIME_SLOT);
}
```

Comentarios `// Given/When/Then` opcionales si el test ya es evidente por sí mismo.

## Construcción de datos de prueba: factoría de dominio antes que builder

La convención base pide builders para objetos complejos (`ItemTestBuilder.anItem()`).
Aquí, para Value Objects/Aggregate Roots, la propia factoría del dominio **ya es** el
builder — no hace falta envolverla:

```java
var appointment = Appointment.schedule(
    AppointmentId.of(1), aTimeSlot(), UserId.of(1)
).value();
```

Un builder de test solo se justifica cuando construir el objeto a mano se vuelve
repetitivo o ruidoso (muchos campos opcionales, DTOs con 10+ campos como `AppointmentDto`
enriquecido). En ese caso, sigue la convención base: vive en `support/builders`, se
reutiliza antes de crear uno nuevo.

## Qué probar según el tipo de clase

Tabla de la convención base, con la columna derecha ajustada a los documentos propios
de este proyecto:

| Clase | Qué probar | Ver también |
|---|---|---|
| Aggregate Root / Entity | Invariantes, transiciones de estado, Domain Events lanzados | `ddd-conventions.md` |
| Value Object | Creación válida/inválida, igualdad por valor, límites | `ddd-conventions.md` |
| Command/Query Handler | Camino feliz, `Result.failure` esperados, orquestación (sin duplicar reglas de negocio — si hace falta duplicar un `if` del agregado para probarlo, es señal de fuga, ver `rich-domain-conventions.md`) | `cqrs-conventions.md` |
| Enum con lógica | Métodos de comportamiento, `valueOf`/`name()`, transiciones | `enum-conventions.md` |
| Mapper | Campos obligatorios/opcionales, enriquecimiento con lookup, ausencia de lógica de negocio nueva | `mapping-conventions.md` |
| Repositorio (implementación) | Test de integración (`IT`) contra SQLite real, no mockeado | `repository-conventions.md` |
| DTO/record simple | Estructural — última prioridad, no debe desplazar tests de comportamiento | — |

## Errores esperados: `Result.failure`, no excepción

La convención base habla de "errores esperados" de forma genérica; aquí eso es
concreto (ver `error-conventions.md`): un fallo de negocio se comprueba con
`result.isFailure()` + `result.error()`, nunca con `assertThrows`. `assertThrows` se
reserva para las Guard clauses (`GuardException`) — violaciones de invariante que
nunca deberían pasar si el código que llama es correcto.

```java
// Fallo de negocio esperable
var result = appointment.cancel(unauthorizedUser);
assertThat(result.isFailure()).isTrue();
assertThat(result.error()).isEqualTo(AppointmentErrors.INSUFFICIENT_PERMISSIONS);

// Violación de invariante (bug si ocurre en producción)
assertThatThrownBy(() -> Guard.notNull(null, "organizerId"))
    .isInstanceOf(GuardException.class);
```

## Mocks

Mockear **puertos** (`AppointmentRepository`, `AppointmentReadModel`,
`DomainEventPublisher`), nunca objetos de dominio. Un test de un Command Handler
mockea el repositorio; un test del propio agregado no mockea nada — se construye y se
invoca directamente.

```java
class ScheduleAppointmentCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-16T10:15:30Z");

    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final DomainEventPublisher events = mock(DomainEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ScheduleAppointmentCommandHandler handler =
        new ScheduleAppointmentCommandHandler(appointments, events, clock);

    @Test
    void shouldPersistAppointmentWhenTimeSlotIsValid() {
        when(appointments.nextId()).thenReturn(AppointmentId.of(1));

        var start = LocalDateTime.of(2026, 8, 16, 11, 0);

        var result = handler.handle(
            new ScheduleAppointmentCommand(UserId.of(1), start, start.plusHours(1)));

        assertThat(result.isSuccess()).isTrue();
        verify(appointments).save(any(Appointment.class));
    }
}
```

Sin cadenas largas de `verify(...)` como único assert — el `verify` acompaña al
resultado observable, no lo sustituye.

## Tiempo y aleatoriedad

Nada de `LocalDateTime.now()`/`UUID.randomUUID()` en código de dominio testeable. Las
IDs ya pasan por `SequenceGenerator` (un puerto, ver `id-conventions.md`) — en tests se
sustituye por una implementación de prueba con valores fijos, igual que cualquier otro
puerto mockeado.

Para el tiempo, el reparto de responsabilidades es el que fija `ddd-conventions.md`:

| Capa | Qué hace | En tests |
|---|---|---|
| Domain | Recibe el `Instant` como parámetro; nunca lee el reloj | Una constante `Instant.parse(...)` |
| Application | Recibe `java.time.Clock` por constructor y llama a `clock.instant()` | `Clock.fixed(NOW, ZoneOffset.UTC)` |

Un test de dominio, por tanto, no necesita `Clock` en absoluto: le pasa una fecha fija
como cualquier otro argumento. `Clock.fixed` solo aparece en tests de handlers.

Fechas relativas al día de ejecución (`now().plusHours(1)`) están prohibidas también en
tests: convierten un fallo determinista en uno que aparece según la hora a la que se
lance la suite. Usa fechas absolutas y coherentes con el `Clock` fijado.

## Cobertura y mutation testing

- **JaCoCo** — cobertura de línea, suelo mínimo y detector de huecos. No es objetivo en
  sí mismo.
- **PIT** — mutation score, en `domain` especialmente (donde vive la lógica de
  negocio). GeneFlow aplica Stryker sobre su proyecto `Domain` con umbrales
  `high: 80, low: 60, break: 50` — propuesta inicial para el módulo `domain` de este
  proyecto, a confirmar cuando haya código real que los valide.

## Tests de arquitectura

Las reglas comunes **no se reescriben en cada proyecto**: vienen empaquetadas en
`dev.sharedkernel:sharedkernel-archunit`. El módulo solo declara dónde mirar:

```java
@AnalyzeClasses(packages = "com.miproyecto")
class ArchitectureTest {

    @ArchTest
    static final ArchTests sharedRules = ArchTests.in(SharedKernelRules.class);
}
```

Tres líneas, y cada regla se reporta como un test independiente — así un fallo dice
*qué* regla se rompió, no solo que "la arquitectura falla".

Qué verifica hoy:

| Grupo | Reglas |
|---|---|
| `LayerRules` | El dominio no depende de Application/Infrastructure/Presentación; Application no depende de Infrastructure/Presentación; Infrastructure no conoce Presentación; el dominio no usa JDBC |
| `DomainPurityRules` | El dominio no loguea, no lee el reloj (`now()`, `currentTimeMillis`) y no usa aleatoriedad (`randomUUID`, `Math.random`) |
| `DddRules` | Domain Events, Commands, Queries y Value Objects son records; los VOs de `vos/` implementan `ValueObject`; los handlers se llaman `*CommandHandler` / `*QueryHandler` |

Las reglas usan patrones de paquete (`..domain..`, `..application..`), así que funcionan
sin configurar el paquete base — basta con respetar la estructura de carpetas de
`architecture.md`. El propio Shared Kernel queda excluido de las comprobaciones porque
es un anillo transversal disponible para todas las capas, por diseño.

Añadir reglas propias del proyecto es un campo más en la misma clase; las compartidas
llegan por el harness y no se editan localmente.

> Las reglas del artefacto **están testeadas**: cada una tiene un par de clases que la
> violan a propósito y un test que comprueba que la señala, indicando la clase culpable.
> Una regla que nunca falla da una confianza falsa, que es peor que no tenerla.

## Regla de oro

Los tests no modifican código de producción. Si algo no es testeable sin refactor,
**no se refactoriza solo para poder testearlo** — se señala como bloqueador y el
refactor va aparte, en su propio cambio.

## Pendiente / a definir más adelante

- Si los builders y fixtures de `support/` acaban necesitando estar disponibles también
  fuera de los tests (`java-test-fixtures`) o basta con dejarlos donde están.
- Confirmar umbrales de PIT una vez haya un primer agregado real con el que probarlos.
- Cómo se marcan explícitamente los tests de caracterización si en algún momento se
  incorpora código legacy (hoy no aplica: todo el desarrollo es greenfield).
