plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.boompala.engine"
    compileSdk = 35
    buildToolsVersion = "34.0.0"

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.all {
            it.systemProperty(
                "yaoTextAssetPath",
                file("src/main/assets/yao_text.json").absolutePath,
            )
            it.systemProperty(
                "hexagramInterpretationAssetPath",
                file("src/main/assets/hexagram_interpretations.json").absolutePath,
            )
        }
    }
}

dependencies {
    implementation("cn.6tail:lunar:1.7.7")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation("junit:junit:4.13.2")
}
