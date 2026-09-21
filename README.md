<div align="center">

<pre>
 █████╗ ████████╗██╗      █████╗ ███████╗
██╔══██╗╚══██╔══╝██║     ██╔══██╗██╔════╝
███████║   ██║   ██║     ███████║███████╗
██╔══██║   ██║   ██║     ██╔══██║╚════██║
██║  ██║   ██║   ███████╗██║  ██║███████║
╚═╝  ╚═╝   ╚═╝   ╚══════╝╚═╝  ╚═╝╚══════╝
</pre>

**Lo que necesitas ver, cuando levantas la vista**

[![Tech Stack](https://skillicons.dev/icons?i=java,gradle,sqlite,js,html,css)](https://skillicons.dev)

</div>

---

## 🪞 Sobre el proyecto

**Atlas** es una pantalla ambiental permanente: un espejo colgado en la pared que, sobre el
reflejo de la cámara, muestra el reloj, el tiempo, las noticias y lo que tengas pendiente hoy.
No tiene teclado ni ratón: **te reconoce por la cara** y se maneja **con gestos**.

La idea es que no haya que pedirle nada. Te acercas, te identifica, y lo que necesitas ya está
ahí: la próxima cita, los hábitos del día, el saldo del mes, las calorías que llevas, el entreno
que toca. Cuando te vas, se duerme sola —tras 90 s sin ningún rostro delante cae un velo negro y
la detección baja a 2 fps— y cualquier cara la despierta. No hace falta sensor de presencia
porque la cámara ya está mirando.

**Nada se carga de fuera.** Los modelos de visión y el runtime de gestos los sirve el propio
Atlas: un espejo de pared no puede depender de un CDN de terceros para dejarte entrar.

## ✨ Qué hace

- **Identidad biométrica facial** con prueba de vida: calibra la pose neutral, pide un gesto de
  desafío (el puño) y agrega las tres mejores capturas frontales. El alta guarda cinco poses como
  plantillas del mismo perfil, y admite variantes posteriores (con y sin gafas, por ejemplo).
- **Control sin contacto**: reconocedor de gestos de mano sobre MediaPipe. Palma abierta cancela,
  pulgar hacia abajo entra y sale del **modo espejo**, que quita todo y deja solo el reflejo.
- **Agenda**: citas, recordatorios y calendario, con avisos en pantalla.
- **Rutinas**: hábitos como cuota dentro de un periodo, con su racha.
- **Economía**: movimientos, saldo y desglose por categoría. Los movimientos entran por HTTP
  (un Atajo de iOS, un script, `curl`) porque son el reflejo del banco; los presupuestos se fijan
  desde la pantalla, porque nacen mirándola.
- **Nutrición**: plan diario y consumo, con macros y calorías tecleadas a mano. Fitia sin base de
  datos de alimentos.
- **Entrenamiento**: catálogo de ejercicios, plantillas por día de la semana y registro de series.
  Strong sin catálogo. Una serie se mide de cuatro formas —carga, repeticiones, tiempo,
  distancia— y el historial nunca se reescribe: lo previsto se congela al empezar el entreno.
- **Ambiente**: reloj, saludo por tu nombre, una frase al día, clima con una sola línea de
  previsión ("Lluvia a las 18:00 · 70%") y titulares refrescados en segundo plano.
- **Todo en vivo**: cada contexto publica sus cambios por SSE y la pantalla se refresca sola.

## 🏗️ Arquitectura

Arquitectura **hexagonal por anillos** (`domain` → `application` → `infrastructure` →
`presentation`), con **DDD táctico** y **CQRS**. Es **un único módulo JPMS**, y cada bounded
context es un subpaquete dentro de cada anillo:

```
atlas/domain/          sharedkernel · appointments · presence · routines · economy · nutrition · training
atlas/application/     (los mismos)
atlas/infrastructure/  (los mismos)
atlas/presentation/    (los mismos)
atlas/app/             el cableado de cada contexto + Application.java, el composition root
```

La regla de dependencia entre anillos **no la impone el compilador** —todo es un módulo— sino
**ArchUnit**, en los tests.

**Los contextos no se conocen.** Cada uno tiene su propia base de datos SQLite en `data/`
—el aislamiento es físico— y su propio bus de comandos y consultas. Lo único que comparten es la
puerta: `appointments`, `routines`, `economy`, `nutrition` y `training` devuelven 401 sin sesión
usando el mismo `SessionGuard`, que consulta la sesión de `presence` a través de un contrato
booleano cableado en el composition root. No hay HTTP interno ni rutas proxy entre contextos.

## 📦 Estructura del proyecto

```
atlas/
├── src/main/java/atlas/        # los cuatro anillos + app (composition root)
├── src/main/resources/
│   ├── web-core/               # la pantalla espejo: reloj, pestañas, cámara
│   ├── web-presence/           # JS de visión: calidad facial, gestos, alta de perfil
│   ├── web-shared/             # mensajes de error que ve la persona, en un único sitio
│   ├── web-<contexto>/         # documentación y vistas auxiliares de cada contexto
│   └── db-migrations/          # una carpeta de migraciones por base de datos
├── src/test/java/atlas/        # espejo de la estructura anterior + architecture/rules
└── docs/                       # la fuente de verdad técnica del proyecto
```

## 🛠️ Stack técnico

| Capa | Tecnologías |
|---|---|
| Lenguaje | Java 25 · JPMS · **sin frameworks** (ni Spring ni contenedor de IoC) |
| Servidor HTTP | `jdk.httpserver` sobre hilos virtuales · SSE para el push · solo loopback |
| Persistencia | SQLite (`org.xerial:sqlite-jdbc`) · una base de datos por contexto |
| JSON | `jackson-jr` |
| Pantalla | HTML + CSS + JavaScript sin build ni bundler · módulos IIFE |
| Visión | Human (detección y descriptores faciales) · MediaPipe Tasks Vision (gestos) |
| Tests | JUnit 5 · AssertJ · Mockito · ArchUnit · PIT (mutation testing) |
| Build | Gradle 9 · Spotless |

El proceso corre con **96 MB de heap** y GC serie: es un espejo, no un servidor.

## 🚀 Empezar

### Requisitos

- **JDK 25**
- Una **cámara** accesible desde el navegador (la pantalla se abre en `localhost`, contexto seguro)

El wrapper de Gradle está incluido en el repositorio.

```bash
git clone <url-del-repositorio>
cd atlas

./gradlew build   # compila, aplica el formatter y pasa los tests (incluidos los de ArchUnit)
./gradlew run     # arranca el servidor en http://localhost:8080
```

La primera compilación descarga los modelos de visión (~43 MB) a `build/generated-resources`
con `gradlew downloadWebVendor`; no están en el repositorio y entran al jar como un recurso más.

```bash
./gradlew pitest  # mutation testing sobre atlas.domain.* (~30 s, no cuelga de `check`)
```

## ✅ Estado

- **1810 tests en verde**, incluidos los de integración contra SQLite real y las reglas de ArchUnit.
- **Mutation testing** con umbral del 90%: el dominio está al 95%.
- Los seis contextos están cableados y en uso; `economy` (presupuestos y objetivos) y `training`
  (progresión) tienen ciclos diseñados y sin implementar.

## 📚 Documentación

- [CLAUDE.md](CLAUDE.md) — contexto general del proyecto
- [docs/architecture.md](docs/architecture.md) — capas, patrones y organización del código
- [docs/stack.md](docs/stack.md) — lenguajes, librerías y herramientas
- [docs/ddd-conventions.md](docs/ddd-conventions.md) · [docs/cqrs-conventions.md](docs/cqrs-conventions.md) · [docs/id-conventions.md](docs/id-conventions.md)
- [docs/error-conventions.md](docs/error-conventions.md) · [docs/rich-domain-conventions.md](docs/rich-domain-conventions.md) · [docs/repository-conventions.md](docs/repository-conventions.md)
- [docs/enum-conventions.md](docs/enum-conventions.md) · [docs/mapping-conventions.md](docs/mapping-conventions.md) · [docs/testing-conventions.md](docs/testing-conventions.md)
- [docs/logging-conventions.md](docs/logging-conventions.md) · [docs/conventions.md](docs/conventions.md)

---

<div align="center">

*Atlas: levantas la vista y ya está ahí.*

</div>
