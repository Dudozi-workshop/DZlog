# Shared core foundation — 2026-10-19

Baseline: `d6e53c9a645d3a2a21f21991eb07302db0ae8a78` (completed Android work; onboarding #37 and tutorial #62 excluded).
Source design: https://app.notion.com/p/3f8cfef7d7638141a7f0e52fe7e75ca9

## First extraction

`shared` contains the existing table/cell/style models, slot helpers, phrase selection and cell-local phrase progress. Source packages and public model fields stay unchanged. Android consumes its JVM artifact through `implementation(project(":shared"))`; there is no duplicate Android implementation.

Only cell-ID generation changes location: a common expect function delegates to Java UUID on JVM and Foundation NSUUID on iOS. Both produce lowercase UUID strings. Existing IDs passed into constructors and copied from stored models are preserved. No data/backup schema migration is introduced.

Kotlin remains pinned to the repository's 2.0.21. No Compose, Android, camera, storage or permission upgrade is included. The Android app stays in `app`; it is not renamed or rebuilt as a second app.

## Checks

- `./gradlew :shared:jvmTest :app:testDebugUnitTest`: shared tests plus the existing Android regression suite. The six existing phrase resolver cases move to commonTest with kotlin.test. Android integration tests remain in app.
- macOS: `./gradlew -PdzlogSharedOnly=true :shared:compileKotlinIosArm64 :shared:compileTestKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 :shared:compileTestKotlinIosSimulatorArm64`.
- `dzlogSharedOnly=true` excludes configuration of the Android application for native-only checks. It is not used for Android release builds.

Native compilation is not a simulator/device test or a finished iOS app. No iOS host app, camera adapter, image renderer, PhotoKit/Files integration, Compose screen migration or TestFlight delivery is included in this checkpoint.

## Next boundary

CounterScopeResolver depends on resolved table cells and time policy; filename/preview calculation uses Java dates. Those dependencies remain in app until their contracts are extracted with behavior tests. Existing .dzlog codecs and persistence stay in Android for now. Common UI will be evaluated separately with Compose Multiplatform, following the existing design.
