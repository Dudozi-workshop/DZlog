import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

extensions.configure<ApplicationExtension> {
    namespace = "com.dudoziworkshop.dzlog"
    compileSdk = 36

    defaultConfig {
        // Filled only for a tested paid release. Empty defaults are fail-closed.
        val billingUrl = providers.gradleProperty("dzlogBillingVerifierUrl").orNull.orEmpty()
        val signingPubkeyB64 = providers.gradleProperty("dzlogBillingPublicKeyB64").orNull.orEmpty()
        require(billingUrl.isEmpty() || billingUrl.startsWith("https://"))
        require(billingUrl.matches(Regex("[A-Za-z0-9:/.?&=_-]*")))
        require(signingPubkeyB64.matches(Regex("[A-Za-z0-9+/_=-]*")))
        buildConfigField("String", "DZLOG_BILLING_URL", "\"$billingUrl\"")
        buildConfigField("String", "DZLOG_BILLING_PUBLIC_KEY_B64", "\"$signingPubkeyB64\"")
        applicationId = "com.dudoziworkshop.dzlog"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.0.0"
        manifestPlaceholders["dzlogAppLabel"] = "@string/app_name"
    }

    // CI restores a persistent test key from GitHub Secrets; release signing is independent.
    providers.environmentVariable("DZLOG_DEBUG_KEYSTORE").orNull?.let { keyPath ->
        signingConfigs.getByName("debug") {
            storeFile = file(keyPath)
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            val galleryStandalone = providers.gradleProperty("dzlogGalleryStandalone").orNull == "true"
            val isolatedQa = providers.gradleProperty("dzlogStandaloneQa").orNull == "true"
            val qaInstanceTag = providers.gradleProperty("dzlogQaInstanceTag").orNull.orEmpty()
            require(!(galleryStandalone && isolatedQa)) {
                "Standalone QA and gallery-only package flags cannot both be enabled."
            }
            require(qaInstanceTag.isEmpty() || (isolatedQa && qaInstanceTag.matches(Regex("[a-z][a-z0-9]{0,15}")))) {
                "QA instance tag must be an ASCII lowercase application ID segment for isolated QA builds."
            }
            if (isolatedQa) {
                // Each CI QA artifact gets a unique install ID. This avoids signature
                // conflicts with an older QA APK built by a different CI runner,
                // while leaving the production application and its data untouched.
                applicationIdSuffix = ".qa" + if (qaInstanceTag.isEmpty()) "" else ".$qaInstanceTag"
                manifestPlaceholders["dzlogAppLabel"] =
                    if (qaInstanceTag.isEmpty()) "DZlog QA" else "DZlog QA $qaInstanceTag"
            } else if (galleryStandalone) {
                applicationIdSuffix = ".gallerydev"
                manifestPlaceholders["dzlogAppLabel"] = "DZlog Gallery"
            }
        }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// CI supplies a monotonically increasing code; release versioning remains independent.
val debugVersionCode = providers.gradleProperty("dzlogDebugVersionCode")
    .map { it.toInt().also { code -> require(code in 1..2_100_000_000) } }
    .orElse(1_000_000)
extensions.configure<ApplicationAndroidComponentsExtension> {
    onVariants(selector().withBuildType("debug")) { variant ->
        variant.outputs.forEach { output ->
            output.versionCode.set(debugVersionCode)
            output.versionName.set(debugVersionCode.map { "0.0.0-test.$it" })
        }
    }
}

extensions.configure<KotlinAndroidProjectExtension> {
    // Build JDK baseline: 21 (toolchain), while emitted bytecode target remains JVM 17.
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    toolchain {
        // Java/KSP tool execution uses JDK 21 for consistent local/CI behavior.
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.reorderable)

    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.concurrent.futures)
    implementation(libs.guava.android)
    implementation(libs.androidx.exifinterface)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.google.billing)
    implementation(libs.google.play.services.ads)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}

