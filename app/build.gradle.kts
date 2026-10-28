plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.tracer)
}

android {
    namespace = "com.example.tracertest"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.tracertest"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        // Tracer встраивает токены через ресурсы, в AGP 9 фича выключена по умолчанию.
        resValues = true
    }
}

// Токены тестовые: загрузка маппинга в Tracer не выполняется, ловить нечего.
tracer {
    create("defaultConfig") {
        appToken = "TEST_APP_TOKEN_0000000000000000000000000"
        pluginToken = "TEST_PLUGIN_TOKEN_0000000000000000000000"
        uploadMapping = true
    }
    create("debug") {
        isDisabled = true
    }
}

dependencies {
    implementation(platform(libs.tracer.platform))
    implementation(libs.tracer.crash.report)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}