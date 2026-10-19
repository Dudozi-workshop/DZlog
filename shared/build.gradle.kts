import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    // Pure Kotlin JVM artifact consumed by the existing Android app.
    // No Android/Compose plugin upgrade is needed for this first extraction.
    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(21)

    sourceSets {
        jvmMain.dependencies { compileOnly("org.json:json:20240303") }
        jvmTest.dependencies { implementation("org.json:json:20240303") }
        iosMain.dependencies { implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3") }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
