plugins {
    `java-library`
    application
    id("com.diffplug.spotless") version "8.9.0"
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

// Gradle canaliza la salida del proceso hacia su demonio, asi que System.console() es
// null y la deteccion automatica elegiria el formato plano. En una ejecucion de
// desarrollo queremos el formato de consola.
tasks.named<JavaExec>("run") {
    systemProperty("log.format", "console")
}
