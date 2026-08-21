# Convenciones de desarrollo

> Documento compartido: gestionado por el harness y sincronizado en todos los proyectos.
> No lo edites manualmente en este repo — los cambios se sobrescribirán en el próximo `sync`.

## Cero comentarios en el código

El código no lleva comentarios: ni Javadoc, ni comentarios de línea, ni de bloque. El
código debe explicarse solo — nombres de clases, métodos y variables con intención
(ver "Lenguaje ubicuo" en `ddd-conventions.md`), métodos cortos, y estructura clara.

Si un fragmento parece necesitar un comentario para entenderse, la solución es
renombrar o extraer, no comentar. Un comentario que explica *qué* hace el código es
ruido; uno que explica *por qué* suele ser señal de que falta un nombre mejor o un
test que lo documente.

Matices:

- Los ejemplos de código dentro de la documentación del harness (`docs/*.md`) sí
  pueden llevar comentarios pedagógicos (`// MAL — ...`) — son material didáctico, no
  código de producción.
- En tests, los separadores `// Given / When / Then` están permitidos cuando el test
  no es evidente por sí mismo (ver `testing-conventions.md`); es la única excepción en
  código real.
- `module-info.java`, ficheros de build y configuración siguen la misma regla.

## Formato del código: Spotless + Eclipse JDT

El formato no se discute ni se revisa a mano: lo impone el build.

```kotlin
plugins {
    id("com.diffplug.spotless") version "8.9.0"
}

spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    java {
        target("src/**/*.java")
        eclipse().configFile("config/formatter.properties")
        importOrder("\\#", "")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
```

- `gradle spotlessApply` reformatea; `gradle spotlessCheck` verifica.
- Spotless engancha `spotlessCheck` a la tarea `check`, así que **el build falla si algo
  no está formateado**. La convención se impone sola, igual que JPMS impone la regla de
  dependencia — no depende de que nadie se acuerde.
- La configuración vive en `config/formatter.properties`, fichero compartido que
  propaga el harness. No se edita en el módulo.

### Reglas que fija el formatter

| Regla | Valor |
|---|---|
| Indentación | 4 espacios, sin tabs |
| Continuación de línea | 4 espacios (un nivel), no 8 |
| Ancho máximo | 120 columnas |
| Llaves | K&R (apertura en la misma línea) |
| Línea en blanco tras la cabecera del tipo | Sí, antes del primer miembro |
| Líneas en blanco consecutivas | Máximo 1 |
| Cuerpos vacíos | En una línea: `public interface ValueObject {}` |
| Constantes de enum | Una por línea |
| Imports | Estáticos primero, luego el resto en un único bloque alfabético; sin wildcards; los no usados se eliminan |
| Fin de línea | LF, en todas las plataformas |

## Respiración del código (esto el formatter NO lo hace por ti)

Un formatter normaliza tipografía, pero **no puede insertar líneas en blanco entre
sentencias**: decidir dónde termina un paso lógico y empieza el siguiente es una
decisión semántica. El nuestro las **preserva** (máximo una consecutiva) pero nunca las
genera. Es decir: la legibilidad del cuerpo de un método sigue siendo responsabilidad
de quien lo escribe.

Regla: **una línea en blanco entre pasos lógicos del método**. En la práctica:

- Separa el bloque de guards/validaciones del cuerpo real.
- Separa cada guard independiente del siguiente.
- Separa el `return` final cuando el método tiene varios pasos.
- Una declaración que solo sirve al guard inmediatamente siguiente va pegada a él.
- Un método de una o dos sentencias no necesita ninguna separación.

```java
public static String format(char prefix, int numericLength, long value) {
    if (value < 0) {
        throw GuardException.forParameter("value", "cannot be negative");
    }

    long maxValue = (long) Math.pow(10, numericLength) - 1;
    if (value > maxValue) {
        throw GuardException.forParameter("value", "exceeds maximum (" + maxValue + ")");
    }

    return prefix + String.format("%0" + numericLength + "d", value);
}
```

Esta convención es especialmente importante aquí porque el código no lleva comentarios:
las líneas en blanco son lo único que marca visualmente la estructura de un método.
También aplica al Given/When/Then de los tests (ver `testing-conventions.md`).

> **Nota sobre el formato `.properties`:** la configuración es un fichero de propiedades,
> no el XML de export de Eclipse. Spotless acepta ambos, pero el XML fallaba con un
> `InvocationTargetException` opaco; el formato properties funciona y además es
> diffeable. Ojo con los valores: `keep_loop_body_block_on_one_line` y
> `keep_if_then_body_block_on_one_line` esperan `one_line_never`, no `never` — un valor
> inválido rompe el formatter en *todos* los ficheros con el mismo error opaco.

## Nada de tipos anidados

**Un tipo, un fichero.** No se declaran `record`, `class`, `enum` ni `interface` dentro
de otro tipo, ni como miembro privado ni estático.

```java
public final class NewsHandlers {

    private record Source(NewsCategory category) { }

    private record NewsItem(NewsCategory category, String title, String url) { }
}
```

Eso mismo son dos ficheros al lado del handler: `Source.java` y `NewsItem.java`. Si el
tipo solo tiene sentido dentro del paquete, se deja *package-private* — la visibilidad
la da el paquete, no el anidamiento.

El motivo es que un tipo anidado esconde un concepto del dominio dentro de otro:
`NewsItem` no se encuentra buscando `NewsItem.java`, no se puede testear ni reutilizar
sin arrastrar la clase contenedora, y el fichero crece hasta que nadie sabe qué hay
dentro. Anidar tampoco ahorra nada: el fichero extra es gratis y el nombre queda a la
vista en el árbol de paquetes.

Vale para todas las capas y también para los tests: los builders, fixtures y datos de
prueba van en su propio fichero, no en una clase interna del test.

Excepción única: los tipos *sellados* cuyas variantes son la definición del propio tipo
y no existen fuera de él (`Result` en el kernel es el caso). Ahí el anidamiento es la
forma de decir "estas son todas las variantes que hay".

> Hoy quedan ~20 tipos anidados de antes de fijar esta regla. Se van sacando a su
> fichero cuando se toque el código que los contiene; no hay migración en bloque.

## Mensajes de commit

**Conventional Commits, una sola línea, sin cuerpo, y en inglés.**

```
<tipo>(<ámbito>): <qué hace, en minúscula y sin punto final>
```

```
feat(appointments): schedule appointment with overlap validation
fix(appointments): stop publishing events after a rollback
refactor(kernel): split guards by guarded type
docs(repositories): document migration ordering rules
test(appointments): cover rejection of cancelled appointments
chore(build): bump the formatter version
```

El mensaje va **en inglés**, igual que el código: el historial es parte del repositorio,
no de la documentación. Esta misma documentación sigue en español.

Tipos: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `build`.

El **ámbito** es el bounded context (`appointments`, `reminders`) o la pieza
transversal que se toca (`kernel`, `build`, `ci`).

Reglas:

- **Una línea y punto.** Sin cuerpo, sin lista de cambios, sin pie. Si el mensaje no
  cabe en una línea, casi siempre es que el commit hace dos cosas y toca partirlo.
- **En presente y en imperativo**, describiendo qué hace el cambio, no qué hiciste tú.
- **Sin `Co-Authored-By` ni ninguna otra firma automática.** El historial lleva autoría
  de quien firma el commit y nada más.
- Un commit = un cambio con sentido propio. Que el proyecto compile en cada uno.

## Pendiente de definición

- Naming general más allá de lo ya fijado en los docs específicos (DDD, CQRS, IDs,
  enums).
- Si se añade además un linter de reglas semánticas (Checkstyle/PMD/Error Prone) o
  basta con el formatter + ArchUnit.
- Definición de "hecho" (qué tiene que cumplir un cambio para considerarse terminado).
