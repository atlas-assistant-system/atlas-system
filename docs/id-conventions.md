# Convenciones de tipado de IDs

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

Este documento amplía la sección "Identificadores fuertemente tipados" de
`ddd-conventions.md` con el patrón completo: dos variantes de ID, cómo se generan y
cómo se adaptan a Java 25 (sin la clase base que usa el proyecto de referencia,
GeneFlow.ApiNet2, en C#).

## Por qué IDs fuertemente tipados

Envolver cada identificador en su propio tipo (`AppointmentId`, no `long` a secas)
impide que el compilador acepte pasar un `UserId` donde se espera un `AppointmentId`,
aunque ambos envuelvan un `long`. El coste es una clase/record más por agregado; el
beneficio es una categoría entera de bugs que deja de ser posible.

## Dos variantes

### 1. ID con prefijo legible — para Aggregate Roots

Se usa siempre que el ID puede acabar expuesto fuera del sistema (URLs, logs, soporte al
usuario, referencias entre bounded contexts). Formato: una letra de prefijo + número
con ceros a la izquierda — ej. `A00000007` para una cita, `U00000042` para un usuario.
Es el equivalente Java del `PrefixedId<TId>` de GeneFlow, legible y fácil de teclear o
mencionar en un log, a diferencia de un UUID.

### 2. UUID simple — para entidades internas (no raíz)

Las entidades que viven dentro de un agregado pero nunca se exponen sueltas (ej. un
asistente dentro de una cita) usan un `UUID` aleatorio envuelto en un `record` simple.
No necesitan ser legibles porque nunca aparecen en una URL ni las pide un usuario por
teléfono.

```java
public record AttendeeId(UUID value) implements SingleValueObject<UUID> {
    public static AttendeeId newId() {
        return new AttendeeId(UUID.randomUUID());
    }
}
```

## El patrón `PrefixedId` en Java

GeneFlow implementa esto con una clase base abstracta (`PrefixedId<TId>`) porque C# no
tiene records con esta semántica hasta hace poco. Un `record` de Java **no puede
heredar de una clase** (implícitamente ya extiende `java.lang.Record`), así que el
mismo comportamiento se comparte por **composición**: una utilidad estática en Shared
Kernel con el formato/parseo, que cada ID concreto invoca.

```java
// sharedkernel/domain/types/PrefixedIds.java
public final class PrefixedIds {

    private PrefixedIds() { }

    public static String format(char prefix, int numericLength, long value) {
        if (value < 0) {
            throw new GuardException("ID value must be non-negative.");
        }
        long maxValue = (long) Math.pow(10, numericLength) - 1;
        if (value > maxValue) {
            throw new GuardException("ID value exceeds maximum (" + maxValue + ").");
        }
        return prefix + String.format("%0" + numericLength + "d", value);
    }

    public static long parse(char prefix, int numericLength, String text) {
        int expectedLength = 1 + numericLength;
        if (text == null || text.length() != expectedLength) {
            throw new FormatException("ID must be " + expectedLength + " characters long.");
        }
        if (text.charAt(0) != prefix) {
            throw new FormatException("ID must start with '" + prefix + "'.");
        }
        try {
            return Long.parseLong(text.substring(1));
        } catch (NumberFormatException e) {
            throw new FormatException("ID must contain only digits after the prefix.");
        }
    }

    public static Optional<Long> tryParse(char prefix, int numericLength, String text) {
        try {
            return Optional.of(parse(prefix, numericLength, text));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
```

Cada ID concreto de aggregate root es un `record` de una línea que fija su propio
prefijo y longitud, y delega en `PrefixedIds`:

```java
// domain/appointments/AppointmentId.java
public record AppointmentId(long value) {

    private static final char PREFIX = 'A';
    private static final int NUMERIC_LENGTH = 8;

    public AppointmentId {
        PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value); // valida el rango; lanza si es inválido
    }

    public static AppointmentId of(long value) {
        return new AppointmentId(value);
    }

    public static AppointmentId parse(String text) {
        return new AppointmentId(PrefixedIds.parse(PREFIX, NUMERIC_LENGTH, text));
    }

    public static Optional<AppointmentId> tryParse(String text) {
        return PrefixedIds.tryParse(PREFIX, NUMERIC_LENGTH, text).map(AppointmentId::new);
    }

    @Override
    public String toString() {
        return PrefixedIds.format(PREFIX, NUMERIC_LENGTH, value);
    }
}
```

> **Por qué `AppointmentId` no declara `SingleValueObject<Long>`:** su componente es un
> `long` primitivo, y un accessor `long value()` no satisface `Long value()` — Java no
> aplica boxing al implementar interfaces, así que no compilaría. Se mantiene el
> primitivo (sin coste de boxing en el dominio) y sin la interfaz: su representación
> uniforme hacia fuera ya es el String formateado (`toString()`/`parse()`), no el número.
> Los IDs con componente de tipo referencia (como `AttendeeId` con `UUID`) sí la declaran.

> El compact constructor (`public AppointmentId { ... }`) valida el **formato** (rango
> numérico, longitud) — es un Guard, no una regla de negocio. Si algún día se quisiera
> devolver `Result` en vez de lanzar, habría que separar la validación de formato de la
> construcción, igual que se hace con los Value Objects — de momento se trata como
> invariante de tipo, no como fallo de negocio esperable (ver "El patrón Result" en
> `ddd-conventions.md`).

## Generación de la parte numérica

La parte numérica es secuencial, no aleatoria — necesita algo que reparta el siguiente
valor sin colisiones. GeneFlow usa Redis `INCR`; aquí, sin Redis, se propone una tabla
`sequences` dentro del propio fichero SQLite de cada bounded context:

```sql
CREATE TABLE IF NOT EXISTS sequences (
    name  TEXT PRIMARY KEY,
    value INTEGER NOT NULL DEFAULT 0
);
```

Puerto genérico en Shared Kernel (es infraestructura transversal, igual que en
GeneFlow):

```java
// sharedkernel/infrastructure/SequenceGenerator.java
public interface SequenceGenerator {
    long next(String sequenceName);
    long current(String sequenceName);
}
```

Implementación SQLite (`infrastructure/common/` o `infrastructure/<bc>/persistence/`,
según si se comparte una única tabla de secuencias por bc o una por proyecto —
pendiente de decidir):

```sql
INSERT INTO sequences (name, value) VALUES (?, 1)
ON CONFLICT(name) DO UPDATE SET value = value + 1
RETURNING value;
```

SQLite es de escritor único por fichero, así que este `UPDATE ... RETURNING` dentro de
una transacción ya es atómico sin necesidad de bloqueo adicional — no hace falta nada
parecido a `Redis INCR`.

Esto es lo que hay detrás de `appointments.nextId()` en el ejemplo de
`cqrs-conventions.md`: el repositorio pide el siguiente valor al `SequenceGenerator` y
construye el ID tipado con él, antes de construir el agregado.

## Reglas

- **Aggregate Root → ID con prefijo.** Siempre, sin excepción, aunque hoy no se prevea
  exponerlo directamente — es barato de mantener y caro de añadir después.
- **Entidad interna (no raíz) → UUID.** Nunca se expone suelta, no necesita ser legible.
- **Referencias entre agregados** usan el tipo de ID del agregado referenciado (ej.
  `Appointment` guarda un `UserId`, no inventa un tipo propio para "usuario
  organizador").
- Cada bounded context define su propio prefijo, único **dentro del proyecto** (no hace
  falta que sea único entre proyectos/módulos — son repositorios independientes, nunca
  comparten base de datos).

## Pendiente / a definir más adelante

- Catálogo real de prefijos por bounded context, según se vayan creando (aún no hay
  ningún módulo real construido).
- Longitud numérica por defecto (GeneFlow usa 8 dígitos) — confirmar si se adopta igual
  o se ajusta por volumen esperado de cada bc.
- Si la tabla `sequences` vive una vez por fichero SQLite (compartida entre los
  aggregate roots de ese bc) o se separa distinto si un bc tiene varios agregados con
  necesidades de secuencia independientes.
