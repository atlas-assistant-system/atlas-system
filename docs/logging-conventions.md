# Convenciones de logging

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

## Dónde se loguea: en el borde, nunca en el dominio

**El dominio no loguea.** Ni una llamada, ni un logger como campo. No lo necesita:
los fallos de negocio son valores (`Result`), y quien los observa es el decorador que
envuelve al handler.

Si una clase de dominio parece necesitar un logger, casi siempre significa que está
devolviendo `void` donde debería devolver `Result`. Es una regla verificable con
ArchUnit: ninguna clase de `domain` referencia `System.Logger`.

## Cómo: decoradores explícitos, no AOP

El logging es el caso de uso canónico de AOP, y aquí se resuelve con el patrón
decorador, cableado a mano en el composition root (ver `ddd-conventions.md`, sección
sobre por qué no usamos anotaciones ni aspectos).

```java
commands.register(
    ScheduleAppointmentCommand.class,
    new LoggingCommandHandler<>(new ScheduleAppointmentCommandHandler(appointments, events, clock), renderer));
```

Repetir eso por handler cansa, así que lo normal es un método privado en el arranque
del módulo:

```java
private static <C extends Command<R>, R> void register(
        SimpleCommandBus bus, Class<C> type, CommandHandler<C, R> handler, LogEntryRenderer renderer) {
    bus.register(type, new LoggingCommandHandler<>(handler, renderer));
}
```

Cinco líneas escritas una vez: ese es el precio completo de no usar AOP, a cambio de
que la cadena sea navegable, depurable y testeable con un doble.

## Qué se loguea: nada del contenido, por defecto

El default es **tipo del mensaje, desenlace y duración — nunca los campos**. Un módulo
puede manejar datos de salud, dinero o localización, y un decorador que vuelque el
comando entero los escribiría a un fichero de texto plano.

Si un comando concreto quiere aportar contexto, lo declara explícitamente:

```java
public record ScheduleAppointmentCommand(UserId organizerId, LocalDateTime start, LocalDateTime end, String notes)
        implements Command<Result<AppointmentId>>, LoggableSummary {

    @Override
    public String logSummary() {
        return "slot=" + start + "/" + end;
    }
}
```

`notes` está en el comando pero no en el resumen: esa omisión **es** la política de
privacidad. La decisión de exponer un dato la toma quien escribe el comando, no una
convención automática ni un mecanismo que adivina.

El resumen se sanea antes de escribirse: los caracteres de control se sustituyen por
espacios y se trunca a 120 caracteres. Sin eso, un texto introducido por el usuario que
contenga un salto de línea podría **forjar una línea de log falsa**.

## Niveles: un fallo de negocio no es un error

| Desenlace | Comandos | Consultas |
|---|---|---|
| Éxito | `INFO` | `DEBUG` |
| Fallo de negocio (`Result.failure`) | `INFO` | `DEBUG` |
| Excepción | `ERROR` | `ERROR` |

Un `Result.failure` es un desenlace **esperado**: si se registrara como `WARNING` o
`ERROR`, la monitorización se llenaría de flujo normal y dejaría de mirarse. La
diferencia entre éxito y fallo la marca el campo `outcome=`, no el nivel.

Las consultas van en `DEBUG` porque son mucho más frecuentes (una interfaz con SSE
refresca constantemente); en el arranque normal no aparecen.

## Formato: dos renderizadores sobre el mismo dato

El decorador no construye texto: construye un `HandlerLogEntry` y delega en un
`LogEntryRenderer`. Hay dos, y se elige en el arranque:

- **`ConsoleLogEntryRenderer`** — para terminal. Barra de color en el margen izquierdo,
  columnas de ancho fijo, sin símbolos:

```
▌ 10:15:30  ScheduleAppointment       12ms   slot=2026-08-17T09:00/10:00
▌ 10:15:30  ListAppointmentsByDay      4ms
▌ 10:15:31  RescheduleAppointment      3ms   APPOINTMENT_CANCELLED
▌ 10:15:34  ScheduleAppointment      214ms   SQLiteException: database is locked
```

  Verde = comando correcto, gris = consulta, ámbar = fallo de negocio, rojo = excepción.

- **`PlainLogEntryRenderer`** — `clave=valor` en una línea, greppable y parseable:

```
type=command name=ScheduleAppointment outcome=failure errorCode=APPOINTMENT_CANCELLED durationMs=3
```

`LogEntryRenderers.forCurrentConsole()` decide en este orden:

1. **`-Dlog.format=console|plain`** (o la variable de entorno `LOG_FORMAT`). Manda sobre
   todo lo demás.
2. **`NO_COLOR`**, si está definida: formato plano. Es la convención estándar para pedir
   salida sin color.
3. **Autodetección**: formato de consola solo si `System.console()` existe y es un
   terminal.

> **Si ves `clave=valor` cuando esperabas el formato de consola, no está roto.** Cuando la
> salida está redirigida —a un fichero, a un IDE, o a `gradle run`, que la canaliza hacia
> su demonio— `System.console()` es `null` y la autodetección elige plano. **La
> autodetección nunca puede acertar bajo `gradle run`**: por eso la plantilla fuerza
> `log.format=console` en la tarea `run`, y por eso existe el interruptor explícito.

Que el fichero de log nunca lleve secuencias ANSI es deliberado, y por eso **la semántica
no depende del color**: el formato plano lleva `outcome=` escrito con todas las letras.

La barra del margen (`▌`) no se emite a ciegas: el renderer comprueba si la codificación
de salida puede representarla y, si no, usa `|`. Sin esa comprobación, una consola de
Windows con página de códigos heredada imprime `?` en cada línea. Si prefieres la barra,
la solución no es forzar `stdout.encoding` —eso produciría caracteres corruptos, que es
peor que el `|`— sino poner el terminal en UTF-8.

## Correlación

`CorrelationContext` usa `ScopedValue` (Java 25): se propaga por la cadena de llamadas
sin `ThreadLocal` ni fugas, y funciona con hilos virtuales. Si no hay ámbito ligado, el
campo simplemente no se escribe.

```java
CorrelationContext.runWith(newCorrelationId(), () -> router.dispatch(exchange));
```

## Configuración

El backend es `System.Logger` del JDK, respaldado por `java.util.logging`. Los niveles
se ajustan por nombre de logger:

```properties
handlers=java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level=ALL
java.util.logging.ConsoleHandler.formatter=sharedkernel.infrastructure.logging.RawMessageFormatter
sharedkernel.command.level=INFO
sharedkernel.query.level=FINE
```

Dos detalles que despistan:

- `System.Logger.Level.DEBUG` se traduce a **`FINE`** en `java.util.logging`. Las
  consultas se activan con `FINE`, no con `DEBUG`.
- `RawMessageFormatter` imprime solo el mensaje (más el stack trace si lo hay). Sin él,
  el formateador por defecto de JUL envuelve cada línea en dos y rompe la alineación de
  columnas.

## Medición del tiempo

El decorador usa `System.nanoTime()` para la duración. No contradice la regla de
`testing-conventions.md` sobre no leer el reloj: esa regla protege el determinismo del
**dominio**, y aquí se trata de infraestructura midiendo tiempo transcurrido — para lo
cual `Clock` sería además incorrecto, porque no es monótono. El instante de la línea sí
viene de un `Clock` inyectado, para que los tests del renderizador sean deterministas.

## Pendiente / a definir más adelante

- Barra proporcional de duración para operaciones lentas (idea descartada de momento
  por no llenar de barras vacías las líneas rápidas).
- Variante ASCII de la barra para consolas que no estén en UTF-8.
- Rotación a fichero y política de retención.
- Decoradores de transacción y de métricas, siguiendo este mismo patrón.
