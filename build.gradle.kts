// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
}

// Paisa-only Gradle overrides for the vendored parser-core module.
// Kept here (outside parser-core/) so a later upstream sync does not overwrite them.
subprojects {
    if (name != "parser-core") return@subprojects

    // Skip pennywise doc-catalogue tests: they assert repo-root docs
    // (docs/supported-banks.json, README markers, pennywise-web) that are
    // not part of Paisa.
    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        filter {
            excludeTestsMatching("com.pennywiseai.parser.core.SupportedBanksDocTest")
            excludeTestsMatching("com.pennywiseai.parser.core.BankSamplesDocTest")
        }
    }

    afterEvaluate {
        // Upstream parser-core uses jvmToolchain(21). Paisa's app and shared
        // modules target JVM 11, and CI runs on JDK 17, so app unit tests
        // cannot load Java 21 bytecode (UnsupportedClassVersionError in
        // CommonParityTest.md5MatchesParserCore). Emit JVM 11 bytecode.
        extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
            ?.jvmToolchain(11)
        extensions.findByType<JavaPluginExtension>()?.apply {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
            compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
        tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = JavaVersion.VERSION_11.toString()
            targetCompatibility = JavaVersion.VERSION_11.toString()
        }

        // PaisaCanaraAbbreviatedDebitTest is JUnit 4; upstream tests are Jupiter.
        val kotlinExt = extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
        kotlinExt?.sourceSets?.findByName("jvmTest")?.dependencies {
            implementation(libs.junit)
            runtimeOnly("org.junit.vintage:junit-vintage-engine:5.10.0")
        }
    }
}
