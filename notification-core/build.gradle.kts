// Reads payment notifications from bank and UPI apps, and decides when two
// channels (SMS, bank app, payment app) reported the same charge. Pure Kotlin:
// the app hands it notification text, so it has no Android dependency.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    // Same bytecode level the root build forces on :parser-core, so app unit tests load it.
    jvmToolchain(11)
}

dependencies {
    api(project(":parser-core"))
    testImplementation(libs.junit)
}
