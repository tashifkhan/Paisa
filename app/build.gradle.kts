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
        val file = out.get().file("sh/taf/billi/WebVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            "package sh.taf.billi\n\n" +
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
        outputModuleName.set("billi-web")
        browser {
            commonWebpackConfig {
                outputFileName = "billi-web.js"
            }
            // commonTest runs in headless Chrome, so the backup container is
            // tested against the browser's own WebCrypto.
            testTask {
                useKarma {
                    useChromeHeadless()
                }
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
            // Backdrop blur for the frosted floating navbar.
            implementation(libs.haze)
            implementation(libs.haze.blur)
        }
        androidMain.dependencies {
            implementation(project(":parser-core"))
            implementation(project(":notification-core"))
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
            implementation(libs.androidx.exifinterface)
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.guava)
            // Google Drive backup sign-in (AuthorizationClient)
            implementation(libs.play.services.auth)
            // On-device Gemini Nano through AICore. Guarded to Android 8+ at runtime.
            implementation(libs.mlkit.genai.prompt)
            // Gemini and other assistants call Billi's actions through AppFunctions (Android 16+).
            implementation(libs.androidx.appfunctions)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
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
            implementation("org.robolectric:robolectric:4.18.1")
            // Runs the generated AppFunction service the way an assistant would.
            implementation(libs.androidx.appfunctions.testing)
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.ktor.client.js)
            implementation(libs.sqlite.web)
            // SQLite compiled to Wasm, run by src/wasmJsMain/sqlite-worker/worker.js.
            // webpack.config.d/sqlite-worker.js points webpack at that file.
            implementation(npm("@sqlite.org/sqlite-wasm", "3.54.1-build1"))
        }
    }
}

android {
    namespace = "sh.taf.billi"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "sh.taf.billi"
        minSdk = 24
        targetSdk = 36
        versionCode = 21
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // -PpaisaSideBySide builds a debug copy that installs beside the real app
        // (sh.taf.billi.audit, "Paisa Audit") with its own data, for testing on
        // a phone that already has Paisa with real transactions.
        debug {
            if (providers.gradleProperty("paisaSideBySide").isPresent) {
                applicationIdSuffix = ".audit"
                versionNameSuffix = "-audit"
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    if (providers.gradleProperty("paisaSideBySide").isPresent) {
        sourceSets["debug"].res.srcDir("src/androidSideBySide/res")
    }
    // Debug-only deep links, such as billi://debug/seed-demo.
    sourceSets["debug"].manifest.srcFile("src/androidDebug/AndroidManifest.xml")
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

// Robolectric needs Java 21 to simulate Android 16, where the AppFunctions
// tests run. Only the unit tests use it; the app still builds on Java 17.
val javaToolchains = extensions.getByType<JavaToolchainService>()
tasks.withType<Test>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
}

dependencies {
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    add("kspAndroid", libs.room3.compiler)
    add("kspAndroid", libs.androidx.appfunctions.compiler)
    add("kspWasmJs", libs.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    packageOfResClass = "sh.taf.billi.resources"
    publicResClass = false
}
