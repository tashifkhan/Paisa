import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.room3)
}

// Paisa in the browser: Compose Multiplatform on Kotlin/Wasm, with Room 3
// keeping its database in the origin private file system.
kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("paisa-web")
        browser {
            commonWebpackConfig {
                outputFileName = "paisa-web.js"
            }
        }
        binaries.executable()
    }

    compilerOptions {
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.room3.runtime)
            implementation(libs.sqlite.web)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            // SQLite compiled to Wasm, run by sqlite-worker/worker.js.
            // webpack.config.d/sqlite-worker.js points webpack at that file.
            implementation(npm("@sqlite.org/sqlite-wasm", "3.54.1-build1"))
        }
    }
}

dependencies {
    add("kspWasmJs", libs.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
