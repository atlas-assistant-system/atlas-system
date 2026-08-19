# Stack técnico

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

## Lenguaje y plataforma

- **Java 25**, sin frameworks (ni Spring ni ningún contenedor de inversión de control
  externo). Todo se construye directamente sobre el JDK.
- **JPMS** (Java Platform Module System): el proyecto entero es **un único módulo Java**,
  con un solo `module-info.java`, que declara de qué depende y qué expone hacia fuera.
  Los anillos de la arquitectura son paquetes dentro de ese módulo, así que **la regla de
  dependencia entre anillos no la impone el compilador** sino ArchUnit, en los tests (ver
  [architecture.md](architecture.md)).

## Construcción

- **Gradle 9**, un único proyecto: un `src/main/java` y un `src/test/java` para todo.

## Persistencia

- **SQLite**. Un fichero de base de datos por contexto (bounded context).

## Servidor HTTP

- **`jdk.httpserver`** (`com.sun.net.httpserver`), incluido en el JDK. Sin servidor de
  aplicaciones ni framework HTTP externo.
- El servidor se monta con `WebServer` (Shared Kernel), que **escucha solo en loopback** y
  usa un **executor de hilos virtuales**: cada petición y cada conexión SSE abierta ocupa
  un hilo virtual, no uno del sistema operativo.

### Convención de rutas (decidida)

**Un prefijo de montaje por bounded context**, declarado con `Routes.at(...)`, y todos
ellos montados en un único `Router`:

```java
Router.builder()
    .mount(Routes.at("/appointments")
        .get("/", handlers::list)
        .get("/{id}", handlers::detail)
        .post("/", handlers::schedule))
    .mount(Routes.at("/reminders")
        .get("/", handlers::list))
    .build();
```

- Los segmentos `{nombre}` son parámetros de ruta; se leen con `request.pathParam("id")`
  ya decodificados (un `%2F` no parte el segmento).
- Un handler recibe `HttpRequest` y devuelve `HttpResponse` — **nunca escribe en el
  socket**. No puede lanzar excepciones comprobadas: todo lo técnico queda en el `Router`.
- Ninguna ruta se declara con el prefijo escrito a mano: **el proyecto no es dueño de la
  raíz** y el prefijo debe poder cambiarse en un solo sitio.
- Si la ruta existe pero el método no, la respuesta es **405 con cabecera `Allow`**; si no
  existe ninguna ruta, **404**. Ambas viajan con el mismo cuerpo `ApiError` que el resto.

El `Router` resuelve una vez, en la frontera, cuatro cosas que si no acaban copiadas en
cada handler:

- **Traducción de errores.** Cualquier excepción no controlada se convierte en `ApiError`;
  una `FormatException` es 400 y **todo lo demás es 500 con mensaje genérico** — el
  detalle real va al log, nunca al cuerpo de la respuesta.
- **Identificador de correlación.** Se lee de `X-Correlation-Id`, y si no viene (o trae
  algo que no encaja en `[A-Za-z0-9_-]{1,64}`) se genera uno. Queda vinculado al hilo
  mientras corre el handler, aparece en el `ApiError` y se devuelve en la misma cabecera.
  Se valida en vez de sanearse porque un valor con salto de línea permitiría **inyectar
  cabeceras** en la respuesta.
- **Límite de tamaño del cuerpo** (`Router.MAX_BODY_BYTES`, 1 MiB): se rechaza con 413,
  antes de leer si el `Content-Length` ya lo declara. Con un heap de 96 MB, leer un cuerpo
  sin límite es un modo de caída trivial de provocar.
- **Cuerpo del error en JSON.** Es lo único que serializa el Shared Kernel; los handlers
  entregan su JSON ya serializado como `String`. Así el kernel no arrastra librería JSON y
  la ruta de error nunca depende de que el serializador del proyecto funcione.

## Actualización de pantalla

- **Server-Sent Events (SSE)** para el push del servidor hacia el cliente.
- **POST** para las acciones del usuario.

### Formato de los eventos SSE (decidido)

```
id: 42
event: appointmentScheduled
data: {"appointmentId":"A00000007"}

```

- `event` es el nombre al que se suscribe el cliente; `data` es JSON en una línea,
  serializado por el módulo (el Shared Kernel no incorpora librería JSON).
- `id` lo asigna `SseHub` con un contador creciente. **Hoy no hay reenvío tras
  reconexión**: el cliente que reconecta recarga su estado con una query normal. Los ids
  se emiten desde el principio para que añadir un búfer de reenvío más adelante no
  obligue a cambiar el formato.
- **Un único stream SSE por proyecto**, en `/events`, compartido por todos los bounded
  contexts. Como el cliente se suscribe por el nombre del evento, **ese nombre tiene que
  ser único en todo el proyecto**: se nombra desde el agregado (`appointmentScheduled`),
  que es lo que naturalmente evita la colisión.
- El `SseHub` trabaja sobre un `OutputStream` y no sobre `jdk.httpserver`, de modo que el
  formato de cable se testea contra un `ByteArrayOutputStream` sin levantar un servidor;
  el adaptador que sí conoce el `HttpExchange` es `SseEndpoint`, aparte y mínimo.

Cuatro detalles que el kernel resuelve una vez y conviene no reimplementar:

- **Un `data` multilínea repite el prefijo `data:` en cada línea.** Si no, el cliente
  recibe el mensaje truncado.
- **Al abrir el stream se envía un comentario `: connected` y se hace flush.**
  `sendResponseHeaders` no manda las cabeceras por el cable hasta que se escribe algo, así
  que un SSE que solo se queda esperando eventos deja al cliente colgado sin llegar a
  abrir la conexión.
- **Latido periódico** (`: ping`) mediante `SseHub.sendHeartbeat()`: sin tráfico, el
  navegador o un proxy cortan la conexión a los pocos minutos. Quién lo programa es
  decisión del composition root.
- **Nunca se fija `Content-Length`** (ver `SseHeaders.forStream()`). Con longitud fija el
  servidor espera el cuerpo completo y no envía nada — es la causa más común de que
  "SSE no funcione".

Los nombres de evento y los ids rechazan saltos de línea: uno solo permitiría **forjar un
frame del protocolo** desde un dato de usuario.

## JSON

- **Jackson jr** (variante ligera de Jackson, sin el módulo `databind` completo).

## Cliente HTTP saliente

- **`java.net.http.HttpClient`**, incluido en el JDK. Sin librerías HTTP externas.

## Interfaz de usuario

- **HTML, CSS y JavaScript** escritos a mano. Cero npm, cero bundlers, cero frameworks
  de frontend.

## Testing

- **JUnit 5** para pruebas unitarias y de integración.
- **AssertJ** para assertions legibles y encadenables.
- **Mockito** para mockear puertos (repositorios, read models, publishers) — nunca
  objetos de dominio.
- **JaCoCo** para cobertura de línea (suelo mínimo, detector de huecos, no objetivo en
  sí mismo).
- **PIT** para mutation testing en `domain` — criterio de profundidad donde vive la
  lógica de negocio.
- **ArchUnit** para verificar programáticamente las reglas de arquitectura (dependencias
  entre capas, convenciones de paquetes) como parte de la suite de tests. Al ser el
  proyecto un único módulo JPMS, **es el único mecanismo que impone la regla de
  dependencia**, no un refuerzo opcional.

Ver [testing-conventions.md](testing-conventions.md) para la convención completa (esta
instancia local de las skills genéricas `test-conventions`/`unit-testing` ya existentes
del usuario, adaptadas a un stack sin Spring/framework).

## Configuración de memoria de la JVM

- `-Xms96m -Xmx96m` (heap fijo y pequeño)
- `-Xss256k` (stack reducido)
- `-XX:+UseSerialGC`
- Metaspace: `64m`

Estos flags reflejan una filosofía deliberada de bajo consumo de recursos: cada
proyecto/módulo debe poder ejecutarse cómodamente con un footprint mínimo.

## Shared Kernel como dependencia

El `Shared Kernel` se consume como dependencia Gradle normal:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("dev.sharedkernel:sharedkernel:0.19.0")
    testImplementation("dev.sharedkernel:sharedkernel-archunit:0.2.0")
}
```

El `module-info.java` del proyecto declara `requires sharedkernel;` una sola vez.

> **Limitación conocida en CI.** `mavenLocal()` es el `~/.m2` de la máquina de
> desarrollo: en un runner de integración continua está vacío, así que el workflow
> fallará al resolver la dependencia hasta que el Shared Kernel se publique en un
> repositorio alcanzable desde la red (un registro de paquetes, o el jar versionado
> dentro del propio repo con `flatDir`). Es una consecuencia directa de haber elegido
> `mavenLocal` para el desarrollo local y está sin decidir.

## Pendiente / a definir más adelante

- Dónde se lee la configuración del proyecto (puerto, directorio de las bases de datos,
  entorno). Hoy el puerto es una constante en `Main`.
- Quién programa el latido del `SseHub` y con qué periodo. **Sin latido, un cliente que
  se va sin avisar mantiene su hilo virtual bloqueado indefinidamente**: la desconexión
  solo se detecta cuando falla una escritura.
- Si en algún momento hace falta reenvío tras reconexión (`Last-Event-ID` + búfer).
