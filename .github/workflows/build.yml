name: CI

on:
  push:
    branches: [main]
    paths-ignore:
      - '**.md'
      - 'docs/**'
      - '.gitignore'
  pull_request:
    branches: [main]
    paths-ignore:
      - '**.md'
      - 'docs/**'
      - '.gitignore'

jobs:
  build:
    name: Build, format and architecture checks
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '25'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      # `build` encadena compilacion, spotlessCheck, tests unitarios y reglas de
      # arquitectura: si pasa, el modulo cumple todas las convenciones verificables.
      - name: Build
        run: ./gradlew build --no-daemon

      - name: Publish test report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: '**/build/reports/tests/test'
          if-no-files-found: ignore
