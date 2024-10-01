plugins {
    kotlin("jvm") version "1.9.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":gbem-lib"))
}

application {
    mainClass.set("MainKt")
}