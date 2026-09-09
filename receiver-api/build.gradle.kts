plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "br.f21campo.receiver.api"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":domain"))
    testImplementation(libs.junit4)
}
