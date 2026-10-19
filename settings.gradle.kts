@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DZlog"
// Native-only checks do not configure the Android application or require an Android SDK.
if (!providers.gradleProperty("dzlogSharedOnly").map(String::toBoolean).getOrElse(false)) {
    include(":app")
}

include(":shared")
