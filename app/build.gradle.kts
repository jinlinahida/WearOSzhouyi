plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.boompala"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "com.boompala"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            // AGP 8.7/R8 8.7 spends several minutes in whole-program analysis
            // for this Wear/Compose graph. Keep the Release variant stable and
            // installable while the toolchain is upgraded; Debug remains the
            // full-featured development build.
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":engine"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.animation:animation:1.9.0")
    implementation("androidx.compose.ui:ui:1.9.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.wear.compose:compose-foundation:1.6.2")
    implementation("androidx.wear.compose:compose-material3:1.6.2")
    implementation("androidx.graphics:graphics-shapes:1.0.1")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.datastore:datastore-preferences:1.1.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.0")
}
