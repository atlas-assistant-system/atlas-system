# Bounded context: `nutrition`

Diseño cerrado del contexto de nutrición. Este documento es la fuente de verdad del
modelo, los casos de uso y las decisiones tomadas — el código se escribe a partir de aquí,
no al revés.

Estado: **el ciclo 1 completo y cableado**, con sus cuatro capas y su pestaña en `core`. El
ciclo 2 —seguimiento de peso— se implementó y **se retiró**; el porqué está al final.

## Qué resuelve

Saber si vas donde querías ir, sin abrir una app de dieta. El espejo responde a cuatro
preguntas, en este orden de importancia:

1. ¿Cuánto me queda hoy de calorías y de cada macro?
2. ¿Cómo ha ido la última semana?
3. ¿Qué me metí exactamente ayer, o el jueves pasado?

Es **Fitia sin alimentos**. No hay base de datos nutricional, ni códigos de barras, ni
recetas, ni pesar la comida. Se registran los números que ya sabes.

## Decisiones de alcance

**Los números del plan los fijas tú.** Ni Mifflin-St Jeor, ni TDEE, ni factores de
actividad. El plan guarda lo que decides: peso de partida, peso objetivo y los macros
diarios. Calcularlos exige altura, edad, sexo y nivel de actividad —cuatro datos más que
mantener— para producir una estimación que ibas a sobreescribir de todas formas. Es la
misma decisión que en `economy` con los presupuestos: **los decides tú mirando el dato**.

**Aquí sí se escribe desde el espejo.** `economy` es de solo consulta porque los
movimientos son el reflejo del banco: nacen fuera. Nada de este contexto nace fuera. No
hay banco de comidas ni báscula conectada; tú decides las calorías y tú te pesas. Así que
la pestaña **Nutrición** tiene formularios, con el mismo patrón de escritura inline que
`routines` ya usa.

**La API queda abierta igualmente.** Los endpoints son REST normales sobre los mismos
comandos, así que un Atajo de iOS que registre la cena desde el móvil funciona sin trabajo
extra. La vista escucha su SSE y se refresca sola, como la de `economy`.

**Las calorías se teclean, no se derivan.** Ni en el consumo ni en el plan: son un dato
propio, al lado de los macros y sin obligación de cuadrar con ellos.

El diseño original hacía lo contrario —`4·proteína + 4·carbos + 9·grasa`, una regla y un
sitio— y se cambió a propósito. El argumento que lo tumbó es el uso real: **lo que sabes de
lo que te comes suele ser la cifra de la etiqueta**, no el desglose; el alcohol no es
ninguno de los tres macros; y dos etiquetas con los mismos gramos declaran kcal distintas
por el redondeo y la fibra. Derivarlas obligaba a inventarse los macros para poder apuntar
algo, que es peor que aceptar dos números.

Lo que se pierde: calorías y macros **pueden discrepar**, y nadie los concilia. Es
deliberado — mandan las calorías tecleadas en todo lo que las use (totales del día,
histórico, rango). Los macros son informativos y se suman por separado.

Un consumo exige **calorías mayores que cero**; los macros pueden ir a cero. Así
`{"calories":150,"note":"una cerveza"}` es un registro válido.

**Sin alimentos, sin comidas con nombre.** No hay `Food`, ni `Meal`, ni desayuno/comida/
cena como entidades. Una entrada de consumo es una nota libre opcional y unos macros. Ese
es exactamente el recorte que separa esto de Fitia y lo que lo hace sostenible.

**Un plan activo a la vez.** Redefinirlo archiva el anterior; no se borra, porque el
histórico de un día de hace tres meses necesita saber contra qué cuota se estaba midiendo
entonces.

**Sin ejercicio, sin agua, sin micronutrientes.** Las calorías quemadas exigirían
integrarse con un reloj o creerse una estimación; el agua y los micros son tres cuotas más
que nadie de la lista de arriba necesita para responderse.

## Los ciclos

El contexto tiene dos agregados vivos. `Plan` es la base: el consumo diario se mide
**contra el plan**, y no tiene nada contra qué medirse hasta que exista.

| Ciclo | Agregados | Estado |
|---|---|---|
| 1 | `Plan` + `Intake` — plan y consumo diario | diseñado abajo, en detalle |
| 2 | `WeighIn` — evolución de peso y progreso | implementado y retirado |

---

# Ciclo 1 — Plan y consumo diario

## El modelo de dominio

**Son dos agregados, no un `Plan` con los consumos dentro.** El total del día no es una
invariante que necesite consistencia transaccional: es una suma. Meter los `Intake` como
entidades del plan obligaría a cargar meses de registros cada vez que apuntas una comida,
para validar algo que nadie puede violar. Los totales del día y la serie histórica son
proyecciones y viven en el read model.

**Tampoco existe una entidad `Day`.** Un día es un filtro por fecha sobre `Intake`, igual
que un mes es un filtro por fecha sobre `Movement`. Crear la fila del día por adelantado
obliga a decidir quién la crea y qué significa un día sin filas.

```
domain/nutrition/
    PlanId.java                         prefijo 'N'
    Plan.java                           raíz de agregado
    PlanErrors.java
    IntakeId.java                       prefijo 'I'
    Intake.java                         raíz de agregado
    IntakeErrors.java
    NutritionErrors.java                errores de los VOs que comparten los dos agregados
    enums/Goal.java                     LOSE | MAINTAIN | GAIN, derivado
    enums/PlanStatus.java               ACTIVE | ARCHIVED
    vos/Weight.java                     gramos
    vos/Macros.java                     proteína + carbos + grasa, en gramos
    vos/Calories.java                   kcal enteras, derivadas de Macros
    vos/DayTotals.java                  consumido vs cuota del plan
    vos/IntakeNote.java
    events/PlanDefinedEvent.java
    events/PlanAdjustedEvent.java
    events/PlanArchivedEvent.java
    events/IntakeRecordedEvent.java
    events/IntakeCorrectedEvent.java
    events/IntakeDeletedEvent.java
```

Prefijos de ID ya ocupados: `A` citas, `G` gate de autenticación, `B` perfil biométrico,
`L` reto de vivacidad, `S` sesión, `R` rutina, `M` movimiento, `P` presupuesto, `O`
objetivo de ahorro. **`N`, `I` y `W` están libres**, y salen de `PrefixedIds` del Shared
Kernel según [id-conventions.md](id-conventions.md).

### `Weight`

Peso en **gramos como `int`**, nunca `double`. El mismo argumento que `Money`: 78,4 kg no
se representa exacto en binario y el error se acumula solo.

```java
public record Weight(int grams) implements ValueObject {
    public static Weight ofGrams(int grams)          // 20 000 .. 400 000
    public static Weight ofKilograms(BigDecimal kg)
    public int gramsTo(Weight other)                 // diferencia con signo, no un Weight
    public BigDecimal toKilograms()
}
```

Una diferencia de pesos **no es un `Weight`**: puede ser negativa y puede caer fuera del rango
válido. `gramsTo` devuelve gramos con signo, y así no existe un `Weight` inválido construible.

Los límites no son decoración: un peso de 4 kg o de 900 kg es un dedo que ha resbalado, y
`Goal` derivaría lo contrario de lo que querías. Rechazarlo en el borde es más barato que un
caso de uso para corregirlo.

### `Macros`

```java
public record Macros(int protein, int carbs, int fat) implements ValueObject {
    public static Result<Macros> create(int protein, int carbs, int fat)   // todos >= 0
    public Macros plus(Macros other)
    public boolean isZero()
}
```

Gramos enteros, mayores o iguales que cero, y **los tres pueden ser cero a la vez**: un día sin
registros consume cero, y apuntar solo las kcal de algo también. `Macros` ya no sabe nada de
calorías. `plus` es lo que usa el read model conceptualmente; en SQLite es un `SUM` por
columna.

### `Calories`

```java
public record Calories(int kcal) implements ValueObject {
    public static Result<Calories> create(int kcal)  // >= 0
    public Calories plus(Calories other)
    public int remainingFor(Calories target)         // con signo: negativo si te pasaste
    public int percentageOf(Calories total)
    public Calories lowerBound()                     // el objetivo menos un 10%
    public Calories upperBound()                     // el objetivo mas un 10%
    public boolean covers(Calories consumed)         // dentro del rango
}
```

**Se persiste y se acepta por la API**, porque es un dato que tecleas. Tiene factoría
`create(...)` con `Result`, como cualquier VO que recibe algo de fuera.

Además del `percentageOf` que la UI necesita, lleva el **rango de tolerancia** del objetivo:
`lowerBound()` y `upperBound()` son el ±10% y `covers(...)` dice si un consumo cae dentro. Es
lo que pintan las dos marcas del arco y lo que colorea los puntos de la semana, y por eso es
una regla del dominio y no una cuenta del JS.

### `Goal`

Smart enum, persistido por nombre según [enum-conventions.md](enum-conventions.md):

```java
public enum Goal {
    LOSE, MAINTAIN, GAIN;

    public static Goal of(Weight start, Weight target)   // tolerancia de 500 g
    public String label();                               // "Perder peso", ...
    public boolean isReached(Weight current, Weight target);
}
```

`of(...)` es **la única definición del objetivo en todo el sistema**, igual que
`MovementKind.of(...)` lo es del signo en `economy`. El plan no guarda un campo `goal`
elegible: dos pesos y un enum derivado no pueden contradecirse, un campo suelto sí ("bajar
de peso" con objetivo 5 kg por encima del actual). La tolerancia de 500 g es lo que hace
que "mantenerme" no dependa de acertar el gramo.

`isReached` decide el sentido de la comparación (`<=` si adelgazas, `>=` si engordas) —
esa es la invariante que justifica el enum en vez de un `if` repartido por los servicios.

### `Plan`

```java
public final class Plan extends AggregateRoot<PlanId> {

    public static Result<Plan> define(
        PlanId id, Weight startWeight, Weight targetWeight,
        Calories dailyCalories, Macros dailyMacros, LocalDate startedOn, Instant now)

    public Result<Void> adjust(
        Calories dailyCalories, Macros dailyMacros, Weight targetWeight, Instant now)
    public Result<Void> archive(Instant now)

    public Goal goal()                  // derivado de startWeight y targetWeight
}
```

Invariantes que hace cumplir:

- Los macros diarios no son cero. Un plan sin cuota no es un plan.
- El peso objetivo y el de partida están dentro de los límites de `Weight`.
- La fecha de inicio no está en el futuro.
- Un plan archivado no admite ajustes.

**El peso de partida no se ajusta.** Es un hecho: lo que pesabas cuando empezaste. Si te
equivocaste al teclearlo, defines un plan nuevo — el anterior se archiva y queda tal cual
fue. Permitir moverlo convierte el objetivo en un número que puedes maquillar hacia atrás.

### `Intake`

```java
public final class Intake extends AggregateRoot<IntakeId> {

    public static Result<Intake> record(
        IntakeId id, Calories calories, Macros macros, Optional<IntakeNote> note,
        LocalDate consumedOn, LocalDate today, Instant now)

    public Result<Void> correct(
        Calories calories, Macros macros, Optional<IntakeNote> note, Instant now)
    public Result<Void> delete(Instant now)
}
```

Invariantes:

- Las calorías son mayores que cero. Los macros pueden ser todos cero: apuntar solo las
  kcal de una cerveza es un registro legítimo.
- La fecha de consumo no está en el futuro. Se apunta lo que ya has comido; planificar la
  cena de mañana es otro contexto y está fuera de alcance.
- Un consumo borrado no admite más cambios.
- La fecha **no se corrige**: apuntar el desayuno en el día equivocado se arregla borrando
  y volviendo a registrar. Mover una entrada entre días cambia dos totales a la vez y no
  hay ninguna de las cuatro preguntas de arriba que lo necesite.

**El `Intake` no conoce el `Plan`.** No lleva `planId`, no valida contra la cuota y no se
entera de si te has pasado. Es un hecho registrado; el juicio ("te quedan 400 kcal") es
una proyección que cruza los dos, y vive en `DayTotals`. Atarlos obligaría a cargar el
plan para apuntar una manzana y dejaría los consumos huérfanos al archivar el plan.

### `DayTotals`

```java
public record DayTotals(
    Macros consumedMacros, Calories consumedCalories,
    Macros targetMacros, Calories targetCalories) implements ValueObject {

    public int remainingProtein()       // y remainingCarbs, remainingFat
    public int remainingCalories()
    public int caloriePercentage()
    public Calories lowerTarget()       // y upperTarget
    public boolean isWithinRange()
    public boolean isOverBudget()
}
```

Es donde vive la única resta que le importa a la persona que mira el espejo, con su test: que
pasarse de proteína y quedarse corto de grasa el mismo día se muestre como dos cosas distintas
y no como un único "te has pasado". Por eso lo que queda **se expone por macro y no como unos
`Macros`** — un `Macros` no admite negativos.

Guarda solo los dos lados y deriva el resto en sus accessors: no hay estado que pueda
desincronizarse. No hay servicio de dominio detrás, a diferencia de `BudgetPace` en `economy`:
allí el servicio existe porque proyectar el gasto del mes es aritmética con criterio propio;
aquí es una resta, y una clase para envolverla no aporta nada.

## Capa de aplicación

Seis comandos y cinco consultas, con la estructura de carpeta por caso de uso
([cqrs-conventions.md](cqrs-conventions.md)).

```
application/nutrition/
    commands/defineplan/            DefinePlanCommand + Handler
    commands/adjustplan/            AdjustPlanCommand + Handler
    commands/archiveplan/           ArchivePlanCommand + Handler
    commands/recordintake/          RecordIntakeCommand + Handler
    commands/correctintake/         CorrectIntakeCommand + Handler
    commands/deleteintake/          DeleteIntakeCommand + Handler
    queries/getactiveplan/          GetActivePlanQuery + Handler
    queries/getday/                 GetDayQuery + Handler
    queries/listintakes/            ListIntakesQuery + Handler
    queries/listdays/               ListDaysQuery + Handler
    queries/getintake/              GetIntakeQuery + Handler
    queries/Period.java             rango por defecto: los últimos 7 días
    dto/                            PlanDto, IntakeDto, DayDto, DaySummaryDto, MacrosDto
    mappers/NutritionMapper.java
    ports/PlanRepository.java       + findActive(), que usan los tres comandos de plan
    ports/PlanReadModel.java
    ports/IntakeRepository.java
    ports/IntakeReadModel.java
    ports/DayConsumption.java       lo que devuelve el GROUP BY por día
    ports/NutritionUnitOfWork.java
```

El periodo por defecto son **los últimos siete días**, no el mes natural como en `economy`: la
pregunta que se hace uno mirando el espejo es "¿cómo ha ido la semana?", no "¿cómo va el mes?".

`MacrosDto` lleva siempre `calories` junto a los tres gramos, derivadas por el mapper. Aparece
tres veces en `DayDto` —consumido, objetivo y restante— y el restante es el único de los tres
que admite negativos.

Los casos de uso, en la forma en que se usan de verdad:

| Caso de uso | Comando / consulta | Qué hace |
|---|---|---|
| Definir mi plan | `DefinePlanCommand` | archiva el activo si lo hay y define el nuevo, en la misma transacción |
| Ajustar la cuota | `AdjustPlanCommand` | cambia macros diarios y/o peso objetivo del plan activo |
| Abandonar el plan | `ArchivePlanCommand` | lo archiva sin definir otro |
| Apuntar lo que he comido | `RecordIntakeCommand` | macros + nota opcional + fecha (por defecto hoy) |
| Corregir lo que apunté | `CorrectIntakeCommand` | macros y nota |
| Borrar lo que apunté | `DeleteIntakeCommand` | |
| Ver mi plan | `GetActivePlanQuery` | plan activo con `goal` y `dailyCalories` derivados |
| ¿Cuánto me queda hoy? | `GetDayQuery(date)` | `DayDto`: consumido, objetivo, restante y sus entradas |
| ¿Cómo ha ido la semana? | `ListDaysQuery(from, to)` | un `DaySummaryDto` por día con total y % de la cuota |
| ¿Qué comí el jueves? | `ListIntakesQuery(from, to)` | las entradas del rango |

`DefinePlanCommand` archiva y define **en la misma unidad de trabajo**: es lo que sostiene
la invariante de "un plan activo a la vez", y por eso es un solo comando y no dos
llamadas seguidas del cliente.

`ListDaysQuery` es la respuesta a "observar qué he consumido el resto de días" y su
agregación va en SQL (`GROUP BY consumed_on`), no en memoria: un mes de histórico no
debería cargar un mes de filas para sumarlas en Java.

`GetDayQuery` es la única consulta que cruza los dos agregados: pide el plan activo al
`PlanReadModel`, las entradas del día al `IntakeReadModel` y las junta con `DayTotals`. Si no
hay plan activo, devuelve los totales **sin objetivo** (`target` y `remaining` a `null`) —
apuntar lo que comes tiene sentido aunque todavía no hayas decidido tu cuota.

Un día suma sus entradas **en memoria**, con `Macros.plus`, en vez de pedir un `SUM` al read
model: son un puñado de filas y así el día y su listado salen de una sola consulta. El `SUM` en
SQL se reserva para `ListDaysQuery`, donde el rango sí puede ser de meses.

## API HTTP

Montada en `/nutrition` desde `NutritionApplication.wire(...)`:

| Método | Ruta | Caso de uso |
|---|---|---|
| `POST` | `/nutrition/plan` | definir plan (archiva el anterior) |
| `GET` | `/nutrition/plan` | plan activo |
| `PUT` | `/nutrition/plan` | ajustar macros y/o peso objetivo |
| `DELETE` | `/nutrition/plan` | archivar |
| `POST` | `/nutrition/intakes` | registrar consumo |
| `GET` | `/nutrition/intakes` | listar (`from`, `to`, `limit`) |
| `GET` | `/nutrition/intakes/{id}` | detalle |
| `PUT` | `/nutrition/intakes/{id}` | corregir macros y nota |
| `DELETE` | `/nutrition/intakes/{id}` | borrar |
| `GET` | `/nutrition/today` | totales de hoy vs plan, con sus entradas |
| `GET` | `/nutrition/days/{date}` | totales del día vs plan, con sus entradas |
| `GET` | `/nutrition/days` | resumen por día del rango (`from`, `to`) |
| `GET` | `/nutrition/docs` | Swagger UI |
| `GET` | `/nutrition/openapi.json` | spec |

`/nutrition/today` existe porque la vista **no sabe qué día es para el servidor**: pedir
`/days/{date}` con la fecha del navegador daría el día equivocado en cuanto los dos relojes no
coincidan. Es la misma consulta con la fecha a `null`.

Los pesos viajan **como cadena en kilos** (`"78.4"`) y los macros como enteros en gramos,
siguiendo el precedente del importe en euros de `economy`. Al salir se les quitan los ceros
sobrantes: 84 000 g es `"84"`, no `"84.000"`.

`calories` es **obligatoria y mayor que cero** al registrar un consumo y al fijar un plan, y
viaja separada de los macros en toda respuesta (`consumedCalories` / `consumedMacros`, nunca
un `calories` dentro del objeto de macros). Un test de API comprueba que las calorías tecleadas
se devuelven tal cual aunque no cuadren con los gramos.

**Un macro ausente vale cero**, así que `{"calories":150,"note":"una cerveza"}` es una petición
válida y `{}` no lo es. Es la decisión que hace cómodo apuntar algo cuyas kcal conoces sin
desglosar lo que no sabes.

Los errores se traducen con la tabla de [error-conventions.md](error-conventions.md), y
sus textos visibles van a `web-shared/error-messages.js` — `ErrorMessagesTest` falla si un
código de `PlanErrors` o `IntakeErrors` se queda sin traducir.

Las rutas quedan detrás del `SessionGuard` de `presence`, con la sobrecarga sin guardia
(`() -> true`) para los tests del contexto suelto.

**SSE en `/events/nutrition`**, con un `NutritionEventsBroadcaster` suscrito a los seis
eventos. Es lo que hace que apuntar la cena desde el móvil aparezca en el espejo sin
recargar. Como la guardia solo mira en el handshake, el composition root añade este hub a
los que ya cierra al recibir `SessionClosedEvent` y `SessionExpiredEvent`.

## Infraestructura

Base de datos propia en `data/nutrition.db` — el aislamiento entre contextos es físico
([repository-conventions.md](repository-conventions.md)).

```
db-migrations/nutrition/
    V001__create_plans.sql
    V002__index_single_active_plan.sql
    V003__create_intakes.sql
    V004__index_intakes_by_date.sql
    V005__create_sequences.sql
```

Una sentencia por fichero: el migrador ejecuta el script entero de una vez, así que el índice
parcial no cabe dentro del `CREATE TABLE` y va en su propia versión.

```sql
CREATE TABLE plans (
    id             INTEGER PRIMARY KEY,
    start_weight_g INTEGER NOT NULL,
    target_weight_g INTEGER NOT NULL,
    protein_g      INTEGER NOT NULL,
    carbs_g        INTEGER NOT NULL,
    fat_g          INTEGER NOT NULL,
    status         TEXT    NOT NULL,   -- ACTIVE | ARCHIVED
    started_on     TEXT    NOT NULL,   -- ISO-8601
    defined_at     TEXT    NOT NULL
);

CREATE UNIQUE INDEX plans_single_active
    ON plans (status) WHERE status = 'ACTIVE';

CREATE TABLE intakes (
    id           INTEGER PRIMARY KEY,
    protein_g    INTEGER NOT NULL,
    carbs_g      INTEGER NOT NULL,
    fat_g        INTEGER NOT NULL,
    note         TEXT,
    consumed_on  TEXT    NOT NULL,     -- ISO-8601, para ordenar y filtrar como texto
    recorded_at  TEXT    NOT NULL
);
```

**`goal` no tiene columna**: lo deriva `Goal.of(...)` al leer. Las calorías **sí la tienen**
(`intakes.calories` y `plans.daily_calories`, añadidas en `V007`–`V010`), porque desde el
cambio son un dato tecleado y no una cuenta. Las migraciones de relleno reconstruyen el valor
de las filas antiguas con `4p+4c+9f`, que es lo que valían cuando se derivaban.

El índice parcial `plans_single_active` deja que **la base de datos sostenga la invariante de un
solo plan activo**, no solo el handler: si algún día entra un segundo camino de escritura, falla
ahí en vez de dejar dos planes vivos y una UI que elige uno al azar.

`SqliteIntakeReadModel` hace las agregaciones en SQL (`SUM` por columna, `GROUP BY
consumed_on`). Un test lo cose con el otro camino: sumar las filas del día a mano con
`Macros.plus` tiene que dar exactamente lo mismo que el `GROUP BY`.

`findActive()` está **en los dos lados** —en `PlanRepository` y en `PlanReadModel`— y no es un
descuido: el comando necesita el agregado para mutarlo dentro de la transacción, y la consulta
lo necesita para leerlo. Es la misma frase SQL en dos puertos con propósitos distintos.

La fontanería de consulta (`SqlQuery`, `StatementBinder`) vive en dos ficheros
*package-private* del propio paquete, no como tipos anidados dentro del read model:
[conventions.md](conventions.md) los prohíbe, y `SqliteMovementReadModel` los tiene solo por
ser anterior a la regla.

## UI espejo

Nueva pestaña **Nutrición**, en `src/main/resources/web-nutrition/`, servida por `core`
junto a las otras cuatro y en el mismo lenguaje visual: fondo negro, sin cajas, tipografía
en `rem`, jerarquía por tamaño y opacidad.

- **Calorías restantes hoy** como cifra protagonista, con las consumidas y la cuota debajo
  en pequeño.
- **Tres barras de macro** —proteína, carbos, grasa— con gramos consumidos sobre objetivo.
  Barras finas de proporción, como el desglose de categorías de `economy`. Nada de anillos
  concéntricos: a la distancia a la que se mira un espejo, tres anillos anidados no se
  leen.
- **Añadir consumo**: formulario inline de cuatro campos (proteína, carbos, grasa, nota),
  con las calorías calculándose en vivo mientras tecleas —con la misma fórmula que el dominio—.
  Es el mismo patrón de escritura inline que ya usa `routines`.
- **Entradas de hoy**: nota, gramos, calorías y un botón de borrar.
- **Plan**: el resumen en una línea y el formulario para fijarlo. Fijarlo dos veces es
  redefinirlo; el comando archiva el anterior por su cuenta, así que la vista no encadena
  dos llamadas.
- **Últimos días**: una fila por día con fecha, calorías y una barra respecto a la cuota.

En la vista de **Inicio**, una línea más junto a Agenda, Rutinas y Economía: calorías
restantes hoy y kilos que faltan para el objetivo.

## Cableado

`NutritionApplication.wire(renderer, dataDirectory, clock, sessionGuard)` sigue el patrón
exacto de `EconomyApplication`: abre su conexión, migra su esquema, monta sus dos buses, su
`SseHub` y su `Router`, con la sobrecarga sin guardia para los tests. `atlas.app.Application`
lo añade como quinto contexto, monta `/nutrition` y `/events/nutrition`, y suma su hub a
los que cierra al cerrarse la sesión.

## Testing

Según [testing-conventions.md](testing-conventions.md):

- **Dominio** — unitarios de `Plan`, `Intake`, `Weight`, `Macros`, `Goal` y `DayTotals`,
  con PIT sobre `atlas.domain.nutrition`. Umbral del 90%, como el resto.
- **Aplicación** — un test por handler con los puertos mockeados. El de `DefinePlan`
  comprueba que archivar y definir ocurren en la misma unidad de trabajo.
- **Infraestructura** — integración contra SQLite real: que el índice parcial impida un
  segundo plan activo, y que el `GROUP BY consumed_on` de un mes dé los mismos totales que
  sumar las filas a mano.
- **API** — un `HttpApiIT` que recorre el ciclo: definir plan, registrar tres consumos,
  consultar el día, corregir uno, borrarlo, listar el rango, redefinir el plan y comprobar
  que el anterior queda archivado.
- Un caso explícito de **calorías tecleadas**: que la respuesta devuelva las kcal que
  mandaste aunque no cuadren con los gramos, y que un consumo sin calorías sea 400. Es la
  regla que más caro sale equivocar.

---

# Ciclo 2 — Peso y evolución (retirado)

Se implementó entero —agregado `WeighIn`, servicio `PlanProgress`, sus dos consultas, la
tabla `weigh_ins` y la gráfica SVG— y **se retiró a petición del usuario**. La tabla la
tira `V011__drop_weigh_ins.sql`; `V006` se queda donde estaba, porque una migración
aplicada no se edita.

Lo que sobrevive es el **peso de partida y el objetivo del plan**, que es de donde sale
`Goal` y la línea "Perder peso: 84 → 78 kg". Lo que se fue es la serie de pesadas: el
progreso contra el plan, la tendencia semanal y la gráfica.

Si algún día vuelve, vuelve como un agregado `Measurement` genérico —peso, medidas,
porcentaje de grasa son la misma serie temporal— y no como una tabla por métrica. El
diseño de entonces está en el historial de este fichero.

---

# Descartado a propósito

**Base de datos de alimentos, códigos de barras y recetas.** Es la mitad de Fitia y el
motivo de que registrar allí sea un trabajo. Aquí se teclean tres números.

**Cálculo de TDEE y reparto automático de macros.** Sería un servicio de dominio puro
sobre altura, edad, sexo y actividad, sin tocar los agregados: `Plan` seguiría guardando
los macros que confirmes. Se puede añadir después sin migración si algún día cansa
calcularlos fuera.

**Comidas con nombre (desayuno, comida, cena, snack).** Un enum más y un filtro más en
todas las consultas. Ninguna de las cuatro preguntas del principio lo necesita; la nota
libre ya sirve para acordarse de qué era.

**Conciliar calorías y macros.** Desde que las calorías se teclean, los dos números pueden
discrepar y el sistema no avisa ni corrige. Podría hacerlo —marcar en la vista cuándo la
diferencia pasa de un umbral— pero eso es una alerta nueva con su umbral que discutir, y hoy
no la pide nadie.

**Ejercicio, calorías quemadas, agua y micronutrientes.**

**Fotos de progreso, medidas corporales y porcentaje de grasa.** Todo eso es otra serie
temporal con su gráfica; si algún día entra, entra como un agregado `Measurement` genérico
del que el peso sería un caso, no como cuatro tablas paralelas.
