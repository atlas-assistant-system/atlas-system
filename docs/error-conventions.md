# Convenciones de emisión de errores en el dominio

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

Este documento amplía "El patrón Result" y el "Catálogo de errores de dominio" de
`ddd-conventions.md`: la forma exacta de `Error`/`Result`, cómo se define el catálogo
de errores de cada agregado, cuándo usar Guard clauses en vez de `Result`, y cómo un
error llega desde `Domain` hasta la respuesta HTTP en `Presentación`. Extraído y
adaptado a Java 25 desde `Error`, `Result`/`Result<T>`, `DomainErrors`,
`GuardException` y `ResultExtensions` de GeneFlow.ApiNet2.

## Los dos mecanismos, y cuándo usar cada uno

- **`Result.failure(...)`** — para cualquier fallo que puede ocurrir en el uso normal
  del sistema: dato inválido, permisos insuficientes, recurso no encontrado, conflicto
  de estado. Es el mecanismo por defecto en `Domain` y `Application`.
- **Excepción (`GuardException`, vía `Guard`)** — solo para violaciones de invariante
  que nunca deberían ocurrir si el código que llama es correcto. No son parte del flujo
  normal de negocio, son bugs.

Regla práctica: **si el fallo puede ocurrir por algo que hizo el usuario o por el estado
del sistema, es un `Result.failure`. Si solo puede ocurrir por un bug en el propio
código, es una excepción.**

## `Error`

Un `record` inmutable con código, mensaje y categoría (`ErrorType`), que es lo que
permite traducirlo a HTTP en `Presentación` sin que `Domain` sepa nada de HTTP.

```java
public record Error(String code, String message, ErrorType type) {

    public static final Error NONE = new Error("", "", ErrorType.NONE);

    public static Error validation(String code, String message) {
        return new Error(code, message, ErrorType.VALIDATION);
    }

    public static Error notFound(String code, String message) {
        return new Error(code, message, ErrorType.NOT_FOUND);
    }

    public static Error conflict(String code, String message) {
        return new Error(code, message, ErrorType.CONFLICT);
    }

    public static Error unauthorized(String code, String message) {
        return new Error(code, message, ErrorType.UNAUTHORIZED);
    }

    public static Error forbidden(String code, String message) {
        return new Error(code, message, ErrorType.FORBIDDEN);
    }

    public static Error failure(String code, String message) {
        return new Error(code, message, ErrorType.FAILURE);
    }

    public static Error unexpected(String code, String message) {
        return new Error(code, message, ErrorType.UNEXPECTED);
    }
}

public enum ErrorType {
    NONE, VALIDATION, NOT_FOUND, CONFLICT, UNAUTHORIZED, FORBIDDEN, FAILURE, UNEXPECTED
}
```

## `Result<T>`

GeneFlow modela `Result`/`Result<T>` con herencia (`Result<T> : Result`) porque en C#
es el patrón natural. En Java, un **sealed interface con dos implementaciones**
(`Success`/`Failure`) es más idiomático y aprovecha el pattern matching de `switch`
(disponible desde Java 21):

```java
public sealed interface Result<T> permits Result.Success, Result.Failure {

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    T value();
    Error error();

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static Result<Void> success() {
        return new Success<>(null);
    }

    static <T> Result<T> failure(Error error) {
        return new Failure<>(error);
    }

    record Success<T>(T value) implements Result<T> {
        public boolean isSuccess() { return true; }
        public Error error() {
            throw new IllegalStateException("Cannot access error of a successful result.");
        }
    }

    record Failure<T>(Error error) implements Result<T> {
        public boolean isSuccess() { return false; }
        public T value() {
            throw new IllegalStateException("Cannot access value of a failed result: " + error());
        }
    }
}
```

Uso habitual (el mismo estilo ya usado en `ddd-conventions.md` y `cqrs-conventions.md`):

```java
var titleResult = AppointmentTitle.create(command.title());
if (titleResult.isFailure()) {
    return Result.failure(titleResult.error());
}
var title = titleResult.value();
```

O, cuando conviene ser exhaustivo, con pattern matching sobre el `sealed interface`:

```java
var message = switch (result) {
    case Result.Success<Appointment> s -> "Scheduled: " + s.value().id();
    case Result.Failure<Appointment> f -> "Error: " + f.error().message();
};
```

`Result<Void>` (con `Result.success()` sin argumento) se usa para operaciones que no
devuelven valor, como en los métodos de comportamiento de un Aggregate Root que solo
mutan estado (ver `Appointment.cancel(...)` en `ddd-conventions.md`).

## Catálogo de errores por agregado

Una clase `<Aggregate>Errors` por agregado, con constantes para errores sin parámetros
y métodos estáticos para los que necesitan interpolar datos. Código con el formato
`"<Aggregate>.<ErrorName>"`.

```java
public final class AppointmentErrors {

    public static final Error TIME_SLOT_REQUIRED =
        Error.validation("Appointment.TimeSlotRequired", "A time slot is required.");

    public static final Error INSUFFICIENT_PERMISSIONS =
        Error.forbidden("Appointment.InsufficientPermissions", "You don't have permission to perform this action.");

    public static Error notFound(AppointmentId id) {
        return Error.notFound("Appointment.NotFound", "Appointment '" + id + "' was not found.");
    }

    private AppointmentErrors() { }
}
```

## Errores genéricos compartidos

Para no repetir "requerido"/"no encontrado"/"valor inválido" en cada agregado, Shared
Kernel expone una fábrica de errores genéricos que cualquier bc puede reutilizar antes
de tener (o en vez de tener) su propio catálogo:

```java
// sharedkernel/domain/results/CommonErrors.java
public final class CommonErrors {

    public static Error notFound(String entityName, Object id) {
        return Error.notFound(entityName + ".NotFound", entityName + " with ID '" + id + "' was not found.");
    }

    public static Error required(String fieldName) {
        return Error.validation("General.ValueIsRequired", "'" + fieldName + "' is required.");
    }

    public static Error invalid(String fieldName) {
        return Error.validation("General.InvalidValue", "'" + fieldName + "' has an invalid value.");
    }

    private CommonErrors() { }
}
```

Cuando un error es específico del dominio de un agregado (con su propio código
legible), va en `<Aggregate>Errors`. Cuando es un caso genérico sin matiz de negocio, se
usa `CommonErrors` directamente.

## Guard clauses y excepciones

GeneFlow implementa `Guard.Against.Null(...)` con *extension methods* (`Guard.Against`
devuelve un objeto vacío que las extension methods "decoran"), lo que le permite tener
**un fichero por guard**. Java no tiene extension methods, así que aquí son métodos
estáticos — pero eso no obliga a meterlos todos en una sola clase, y no se hace: una
clase única acaba siendo un cajón de sastre en cuanto aparecen rangos, longitudes,
formatos y fechas.

Los guards se agrupan por **el tipo que protegen**, un fichero por familia:

| Clase | Guards |
|---|---|
| `ObjectGuard` | `notNull` |
| `StringGuard` | `notBlank`, `notLongerThan`, `matches` |
| `NumberGuard` | `notNegative`, `positive`, `inRange` |
| `CollectionGuard` | `notEmpty`, `noNullElements` |
| `TimeGuard` | `notInFuture`, `notInPast` |

```java
public final class StringGuard {

    private StringGuard() {}

    public static String notBlank(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            throw GuardException.forParameter(parameterName, "cannot be blank");
        }

        return value;
    }
}
```

Reglas de la familia:

- **Dónde va un guard nuevo lo decide el tipo de su primer parámetro.** Si no encaja en
  ninguna de las cinco, es que hace falta una familia nueva, no que se meta en la más
  parecida.
- Todos **devuelven el valor**, para poder escribir `this.id = ObjectGuard.notNull(id, "id")`.
- Todos lanzan `GuardException.forParameter(...)`, que conserva el nombre del parámetro
  para el log — nunca para la respuesta al cliente (ver la regla de `UNEXPECTED`).
- `TimeGuard` recibe el instante actual **como parámetro** (`notInFuture(value, now, ...)`),
  nunca lee el reloj: es la misma regla de `ddd-conventions.md`, ahora impuesta por la
  firma.

`DomainException`/`GuardException` extienden `RuntimeException` (no checked): una
checked exception en Java obligaría a declarar `throws` en cada método intermedio, lo
que sugeriría que el llamador debería recuperarse de ella — justo lo contrario de lo
que representa una violación de invariante.

```java
// sharedkernel/domain/exceptions/DomainException.java
public class DomainException extends RuntimeException {
    public DomainException(String message) { super(message); }
    public DomainException(String message, Throwable cause) { super(message, cause); }
}

// sharedkernel/domain/exceptions/GuardException.java
public final class GuardException extends DomainException {

    private final String parameterName;

    private GuardException(String parameterName, String message) {
        super(message);
        this.parameterName = parameterName;
    }

    public static GuardException forParameter(String parameterName, String violation) {
        return new GuardException(parameterName, "'" + parameterName + "' " + violation + ".");
    }

    public String parameterName() { return parameterName; }
}
```

## De Domain a la respuesta HTTP

`Application` **nunca transforma** el `Result` que recibe de `Domain` — lo propaga tal
cual hasta `Presentación`. Es `Presentación` quien traduce `ErrorType` al código HTTP,
igual que `ResultExtensions.ToHttpResult` en GeneFlow, aquí sobre `jdk.httpserver`:

```java
private static int statusFor(ErrorType type) {
    return switch (type) {
        case VALIDATION -> 400;
        case NOT_FOUND -> 404;
        case CONFLICT -> 409;
        case UNAUTHORIZED -> 401;
        case FORBIDDEN -> 403;
        case FAILURE -> 400;
        case UNEXPECTED -> 500;
        case NONE -> 200;
    };
}
```

> GeneFlow trata `Failure` y `Unexpected` igual (ambos caen al `default: BadRequest`
> del `switch` en `ResultExtensions`). Aquí se separan explícitamente: `Unexpected` es
> un fallo del sistema, no una respuesta de negocio, y merece un 500 — no un 400.

Esa traducción no se reescribe en cada proyecto: vive en `HttpStatuses.forErrorType(...)`
del Shared Kernel, y su test recorre **los ocho** valores de `ErrorType`, así que añadir
uno nuevo sin decidir su status rompe el build.

## El cuerpo de error: `ApiError` (decidido)

```java
public record ApiError(int status, String code, String message, String correlationId, List<FieldError> fieldErrors)
public record FieldError(String field, String message)
```

Se construye siempre con las factorías, nunca a mano:

```java
ApiError.from(result.error())                    // fallo de negocio
ApiError.from(result.error(), fieldErrors)       // validacion, con detalle por campo
ApiError.from(thrown)                            // excepcion no controlada
```

`fieldErrors` nunca es `null`: si no hay detalle por campo, es una lista vacía.

## La regla que no se negocia: `UNEXPECTED` no filtra nada

Cuando el `ErrorType` es `UNEXPECTED`, `ApiError` **sustituye el mensaje** por uno
genérico antes de que salga del proceso. Da igual que el `Error` original dijera
`"Connection to /var/db/agenda.sqlite refused as user admin"`: al cliente le llega
`"An unexpected error occurred."`.

El **código** sí se conserva, porque es un identificador estable pensado para
comunicarse; el mensaje interno no lo es. Y el `correlationId` viaja en la respuesta,
así que un usuario puede reportar "me ha fallado, id a3f9c1" y esa cadena aparece
literalmente en la línea de log que sí tiene el stack trace completo. Esa es la razón de
ser del contexto de correlación de `logging-conventions.md`.

Hay tests que lo verifican con cadenas que contienen rutas, credenciales y nombres de
parámetro, comprobando que **ninguna** aparece en la respuesta.

## Cómo se traduce una excepción

`ExceptionTranslator.translate(...)`:

| Excepción | `ErrorType` | HTTP | Mensaje al cliente |
|---|---|---|---|
| `FormatException` | `VALIDATION` | 400 | El suyo (habla de formato, no de internals) |
| Cualquier otra | `UNEXPECTED` | 500 | Genérico |

`FormatException` es la única excepción con traducción propia porque significa
**entrada mal formada**, y devolver 500 por una URL malformada sería mentir. Todo lo
demás —incluida una `GuardException` escapada, que por definición es un bug (ver "Los
dos mecanismos" arriba)— es un 500 sin detalles.

El manejador global de la capa de Presentación queda entonces reducido a: capturar,
**loguear** (con el stack trace, ver `logging-conventions.md`) y serializar el
`ApiError`. El Shared Kernel no serializa: entrega el record y es Presentación quien lo
convierte a JSON con Jackson jr.

## Pendiente / a definir más adelante
- Si `CommonErrors` vive en `sharedkernel/domain/results/` o se fusiona con `Error`
  directamente.
- Nivel de detalle de los mensajes de error expuestos al cliente en errores
  `UNEXPECTED` (GeneFlow oculta el detalle en producción y lo muestra en desarrollo) —
  confirmar si aplica igual aquí.
