import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.room3)
}

// The web build gets android.defaultConfig.versionName as a generated
// constant, so About shows the same version on both. The release workflow
// bumps versionName below; keep it a plain string literal.
val generateWebVersion by tasks.registering {
    val version = provider { android.defaultConfig.versionName ?: "dev" }
    val out = layout.buildDirectory.dir("generated/webVersion/kotlin")
    inputs.property("version", version)
    outputs.dir(out)
    doLast {
        val file = out.get().file("codes/tashif/paisa/WebVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package codes.tashif.paisa\n\n" +
                "/** Generated from versionName in app/build.gradle.kts. */\n" +
                "internal const val WEB_VERSION_NAME = \"${version.get()}\"\n"
        )
    }
}

// One Compose Multiplatform app. commonMain holds the screens, viewmodels,
// database and sync; androidMain adds SMS, widgets, WorkManager, biometrics
// and Google sign-in; wasmJsMain is the web host.
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

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
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        wasmJsMain {
            kotlin.srcDir(generateWebVersion)
        }
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.room3.runtime)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.ui)
            implementation(libs.cmp.material3)
            implementation(libs.cmp.material.icons.extended)
            implementation(compose.components.resources)
            implementation(libs.jb.lifecycle.viewmodel.compose)
            implementation(libs.jb.lifecycle.runtime.compose)
        }
        androidMain.dependencies {
            implementation(project(":parser-core"))
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.workmanager)
            implementation(libs.koin.compose.viewmodel)

            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.fragment.ktx)
            implementation(libs.androidx.compose.material.icons.core)
            implementation(libs.androidx.compose.material.icons.extended)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.graphics)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.androidx.glance.appwidget)
            implementation(libs.androidx.glance.material3)
            implementation(libs.androidx.graphics.shapes)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.okhttp)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.security.crypto)
            implementation(libs.pdfbox.android)
            // Google Drive backup sign-in (AuthorizationClient)
            implementation(libs.play.services.auth)
        }
        androidUnitTest.dependencies {
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.core)
            implementation(libs.androidx.junit)
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            // Real org.json for JVM unit tests (Android stubs throw at runtime)
            implementation("org.json:json:20240303")
            // Real SQLite + android.util for the database tests
            implementation("org.robolectric:robolectric:4.16")
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.ktor.client.js)
            implementation(libs.sqlite.web)
            // SQLite compiled to Wasm, run by sqlite-worker/worker.js.
            // webpack.config.d/sqlite-worker.js points webpack at that file.
            implementation(npm("@sqlite.org/sqlite-wasm", "3.54.1-build1"))
        }
    }
}

android {
    namespace = "codes.tashif.paisa"
    compileSdk = 36

    defaultConfig {
        applicationId = "codes.tashif.paisa"
        minSdk = 24
        targetSdk = 35
        versionCode = 8
        versionName = "1.5.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    add("kspAndroid", libs.room3.compiler)
    add("kspWasmJs", libs.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    packageOfResClass = "codes.tashif.paisa.resources"
    publicResClass = false
}
