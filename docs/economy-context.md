# Bounded context: `economy`

Diseño cerrado del contexto de economía doméstica. Este documento es la fuente de verdad
del modelo, los casos de uso y las decisiones tomadas — el código se escribe a partir de
aquí, no al revés.

Estado: **diseñado, sin implementar**.

## Qué resuelve

Saber en qué se te va el dinero sin abrir una app de banco. El espejo responde a tres
preguntas, en este orden de importancia:

1. ¿Cuánto he gastado este mes y cuánto me queda respecto a lo que entra?
2. ¿En qué categorías se está yendo?
3. ¿Qué he apuntado últimamente?

## Decisiones de alcance

**El espejo es de solo consulta.** La UI no registra movimientos: los muestra. Registrar
un gasto exige teclear un importe, y la interacción del espejo es gestual y por voz —
teclear ahí es la peor experiencia posible.

**La escritura entra por HTTP.** El contexto expone `/economy` como API completa y
alimentas desde donde quieras: un Atajo de iOS con un POST, un script, curl. La superficie
de escritura existe en la aplicación y en la API, pero no en la interfaz.

**Cero integración bancaria.** Ni PSD2, ni agregadores, ni importación de extractos. Sin
credenciales de terceros, sin tokens que rotar, sin proveedor de pago. Si algún día hace
falta importar un CSV, será un adaptador nuevo sobre los mismos comandos — el dominio no
se entera.

**Una sola moneda: EUR.** `Money` lleva el campo de divisa para que el sitio exista, pero
no hay conversión ni tabla de cambio.

**Un único bolsillo.** No hay cuentas. Todo movimiento cae en el mismo saldo. Modelar
cuentas (corriente, efectivo, tarjeta) añade un agregado, sus transferencias entre cuentas
—que no son ni ingreso ni gasto— y su UI. No hay ninguna pregunta de las tres de arriba
que necesite cuentas para responderse.

## Los tres ciclos

El contexto tiene tres agregados y **no caben en una sola entrega**. Movimientos es la
base: el presupuesto vigila el gasto *de los movimientos* y el objetivo de ahorro se
alimenta *de los movimientos*. Ninguno de los dos tiene nada que medir hasta que el
primero existe.

| Ciclo | Agregado | Estado |
|---|---|---|
| 1 | `Movement` — movimientos y saldo | diseñado abajo, en detalle |
| 2 | `Budget` — presupuesto por categoría | esbozado al final |
| 3 | `SavingsGoal` — objetivos de ahorro | esbozado al final |

Cada ciclo es su propio spec → plan → implementación. Lo que sigue es el ciclo 1 completo.

---

# Ciclo 1 — Movimientos y saldo

## El modelo de dominio

**El agregado es `Movement`, no una cuenta con movimientos dentro.** El saldo no es una
invariante que necesite consistencia transaccional: es una suma. Modelarlo como `Account`
con los movimientos como entidades internas obligaría a cargar el histórico entero en cada
gasto para validar algo que nadie puede violar. El saldo y el desglose son proyecciones y
viven en el read model.

```
domain/economy/
    MovementId.java                     prefijo 'M'
    Movement.java                       raíz de agregado
    MovementErrors.java                 catálogo de errores del contexto
    enums/MovementKind.java             INCOME | EXPENSE
    enums/Category.java                 smart enum
    vos/Money.java                      céntimos + divisa
    vos/MovementNote.java
    events/MovementRecordedEvent.java
    events/MovementCorrectedEvent.java
    events/MovementRecategorizedEvent.java
    events/MovementDeletedEvent.java
```

Prefijos de ID ya ocupados en el sistema: `A` citas, `G` gate de autenticación,
`B` perfil biométrico, `L` reto de vivacidad, `S` sesión, `R` rutina. **`M` está libre**
y es el que usa `MovementId`, con la utilidad `PrefixedIds` del Shared Kernel según
[id-conventions.md](id-conventions.md).

### `Money`

Importe en **céntimos como `long`**, nunca `double`. La coma flotante binaria no
representa exactamente 0,10 €, y el error se acumula en cada suma hasta aparecer como un
descuadre de céntimos en el saldo del mes.

```java
public record Money(long cents, String currency) implements ValueObject {
    public static Money ofCents(long cents)      // >= 0, EUR
    public static Money ofEuros(BigDecimal euros)
    public Money plus(Money other)               // exige misma divisa
    public Money minus(Money other)
    public BigDecimal toEuros()
}
```

**`Money` es siempre positivo.** El signo lo pone `MovementKind`, así el estado inválido
"un gasto de −20 €" —que sumaría en vez de restar— no existe como valor construible.

### `MovementKind`

```java
public enum MovementKind {
    INCOME, EXPENSE;

    public long signed(Money money)      // EXPENSE devuelve el negativo
    public static MovementKind of(long signedCents)
}
```

Estas dos operaciones son **la única definición del signo en todo el sistema**. La
persistencia guarda el importe ya con signo y lo lee de vuelta con `of(...)`, de modo que
el `SUM` de SQLite no tiene que repetir la regla en un `CASE WHEN`. Una regla, un sitio.

### `Category`

Smart enum, persistido por nombre según [enum-conventions.md](enum-conventions.md):

```
COMIDA, TRANSPORTE, HOGAR, OCIO, SALUD, COMPRAS, INGRESO, OTROS
```

Cada constante lleva su etiqueta visible y su icono para la UI, y sabe si es de ingreso o
de gasto (`INGRESO` no puede etiquetar un `EXPENSE`, y a la inversa) — esa es la
invariante que justifica el enum en vez de una constante suelta.

Categorías definidas por el usuario serían un segundo agregado con su CRUD, su UI de
administración y su regla de qué pasa con los movimientos de una categoría borrada. Hoy no
hay quién las administre: el espejo no escribe.

### `Movement`

```java
public final class Movement extends AggregateRoot<MovementId> {

    public static Result<Movement> record(
        MovementId id, MovementKind kind, Money amount,
        Category category, MovementNote note, LocalDate occurredOn, Instant now)

    public Result<Void> correct(Money amount, MovementNote note, LocalDate occurredOn, Instant now)
    public Result<Void> recategorize(Category category, Instant now)
    public Result<Void> delete(Instant now)
}
```

Invariantes que hace cumplir:

- El importe es mayor que cero. Un movimiento de 0 € no es un hecho económico.
- La categoría concuerda con el tipo (`INGRESO` ↔ `INCOME`).
- La fecha del movimiento no está en el futuro. Apuntas lo que ya ha pasado; lo que va a
  pasar es un pago recurrente, que es otro contexto y está fuera de alcance.
- Un movimiento borrado no admite más cambios.

**Sin contraasientos ni anulaciones.** Es economía doméstica, no contabilidad por partida
doble: corregir un importe mal metido lo corrige, no genera un asiento inverso.

## Capa de aplicación

Cuatro comandos y cuatro consultas, con la estructura de carpeta por caso de uso que sigue
el resto de contextos ([cqrs-conventions.md](cqrs-conventions.md)).

```
application/economy/
    commands/recordmovement/        RecordMovementCommand + Handler
    commands/correctmovement/       CorrectMovementCommand + Handler
    commands/recategorizemovement/  RecategorizeMovementCommand + Handler
    commands/deletemovement/        DeleteMovementCommand + Handler
    queries/getbalance/             GetBalanceQuery + Handler
    queries/listmovements/          ListMovementsQuery + Handler
    queries/getmovement/            GetMovementQuery + Handler
    queries/getbreakdown/           GetBreakdownQuery + Handler
    dto/                            MovementDto, MovementSummaryDto,
                                    BalanceDto, CategorySpendDto
    mappers/EconomyMapper.java
    ports/MovementRepository.java
    ports/MovementReadModel.java
    ports/MovementUnitOfWork.java
```

`BalanceDto` lleva `income`, `expense` y `net` del periodo pedido. `CategorySpendDto`
lleva categoría, total y porcentaje sobre el gasto del periodo — el porcentaje lo calcula
el mapper, no la UI, para que la cifra sea la misma en pantalla y en la API.

No hay generador de IDs de entidad interna: el agregado no tiene entidades hijas. El ID de
la raíz sale de `SqliteSequenceGenerator`, como en los demás contextos.

## API HTTP

Montada en `/economy` desde `EconomyApplication.wire(...)`:

| Método | Ruta | Caso de uso |
|---|---|---|
| `POST` | `/economy/movements` | registrar |
| `GET` | `/economy/movements` | listar (`from`, `to`, `category`, `limit`) |
| `GET` | `/economy/movements/{id}` | detalle |
| `PUT` | `/economy/movements/{id}` | corregir importe, nota y fecha |
| `PUT` | `/economy/movements/{id}/category` | recategorizar |
| `DELETE` | `/economy/movements/{id}` | borrar |
| `GET` | `/economy/balance` | saldo del periodo (`from`, `to`) |
| `GET` | `/economy/breakdown` | desglose por categoría del periodo |
| `GET` | `/economy/docs` | Swagger UI |
| `GET` | `/economy/openapi.json` | spec |

Los errores se traducen a HTTP con la tabla de
[error-conventions.md](error-conventions.md). Las rutas quedan detrás de la sesión de
`presence`, igual que `/appointments`.

**SSE en `/events/economy`**, con un `EconomyEventsBroadcaster` suscrito a los cuatro
eventos de dominio. Es lo que hace que apuntar un gasto desde el móvil se vea en el espejo
sin recargar, y es la razón por la que la UI puede ser de solo lectura sin quedarse
obsoleta.

## Infraestructura

Base de datos propia en `data/economy.db` — el aislamiento entre contextos es físico
([repository-conventions.md](repository-conventions.md)).

```
db-migrations/economy/
    V001__create_movements.sql
    V002__create_sequences.sql
    V003__index_movements_by_date.sql
```

```sql
CREATE TABLE movements (
    id           INTEGER PRIMARY KEY,
    amount_cents INTEGER NOT NULL,   -- con signo: negativo = gasto
    currency     TEXT    NOT NULL DEFAULT 'EUR',
    category     TEXT    NOT NULL,
    note         TEXT,
    occurred_on  TEXT    NOT NULL,   -- ISO-8601, para ordenar y filtrar como texto
    recorded_at  TEXT    NOT NULL
)
```

**`amount_cents` se guarda con signo y `kind` no se persiste**: se deriva con
`MovementKind.of(signedCents)` al leer. Guardar las dos cosas permite que discrepen, y
entonces hay que decidir cuál gana. El índice de `V003` es por `occurred_on`, que es el
filtro de toda consulta real.

`SqliteMovementReadModel` hace las agregaciones en SQL (`SUM`, `GROUP BY category`), no en
memoria: el desglose de un año no debería cargar un año de filas.

## UI espejo

Nueva pestaña **Economía**, en `src/main/resources/web-economy/`, en el mismo lenguaje
visual que el resto: fondo negro, sin cajas, tipografía en `rem`, jerarquía por tamaño y
opacidad.

- **Saldo del mes** como cifra protagonista, con ingresos y gastos debajo en pequeño.
- **Desglose por categoría**: lista ordenada por importe, con icono, total y una barra fina
  de proporción. Sin gráfico de tarta — a la distancia a la que se mira un espejo, una
  tarta con ocho porciones es ilegible.
- **Últimos movimientos**: fecha, nota, categoría e importe.
- Sin formularios ni botones de acción. La vista escucha `/events/economy` y se refresca
  sola.

En la vista de **Inicio**, una línea más junto a Agenda y Rutinas: gasto del mes y saldo.
Cabe en el bloque de la izquierda que ya está montado.

## Cableado

`EconomyApplication.wire(renderer, dataDirectory, clock)` sigue el patrón exacto de
`RoutinesApplication`: abre su conexión, migra su esquema, monta sus dos buses, su
`SseHub` y su `Router`. `atlas.app.Application` lo añade como cuarto contexto y monta
`/economy` y `/events/economy`.

## Testing

Según [testing-conventions.md](testing-conventions.md):

- **Dominio** — unitarios de `Movement`, `Money`, `MovementKind` y `Category`, con PIT
  sobre el paquete de dominio. El objetivo del contexto es el mismo que el de
  `appointments`: mutation score por encima del 90%.
- **Aplicación** — un test por handler con el puerto mockeado.
- **Infraestructura** — tests de integración contra SQLite real, incluidos los redondeos:
  que `SUM` de un mes con importes que no son redondos dé el céntimo exacto.
- **API** — un `HttpApiIT` que recorre el ciclo completo: registrar, listar, corregir,
  recategorizar, consultar saldo y desglose, borrar.
- Un caso explícito de **signo**: registrar un ingreso y un gasto del mismo importe y
  comprobar que el saldo es cero. Es la regla que más caro sale equivocar.

---

# Ciclo 2 — Presupuesto por categoría (esbozo)

Agregado `Budget`: una categoría, un límite mensual (`Money`) y el mes al que aplica.
Invariantes: límite positivo, un solo presupuesto vivo por categoría y mes.

El consumo **no se guarda en el agregado**: se calcula cruzando el límite con el gasto del
read model de movimientos. Guardarlo obligaría a actualizar el presupuesto en cada gasto y
abriría la puerta a que se desincronice.

Lo interesante es el dominio del ritmo: no solo "llevas 180 de 200", sino "a este ritmo
acabas el mes en 260". Eso es un servicio de dominio con su test, y es lo que hace que el
ciclo valga la pena en vez de ser una resta.

En la UI, la barra de la categoría pasa a mostrar consumo sobre límite, y el desvío se
señala con color.

# Ciclo 3 — Objetivos de ahorro (esbozo)

Agregado `SavingsGoal`: nombre, importe objetivo, fecha límite. Proyecta si llegas al
ritmo de ahorro actual (ingresos menos gastos de los últimos meses) y a cuánto tendrías
que subirlo para llegar.

Depende del ciclo 1 para el ritmo real y se lleva bien con el ciclo 2: el margen que deja
el presupuesto es lo que alimenta el objetivo.

---

# Descartado a propósito

Registro por voz en el espejo (`"gasto, doce euros, comida"`) — el reconocimiento de
importes por voz falla justo donde más duele. Se puede reconsiderar cuando la entrada por
API lleve un tiempo funcionando y sepamos qué se apunta de verdad.

Importación de CSV del banco — parsing por entidad, deduplicación y categorización
automática. Sería un adaptador sobre `RecordMovementCommand`, sin tocar el dominio.

Pagos recurrentes y avisos de vencimiento — es el cuarto agregado candidato y solapa con
los recordatorios de `appointments`. Antes de modelarlo hay que decidir cuál de los dos
contextos es dueño del aviso.

Cuentas, adjuntos de tickets, multidivisa y partida doble.
