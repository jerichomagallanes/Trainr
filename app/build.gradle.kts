import java.io.File
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 provides built-in Kotlin support, so no separate kotlin-android plugin.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// Play refuses an upload whose versionCode is not above the last one, and a
// forgotten bump is only discovered after the build and the upload have already
// been spent. Counting commits makes it climb by itself, once per merge, with no
// step for anyone to remember.
val commitCount = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim().toIntOrNull() ?: 0 }

// A shallow clone, an exported zip or a machine without git has no history to
// count. The floor keeps such a build from numbering itself below something
// already uploaded, which Play would reject. Note for whoever adds a job that
// builds a release: actions/checkout fetches one commit by default, so it would
// count 1 and fall back to this floor. Such a job needs fetch-depth: 0.
val minimumVersionCode = 2

android {
    namespace = "com.jericx.trainr"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jericx.trainr"
        minSdk = 24
        targetSdk = 36
        versionCode = maxOf(runCatching { commitCount.get() }.getOrDefault(0), minimumVersionCode)
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Fallback for components that are not app variants (e.g. the unit test
        // manifest); real variants get a labelled name from androidComponents below.
        manifestPlaceholders["appName"] = "Trainr"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // English-only for now: ja and tl stay in the repo but out of the build
    // until language switching returns.
    androidResources {
        localeFilters += listOf("en")
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }

        create("prod") {
            dimension = "environment"
        }
    }

    // Signing details live in ~/.gradle/gradle.properties, never in the
    // repository: the keystore itself is outside the project entirely. A
    // machine without them, CI included, simply gets no release signing config
    // rather than a build that fails to configure.
    val uploadStoreFile = (findProperty("TRAINR_UPLOAD_STORE_FILE") as String?)
        ?.let(::File)
        ?.takeIf { it.exists() }

    signingConfigs {
        if (uploadStoreFile != null) {
            create("upload") {
                storeFile = uploadStoreFile
                storePassword = findProperty("TRAINR_UPLOAD_STORE_PASSWORD") as String?
                keyAlias = findProperty("TRAINR_UPLOAD_KEY_ALIAS") as String?
                keyPassword = findProperty("TRAINR_UPLOAD_KEY_PASSWORD") as String?
            }
        }
    }

    buildTypes {
        release {
            // Absent on a machine without the keystore, which leaves an unsigned
            // release build rather than a broken one.
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // Release's R8 configuration, but debug-signed so it can be installed.
        // Exists so CI can launch a minified build and catch a missing keep rule,
        // which otherwise only shows up in production.
        create("minified") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // Emit SARIF so CI can publish lint findings into GitHub code scanning.
        sarifReport = true
        abortOnError = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }

        // Gradle Managed Devices: the emulator definition lives in the build file
        // instead of the CI config, so local runs and CI use the exact same device.
        // ATD (Automated Test Device) images boot faster and are far less flaky on
        // headless runners than the full google_apis images.
        managedDevices {
            localDevices {
                create("ciAtd") {
                    device = "Pixel 6"
                    apiLevel = 34
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

// The release build of the prod flavor ships the plain app name; every other
// variant is labelled so testers can tell the installed builds apart.
androidComponents {
    val baseVersionName = android.defaultConfig.versionName
    onVariants { variant ->
        val appName = if (variant.name == "prodRelease") {
            "Trainr"
        } else {
            "Trainr $baseVersionName ${variant.flavorName.orEmpty().uppercase()}"
        }
        variant.manifestPlaceholders.put("appName", appName)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

ksp {
    arg("dagger.fastInit", "enabled")
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.lifecycle)

    // Coroutines & serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Dagger - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // Video tutorials
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.playintegrity)
    // Debug and minified-smoke builds cannot pass Play Integrity: nothing there
    // was installed from Play. They attest with a token registered in the
    // console instead, which is what makes local development possible at all.
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(libs.youtube.player)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.truth)

    // Instrumentation tests
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.truth)
}

// What the next upload will be numbered, without building it: Play only tells
// you the number was wrong after the whole bundle has been sent.
tasks.register("printVersionCode") {
    val versionCode = android.defaultConfig.versionCode
    doLast { println(versionCode) }
}
