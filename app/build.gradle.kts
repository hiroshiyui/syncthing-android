import org.gradle.configurationcache.extensions.capitalized

plugins {
    id("com.android.application")
    id("com.github.ben-manes.versions")
    id("com.github.triplet.play") version "3.7.0"
}

dependencies {
    implementation(libs.libsuperuser)
    implementation(libs.material)
    implementation(libs.gson)
    implementation(libs.jbcrypt)
    implementation(libs.guava)
    implementation(libs.stream)
    implementation(libs.volley)
    implementation(libs.commons.io)

    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        isTransitive = false
    }
    implementation(libs.core)

    implementation(libs.constraintlayout)
    implementation(libs.dagger)
    annotationProcessor(libs.dagger.compiler)
    androidTestImplementation(libs.rules)
    androidTestImplementation(libs.annotation)
}

android {
    val ndkVersionShared = rootProject.extra.get("ndkVersionShared")
    // Changes to these values need to be reflected in `../docker/Dockerfile`
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    ndkVersion = "$ndkVersionShared"

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.nutomic.syncthingandroid"
        minSdk = 21
        targetSdk = 35
        versionCode = 4395
        versionName = "1.28.1"
        testApplicationId = "com.nutomic.syncthingandroid.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = System.getenv("SYNCTHING_RELEASE_STORE_FILE")?.let(::file)
            storePassword = System.getenv("SIGNING_PASSWORD")
            keyAlias = System.getenv("SYNCTHING_RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_PASSWORD")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isJniDebuggable = true
            isRenderscriptDebuggable = true
            isMinifyEnabled = false
        }
        getByName("release") {
            signingConfig = signingConfigs.runCatching { getByName("release") }
                .getOrNull()
                .takeIf { it?.storeFile != null }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Otherwise libsyncthing.so doesn't appear where it should in installs
    // based on app bundles, and thus nothing works.
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    namespace = "com.nutomic.syncthingandroid"
}

play {
    serviceAccountCredentials.set(
        file(System.getenv("SYNCTHING_RELEASE_PLAY_ACCOUNT_CONFIG_FILE") ?: "keys.json")
    )
    track.set("beta")
}

/**
 * Some languages are not supported by Google Play, so we ignore them.
 */
tasks.register<Delete>("deleteUnsupportedPlayTranslations") {
    delete(
        "src/main/play/listings/de_DE/",
        "src/main/play/listings/el-EL/",
        "src/main/play/listings/en/",
        "src/main/play/listings/eo/",
        "src/main/play/listings/eu/",
        "src/main/play/listings/nb/",
        "src/main/play/listings/nl_BE/",
        "src/main/play/listings/nn/",
        "src/main/play/listings/ta/",
    )
}

project.afterEvaluate {
    android.buildTypes.forEach {
        tasks.named("merge${it.name.capitalized()}JniLibFolders") {
            dependsOn(":syncthing:buildNative")
        }
    }
}
