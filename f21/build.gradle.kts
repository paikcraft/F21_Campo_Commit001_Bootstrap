plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "br.f21campo.f21"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
