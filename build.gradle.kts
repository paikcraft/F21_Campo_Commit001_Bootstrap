buildscript {
    dependencies {
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin") {
            version { strictly("2.3.11") }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}
