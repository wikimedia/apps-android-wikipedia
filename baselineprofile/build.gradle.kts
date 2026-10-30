import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "org.wikipedia.baselineprofile"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // Generating a profile requires API 33+, or API 28+ on a rooted device/emulator.
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Profiles are generated against the alpha flavor only, and merged into the app's main source set.
        missingDimensionStrategy("default", "alpha")
    }

    targetProjectPath = ":app"

    testOptions.managedDevices.localDevices {
        create("pixel6Api35") {
            device = "Pixel 6"
            apiLevel = 35
            systemImageSource = "google_apis_playstore"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// By default, profiles are generated on a dedicated Gradle Managed Device, so that the results don't depend on the
// state of a developer's device. Pass -PgenerateOnConnectedDevice to use a connected device or emulator instead.
val generateOnConnectedDevice = providers.gradleProperty("generateOnConnectedDevice").isPresent

baselineProfile {
    useConnectedDevices = generateOnConnectedDevice
    if (!generateOnConnectedDevice) {
        managedDevices += "pixel6Api35"
    }
}

apply(from = "../gradle/src/ktlint.gradle")

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

androidComponents {
    onVariants { variant ->
        val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { artifactsLoader.load(it)?.applicationId }
        )
    }
}
