# Atlas

Lo que necesitas ver, cuando levantas la vista

Pantalla ambiental permanente: reloj, estado y avisos sobre fondo de camara en espejo, con vistas conmutables y control sin contacto.

## Empezar

Requiere **JDK 25**. El wrapper de Gradle está incluido en el repositorio.

```
./gradlew build   # compila, aplica el formatter y pasa los tests de arquitectura
./gradlew run     # arranca el servidor HTTP en http://localhost:8080
```

El código vive todo bajo `src/main/java`, con un paquete por anillo de la arquitectura
(`domain`, `application`, `infrastructure`, `presentation`) más `app`, el composition
root. Los tests están en `src/test/java`, replicando esa misma estructura.

## Documentación

- [CLAUDE.md](CLAUDE.md) — contexto general del proyecto
- [docs/architecture.md](docs/architecture.md)
- [docs/stack.md](docs/stack.md)
- [docs/ddd-conventions.md](docs/ddd-conventions.md)
- [docs/cqrs-conventions.md](docs/cqrs-conventions.md)
- [docs/id-conventions.md](docs/id-conventions.md)
- [docs/error-conventions.md](docs/error-conventions.md)
- [docs/rich-domain-conventions.md](docs/rich-domain-conventions.md)
- [docs/repository-conventions.md](docs/repository-conventions.md)
- [docs/enum-conventions.md](docs/enum-conventions.md)
- [docs/mapping-conventions.md](docs/mapping-conventions.md)
- [docs/testing-conventions.md](docs/testing-conventions.md)
- [docs/validation-specification-conventions.md](docs/validation-specification-conventions.md)
- [docs/conventions.md](docs/conventions.md)
