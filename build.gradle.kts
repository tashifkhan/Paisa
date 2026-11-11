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


// parser-core ships SupportedBanksDocTest and BankSamplesDocTest, which assert
// pennywise repo docs (docs/supported-banks.json, README markers, pennywise-web).
// Those files are not part of Paisa, so skip the tests here. Kept outside
// parser-core so a later parser-core sync does not overwrite this.
subprojects {
    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        filter {
            excludeTestsMatching("com.pennywiseai.parser.core.SupportedBanksDocTest")
            excludeTestsMatching("com.pennywiseai.parser.core.BankSamplesDocTest")
        }
    }
}
