import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.library") apply false
}

val nativeOnly = providers.gradleProperty("dzlogSharedOnly").map(String::toBoolean).getOrElse(false)
if (!nativeOnly) pluginManager.apply("com.android.library")

kotlin {
    if (!nativeOnly) {
        androidTarget { compilerOptions.jvmTarget.set(JvmTarget.JVM_17) }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "DZlogUi"
            isStatic = false
            binaryOption("bundleId", "com.dudoziworkshop.dzlog.poc.ui")
        }
    }
    jvmToolchain(21)
    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
    }
}

if (!nativeOnly) extensions.configure<LibraryExtension> {
    namespace = "com.dudoziworkshop.dzlog.sharedui"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
