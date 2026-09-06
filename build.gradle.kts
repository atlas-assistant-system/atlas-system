import java.net.URI

plugins {
    `java-library`
    application
    id("com.diffplug.spotless") version "8.9.0"
    id("info.solidsoft.pitest") version "1.19.0-rc.1"
}

description = "Lo que necesitas ver, cuando levantas la vista"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")
    implementation("com.fasterxml.jackson.jr:jackson-jr-objects:2.22.2")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")
}

application {
    mainModule = "atlas"
    mainClass = "atlas.app.Main"
    applicationDefaultJvmArgs = listOf(
        "-Xms96m",
        "-Xmx96m",
        "-Xss256k",
        "-XX:+UseSerialGC",
        "-XX:MaxMetaspaceSize=64m",
        "--enable-native-access=org.xerial.sqlitejdbc")
}

/*
 * Los modelos y los runtimes de vision se sirven desde el propio Atlas, no desde un CDN. Un
 * espejo colgado en la pared no puede depender de que jsdelivr este levantado para dejarte
 * entrar, y son ficheros inmutables clavados a una version. No van al repositorio: son ~44 MB
 * de binarios que git guardaria para siempre y que no se pueden diffear, asi que los baja el
 * build a `build/generated-resources` y de ahi entran al jar como un recurso mas.
 */
val humanVersion = "3.3.6"
val mediapipeVersion = "1.0.1"
val humanCdn = "https://cdn.jsdelivr.net/npm/@vladmandic/human@$humanVersion"
val mediapipeCdn = "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@$mediapipeVersion"

// Solo los modelos que enciende la configuracion de `face` en las tres paginas: detector, malla,
// descriptor, antispoof y liveness. Iris, emocion, cuerpo, mano y objetos estan desactivados.
val webVendorAssets = mapOf(
    "human/human.js" to "$humanCdn/dist/human.js",
    "human/models/models.json" to "$humanCdn/models/models.json",
    "human/models/blazeface.json" to "$humanCdn/models/blazeface.json",
    "human/models/blazeface.bin" to "$humanCdn/models/blazeface.bin",
    "human/models/facemesh.json" to "$humanCdn/models/facemesh.json",
    "human/models/facemesh.bin" to "$humanCdn/models/facemesh.bin",
    "human/models/faceres.json" to "$humanCdn/models/faceres.json",
    "human/models/faceres.bin" to "$humanCdn/models/faceres.bin",
    "human/models/antispoof.json" to "$humanCdn/models/antispoof.json",
    "human/models/antispoof.bin" to "$humanCdn/models/antispoof.bin",
    "human/models/liveness.json" to "$humanCdn/models/liveness.json",
    "human/models/liveness.bin" to "$humanCdn/models/liveness.bin",
    "mediapipe/vision_bundle.mjs" to "$mediapipeCdn/vision_bundle.mjs",
    // Las dos variantes: `FilesetResolver` elige simd o nosimd segun el navegador, y si escoge
    // la que falta se queda sin seguimiento de manos y sin forma de decirlo.
    "mediapipe/wasm/vision_wasm_internal.js" to "$mediapipeCdn/wasm/vision_wasm_internal.js",
    "mediapipe/wasm/vision_wasm_internal.wasm" to "$mediapipeCdn/wasm/vision_wasm_internal.wasm",
    "mediapipe/wasm/vision_wasm_nosimd_internal.js" to "$mediapipeCdn/wasm/vision_wasm_nosimd_internal.js",
    "mediapipe/wasm/vision_wasm_nosimd_internal.wasm" to "$mediapipeCdn/wasm/vision_wasm_nosimd_internal.wasm",
    "mediapipe/gesture_recognizer.task" to
        "https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/1/gesture_recognizer.task")

val webVendorDirectory = layout.buildDirectory.dir("generated-resources")

val downloadWebVendor = tasks.register("downloadWebVendor") {
    group = "build"
    description = "Baja los modelos de Human y el runtime de MediaPipe para servirlos desde Atlas."

    val assets = webVendorAssets
    val target = webVendorDirectory
    outputs.dir(target)
    outputs.upToDateWhen {
        assets.keys.all { target.get().file("web-vendor/$it").asFile.length() > 0 }
    }

    doLast {
        assets.forEach { (path, url) ->
            val file = target.get().file("web-vendor/$path").asFile
            if (file.length() > 0) {
                return@forEach
            }
            logger.lifecycle("Descargando $path")
            file.parentFile.mkdirs()
            val partial = File(file.parentFile, file.name + ".part")
            URI(url).toURL().openStream().use { input ->
                partial.outputStream().use { output -> input.copyTo(output) }
            }
            // Renombrado al final: una descarga cortada a medias dejaba un fichero corto que
            // parecia bueno y el navegador fallaba al cargar el modelo sin decir por que.
            partial.renameTo(file)
        }
    }
}

sourceSets.main {
    resources.srcDir(webVendorDirectory)
}

tasks.processResources {
    dependsOn(downloadWebVendor)
}

spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    java {
        target("src/**/*.java")
        eclipse().configFile(file("config/formatter.properties"))
        importOrder("\\#", "")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.test {
    useJUnitPlatform()
}

pitest {
    pitestVersion = "1.25.9"
    junit5PluginVersion = "1.2.3"
    targetClasses = setOf("atlas.domain.*")
    targetTests = setOf("atlas.domain.*")
    excludedClasses = setOf("atlas.domain.sharedkernel.*")
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
    threads = 4
    mutationThreshold = 90
}

tasks.named<JavaExec>("run") {
    systemProperty("log.format", "console")
}

tasks.register<JavaExec>("seed") {
    group = "application"
    description = "Llena training, routines, nutrition y appointments con lo minimo para ver el espejo vivo. Cada contexto se salta si ya tiene datos."
    mainModule = "atlas"
    mainClass = "atlas.app.Seeder"
    classpath = files(tasks.named("jar")) + configurations.runtimeClasspath.get()
    systemProperty("log.format", "console")
    jvmArgs("--enable-native-access=org.xerial.sqlitejdbc")
}

tasks.register<JavaExec>("reseed") {
    group = "application"
    description = "Borra training, routines, nutrition y appointments y los siembra de cero. No toca economy ni presence."
    mainModule = "atlas"
    mainClass = "atlas.app.Seeder"
    args("--reset")
    classpath = files(tasks.named("jar")) + configurations.runtimeClasspath.get()
    systemProperty("log.format", "console")
    jvmArgs("--enable-native-access=org.xerial.sqlitejdbc")
}

tasks.register<JavaExec>("seedEconomy") {
    group = "application"
    description = "Llena data/economy.db con datos de ejemplo. No hace nada si ya tiene movimientos."
    mainModule = "atlas"
    mainClass = "atlas.app.economy.EconomySeeder"
    classpath = files(tasks.named("jar")) + configurations.runtimeClasspath.get()
    systemProperty("log.format", "console")
    jvmArgs("--enable-native-access=org.xerial.sqlitejdbc")
}

tasks.register<JavaExec>("importEconomy") {
    group = "application"
    description = "Importa un extracto bancario en CSV a data/economy.db. No hace nada si ya tiene movimientos."
    mainModule = "atlas"
    mainClass = "atlas.app.economy.EconomyImporter"
    classpath = files(tasks.named("jar")) + configurations.runtimeClasspath.get()
    systemProperty("log.format", "console")
    jvmArgs("--enable-native-access=org.xerial.sqlitejdbc")
}
