<div align="center">
  <img src="./assets/public-png/lockup/lockup-horizontal-monochrome.png" alt="Atlas — Personal Life Assistant" width="480" />
</div>

<div align="center">

# Atlas

</div>

<div align="center">
  What you need to see, when you look up.
</div>

<div align="center">
  <a href="./src/main/java/atlas/">source</a> ·
  <a href="./src/test/java/atlas/">tests</a> ·
  <a href="./docs/">docs</a> ·
  <a href="./assets/">assets</a>
</div>

<br />

<div align="center">
  <a href="https://go-skill-icons.vercel.app/">
    <img src="https://go-skill-icons.vercel.app/api/icons?i=java,gradle,sqlite,js,html,css,git&titles=true" alt="Java, Gradle, SQLite, JavaScript, HTML, CSS, and Git" />
  </a>
</div>

---

<div align="center">

## 🪞 About

</div>

**Atlas** is an always-on ambient display: a mirror hanging on the wall that, over the camera's
reflection, shows the time, the weather, the news, and whatever you have on today. It has no
keyboard or mouse: **it recognizes your face** and is driven **by gestures**.

The idea is that you never have to ask it for anything. You walk up, it identifies you, and what
you need is already there:

- **Your next appointment** and its reminders.
- **Today's habits** and how much is left to complete them.
- **This month's balance** and where it went.
- **The calories you've eaten** against your plan.
- **Today's workout**, with its plan line by line.

When you leave, it goes to sleep on its own —after 90 s with no face in front of it, a black veil
drops and detection slows to 2 fps— and any face wakes it up. No presence sensor is needed
because the camera is already watching.

**Nothing is loaded from outside.** The vision models and the gesture runtime are served by
Atlas itself: a wall mirror cannot depend on a third-party CDN to let you in.

<div align="center">

## ✨ Features

</div>

| Area | What it offers |
|---|---|
| Identity | Face recognition with liveness proof: it calibrates the neutral pose, asks for a challenge gesture (a fist), and aggregates the three best frontal captures. Enrollment stores five poses and accepts later variants (with and without glasses) |
| Touchless control | Hand gestures on MediaPipe. An open palm cancels; a thumbs-down —or saying "mirror mode"— clears everything and leaves only the reflection |
| Agenda | Appointments, reminders, and calendar, with on-screen alerts |
| Routines | Habits as a quota within a period, with their streak |
| Economy | Movements, balance, and breakdown by category. Movements come in over HTTP (an iOS Shortcut, a script, `curl`) because they mirror the bank |
| Nutrition | Daily plan and intake, with macros and calories typed by hand. Fitia without a food database |
| Training | Exercises, templates by weekday, and set logging. Strong without a catalog: load, reps, time, or distance, and history is never rewritten |
| Ambient | Clock, greeting by name, a quote of the day, weather with a single forecast line ("Rain at 18:00 · 70%"), and headlines refreshed in the background |

Every context publishes its changes over SSE and the screen refreshes on its own.

<div align="center">

## 🏗️ Architecture

</div>

**Hexagonal architecture in rings** (`domain` → `application` → `infrastructure` →
`presentation`), with **tactical DDD** and **CQRS**. It is **a single JPMS module**, and each
bounded context is a subpackage inside each ring:

```text
atlas/domain/          sharedkernel · appointments · presence · routines · economy · nutrition · training
atlas/application/     (the same)
atlas/infrastructure/  (the same)
atlas/presentation/    (the same)
atlas/app/             the wiring of each context + Application.java, the composition root
```

The dependency rule between rings **is not enforced by the compiler** —everything is one
module— but by **ArchUnit**, in the tests.

**Contexts don't know each other.** Each one has its own SQLite database in `data/`
—the isolation is physical— and its own command and query bus. The only thing they share is the
door: `appointments`, `routines`, `economy`, `nutrition`, and `training` return 401 without a
session using the same `SessionGuard`, which checks the `presence` session through a boolean
contract wired in the composition root. There is no internal HTTP and no proxy routes between
contexts.

<div align="center">

## 📦 Project layout

</div>

| Path | Responsibility |
|---|---|
| `src/main/java/atlas/domain/` | Domain model of each context and the shared kernel |
| `src/main/java/atlas/application/` | Commands, queries, and their handlers |
| `src/main/java/atlas/infrastructure/` | SQLite repositories, events, and external adapters |
| `src/main/java/atlas/presentation/` | HTTP handlers, SSE, and DTOs |
| `src/main/java/atlas/app/` | Wiring of each context and the composition root |
| `src/main/resources/web-core/` | The mirror screen: clock, tabs, and camera |
| `src/main/resources/web-presence/` | Vision JS: face quality, gestures, and profile enrollment |
| `src/main/resources/web-shared/` | User-facing error messages, in a single place |
| `src/main/resources/web-<context>/` | Documentation and auxiliary views of each context |
| `src/main/resources/db-migrations/` | One migrations folder per database |
| `src/test/java/atlas/` | Mirror of the structure above |
| `src/test/java/atlas/architecture/rules/` | ArchUnit rules |
| `docs/` | The project's technical source of truth |
| `assets/` | Visual identity: symbol, wordmark, lockups, and favicon |

<div align="center">

## 🛠️ Tech stack

</div>

| Layer | Technologies |
|---|---|
| Language | Java 25 · JPMS · **no frameworks** (no Spring, no IoC container) |
| HTTP server | `jdk.httpserver` on virtual threads · SSE for push · loopback only |
| Persistence | SQLite (`org.xerial:sqlite-jdbc`) · one database per context |
| JSON | `jackson-jr` |
| Screen | HTML + CSS + JavaScript with no build step or bundler · IIFE modules |
| Vision | Human (face detection and descriptors) · MediaPipe Tasks Vision (gestures) |
| Tests | JUnit 5 · AssertJ · Mockito · ArchUnit · PIT (mutation testing) |
| Build | Gradle 9 · Spotless |

The process runs with a **96 MB heap** and the serial GC: it's a mirror, not a server.

<div align="center">

## 🚀 Getting started

</div>

Requirements: **JDK 25** and a **camera** accessible from the browser (the screen opens on
`localhost`, which is a secure context). The Gradle wrapper is included in the repository.

```bash
git clone <repository-url>
cd atlas

./gradlew build   # compiles, applies the formatter, and runs the tests
./gradlew run     # starts the server at http://localhost:8080
```

The first build downloads the vision models (~43 MB) into `build/generated-resources` with
`gradlew downloadWebVendor`; they are not in the repository and go into the jar as just another
resource.

<div align="center">

## 🧪 Testing and quality gates

</div>

```bash
./gradlew check   # unit tests, integration tests, and ArchUnit rules
./gradlew pitest  # mutation testing over atlas.domain.* (~30 s, not part of check)
```

| Gate | Tool | Enforces |
|---|---|---|
| Formatting | Spotless | Consistent style across the codebase |
| Architecture | ArchUnit | Dependencies between rings and isolation between contexts |
| Tests | JUnit 5 + AssertJ | 1810 passing tests, including integration tests against real SQLite |
| Mutations | PIT | 90% threshold over the domain; currently at 95% |
| Web resources | `WebAssetsTest` | No external URLs in the screen's `.html`, `.js`, or `.css` |
| Messages | `ErrorMessagesTest` | Every domain error code has a user-facing translation |

<div align="center">

## 🔬 Status

</div>

All six contexts are wired and in use. `economy` (budgets and savings goals) and `training`
(progression: personal bests, per-exercise history, and volume) have cycles designed but not yet
implemented.

<div align="center">

## 📚 Documentation

</div>

The technical documentation is written in Spanish.

| Document | Covers |
|---|---|
| [CLAUDE.md](CLAUDE.md) | General project context |
| [docs/architecture.md](docs/architecture.md) | Layers, patterns, and code organization |
| [docs/stack.md](docs/stack.md) | Languages, libraries, and tools |
| [docs/ddd-conventions.md](docs/ddd-conventions.md) | DDD building blocks |
| [docs/cqrs-conventions.md](docs/cqrs-conventions.md) | Commands, queries, and how they are dispatched |
| [docs/id-conventions.md](docs/id-conventions.md) | ID typing |
| [docs/error-conventions.md](docs/error-conventions.md) | Error/Result, error catalog, and HTTP translation |
| [docs/rich-domain-conventions.md](docs/rich-domain-conventions.md) | How to avoid an anemic domain |
| [docs/repository-conventions.md](docs/repository-conventions.md) | Shape of the repositories |
| [docs/enum-conventions.md](docs/enum-conventions.md) | Smart enums |
| [docs/mapping-conventions.md](docs/mapping-conventions.md) | From domain to DTOs |
| [docs/testing-conventions.md](docs/testing-conventions.md) | Naming, structure, mocks, and coverage |
| [docs/logging-conventions.md](docs/logging-conventions.md) | What gets logged and how |
| [docs/conventions.md](docs/conventions.md) | Code, naming, and workflow |
| [assets/README.md](assets/README.md) | Visual identity, palette, and logo usage |

---

<div align="center">
  <img src="./assets/public-svg/symbol/symbol-color.svg" alt="Atlas symbol" width="48" />
  <br />
  <em>Atlas: you look up, and it's already there.</em>
</div>
