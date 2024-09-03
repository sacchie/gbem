plugins {
    kotlin("multiplatform") version "2.0.20"
}

group = "gbem-wasm"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("../src/main/kotlin/emulator/")
        }
    }

    wasmJs {
        binaries.executable()
        nodejs()
    }
    jvmToolchain(17)
}
