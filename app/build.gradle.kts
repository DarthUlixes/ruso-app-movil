import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.1.0"
}

android {
    namespace = "com.example.rusoit"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.rusoit"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Permite que el Manifiesto lea la clave desde local.properties
        val properties = Properties()
        val propertiesFile = project.rootProject.file("local.properties")
        if (propertiesFile.exists()) {
            propertiesFile.inputStream().use { properties.load(it) }
        }
        manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY") ?: "TU_API_KEY_AQUI"
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
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.material.icons.extended)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlin.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    // UI Extras
    implementation(libs.coil.compose)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.maps.compose)

    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}