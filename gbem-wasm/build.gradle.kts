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
            kotlin.srcDir("../gbem-lib/src/main/kotlin/")
        }
    }

    wasmJs {
        binaries.executable()
        nodejs()
    }
    jvmToolchain(17)
}
