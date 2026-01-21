plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // ✅ Kotlin 2.0+ 필수
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.dzlog"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dzlog"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // ✅ Kotlin 2.0에서는 compose compiler plugin이 관리하므로
    // kotlinCompilerExtensionVersion을 명시하지 않아도 된다(오히려 충돌 원인).
    // composeOptions 블록은 제거.

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // Lifecycle Compose (LocalLifecycleOwner)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
}
