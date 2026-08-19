# Convenciones de enumeraciones (Smart Enums)

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

GeneFlow.ApiNet2 modela sus valores de dominio con un conjunto cerrado y conocido
(`StudyStatus`, `StudyRole`, `ResearchField`) con una clase base propia,
`Enumeration<TEnum>`, en vez de con el `enum` nativo de C#. La razón: un `enum` de C#
es solo un entero con nombre — no admite campos, constructores ni métodos de
instancia, así que cualquier comportamiento o metadato extra exige una clase que lo
simule.

**El `enum` de Java no tiene esa limitación.** Un `enum` de Java es una clase real: puede
tener campos, constructores, métodos de instancia e incluso un cuerpo distinto por cada
constante. Por eso aquí no hace falta ninguna clase base tipo `Enumeration<TEnum>` — se
usa el `enum` nativo directamente, y se obtiene lo mismo (o más) con menos código.

## Regla base

Cualquier valor de dominio con un **conjunto cerrado y conocido en tiempo de
compilación** (un estado, un rol, una categoría de una lista fija) se modela como
`enum`, nunca como `String`/`int` sueltos. Guardar un estado como `String` libre
("scheduled", "Scheduled", "SCHEDULED"...) es la misma anemia de la que ya habla
`rich-domain-conventions.md`, solo que a nivel de tipo en vez de a nivel de método.

## Enum simple con comportamiento

Igual que `StudyStatus.CanTransitionTo(...)` en GeneFlow, pero como `switch`
expression sobre `this` — exhaustivo: si se añade un valor nuevo al enum y se olvida
un `case`, el compilador de Java lo marca en vez de fallar en tiempo de ejecución
(a diferencia del `switch` con tuplas de GeneFlow, que no tiene ese chequeo).

```java
public enum AppointmentStatus {
    SCHEDULED,
    CONFIRMED,
    CANCELLED,
    COMPLETED;

    public boolean canTransitionTo(AppointmentStatus target) {
        return switch (this) {
            case SCHEDULED -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == COMPLETED || target == CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
    }

    public boolean isActive() {
        return this == SCHEDULED || this == CONFIRMED;
    }
}
```

## Enum con datos adicionales por constante

Igual que `StudyStatus.DisplayName` en GeneFlow, con constructor y campo — sin
necesitar `Id`/`Name` genéricos ni reflexión para construir la lista de valores
(`Enumeration<TEnum>.GetAll()` los descubre por reflexión; en Java, `values()` ya viene
gratis con cualquier enum).

```java
public enum ResearchField {
    GENOMICS("Genomics"),
    PROTEOMICS("Proteomics"),
    BIOINFORMATICS("Bioinformatics");

    private final String displayName;

    ResearchField(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
```

## Enum con comportamiento distinto por constante

Esto no tiene equivalente directo en un `enum` de C# sin recurrir de nuevo a una clase
base con `switch`: cada constante de un `enum` de Java puede llevar su **propio cuerpo**,
sobrescribiendo un método abstracto. Útil cuando el comportamiento varía tanto entre
valores que un único `switch` centralizado empezaría a oler a lógica condicional
repetida (la misma señal de alerta de `rich-domain-conventions.md`, aplicada a enums).

```java
public enum AttendeeRole {
    ORGANIZER {
        @Override public boolean canCancelAppointment() { return true; }
        @Override public boolean canInviteOthers() { return true; }
    },
    GUEST {
        @Override public boolean canCancelAppointment() { return false; }
        @Override public boolean canInviteOthers() { return false; }
    };

    public abstract boolean canCancelAppointment();
    public abstract boolean canInviteOthers();
}
```

Usar esta forma solo cuando de verdad hay comportamiento por constante que lo
justifique — para un par de métodos booleanos simples, un `switch` como en
`AppointmentStatus` de arriba es más legible.

## Enum vs Value Object

Dos cosas distintas que a veces se confunden:

- **Enum** — el conjunto de valores posibles es cerrado y se conoce al escribir el
  código (`AppointmentStatus` siempre tendrá exactamente esos cuatro valores).
- **Value Object** — el valor concreto no se conoce hasta tiempo de ejecución, pero
  tiene que cumplir reglas de validación (`AppointmentTitle`, `TimeSlot`). Ver
  `ddd-conventions.md`.

Si en algún punto un "enum" empieza a necesitar que el usuario pueda añadir sus propios
valores (categorías definidas por el usuario, por ejemplo), ha dejado de ser un enum y
es un Value Object respaldado por una tabla — señal para migrarlo, no para forzarlo.

## Persistencia: por nombre, nunca por posición

Sin ORM, el mapeo `ResultSet ↔ enum` es manual — la regla no negociable es guardar
`name()` (el nombre de la constante, `"SCHEDULED"`), **nunca** `ordinal()` (la posición
numérica). El `ordinal()` cambia si alguien reordena o inserta un valor en medio del
enum; el `name()` no, salvo que alguien lo renombre explícitamente (y eso ya se nota en
el propio código).

```java
// Escribir
statement.setString(paramIndex, appointment.status().name());

// Leer
var status = AppointmentStatus.valueOf(resultSet.getString("status"));
```

`AppointmentStatus.valueOf(...)` lanza `IllegalArgumentException` si el valor
almacenado no coincide con ninguna constante — tratar eso como un dato corrupto (bug o
migración incompleta), no como un fallo de negocio esperable (ver "Los dos mecanismos,
y cuándo usar cada uno" en `error-conventions.md`).

## Serialización JSON

Jackson jr (el stack ya decidido en `stack.md`) serializa un `enum` de Java como su
`name()` por defecto — coincide exactamente con la convención de persistencia de
arriba, sin necesitar ningún mapeo adicional.

## Simplificaciones respecto al proyecto de referencia

- **Igualdad, `hashCode`, comparación**: `Enumeration<TEnum>` en GeneFlow reimplementa
  `Equals`/`GetHashCode`/`CompareTo` a mano porque C# no se lo da gratis. Un `enum` de
  Java ya tiene igualdad por identidad correcta (`==` funciona) y `Comparable` basado en
  el orden de declaración — no hay nada que reimplementar.
- **`Id` numérico**: GeneFlow da a cada valor un `Id` entero además del `Name`, porque
  su API pública ya lo expone así (`ResearchFieldDto.Id`) y no puede romper contratos
  existentes. Aquí, al ser un proyecto nuevo sin esa carga histórica, **no se añade un
  `Id` numérico por defecto** — se usa `name()` como identificador natural. Si en algún
  momento hace falta un id numérico estable (ej. para ordenar un desplegable de forma
  específica), se añade entonces, de forma explícita, no por costumbre.

## Naming

- Tipo en singular: `AppointmentStatus`, no `AppointmentStatuses`.
- Constantes en `SCREAMING_SNAKE_CASE` (convención estándar de Java), a diferencia del
  `PascalCase` que usa GeneFlow en C# (`StudyStatus.Draft` → `AppointmentStatus.DRAFT`).

## Ubicación

`domain/<bc>/enums/`, según la estructura de carpetas ya definida en `architecture.md`.

## Señal de alerta añadida a `rich-domain-conventions.md`

Lógica condicional repetida en varios sitios que compara contra los valores de un mismo
enum (`if (status == SCHEDULED || status == CONFIRMED)` repetido en dos Command
Handlers distintos) es la misma señal de dominio anémico que ya describe ese
documento — la comprobación debería vivir como método del propio enum
(`status.isActive()`), no repetirse fuera de él.

## Pendiente / a definir más adelante

- Si aparece algún caso real que sí necesite un `Id` numérico estable (más allá del
  `name()`), definir entonces el criterio para añadirlo — no antes.
