import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// Code shared by the Android app and the web app: the .paisa container,
// its crypto, and the sync file format. Android uses the JVM target.
kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    wasmJs {
        // Node runs the tests. It has the same WebCrypto and
        // DecompressionStream as a browser, and the web app still links
        // this target into its browser bundle.
        nodejs()
    }

    compilerOptions {
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
