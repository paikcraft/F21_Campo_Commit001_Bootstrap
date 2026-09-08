plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "br.f21campo.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "br.f21campo"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.1.2-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "mode"
    productFlavors {
        create("operational") {
            dimension = "mode"
            buildConfigField("String", "BUILD_MODE", "\"OPERATIONAL\"")
        }
        create("lab") {
            dimension = "mode"
            applicationIdSuffix = ".lab"
            versionNameSuffix = "-lab"
            buildConfigField("String", "BUILD_MODE", "\"LAB\"")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":files"))
    implementation(project(":rinex"))
    implementation(project(":processing"))
    implementation(project(":f21"))
    implementation(project(":hnproject"))
    implementation(project(":receiver-api"))
    implementation(project(":receiver-manual"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
