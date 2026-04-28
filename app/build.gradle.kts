//plugins {
//    alias(libs.plugins.android.application)
//    alias(libs.plugins.kotlin.compose)
//}
//
//android {
//    namespace = "com.basahero.elearning"
//    compileSdk {
//        version = release(36) {
//            minorApiLevel = 1
//        }
//    }
//
//    defaultConfig {
//        applicationId = "com.basahero.elearning"
//        minSdk = 26
//        targetSdk = 36
//        versionCode = 1
//        versionName = "1.0"
//
//        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//    buildFeatures {
//        compose = true
//    }
//}
//
//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.compose.ui)
//    implementation(libs.androidx.compose.ui.graphics)
//    implementation(libs.androidx.compose.ui.tooling.preview)
//    implementation(libs.androidx.compose.material3)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
//    debugImplementation(libs.androidx.compose.ui.tooling)
//    debugImplementation(libs.androidx.compose.ui.test.manifest)
//}
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace  = "com.basahero.elearning"   // your existing namespace — unchanged
    compileSdk {
        version = release(36) {              // your existing compileSdk block — unchanged
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.basahero.elearning"  // your existing app ID — unchanged
        minSdk        = 26
        targetSdk     = 36
        versionCode   = 1
        versionName   = "1.0"

        // Supabase credentials — read from local.properties (not committed to Git)
        // Supabase credentials — read from local.properties (not committed to Git)
        val properties = Properties().apply {
            val localPropertiesFile = project.rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                load(localPropertiesFile.inputStream())
            }
        }
        val supabaseUrl = properties.getProperty("SUPABASE_URL") ?: ""
        val supabaseKey = properties.getProperty("SUPABASE_ANON_KEY") ?: ""
        buildConfigField("String", "SUPABASE_URL",      "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false          // kept your existing value — enable when ready to ship
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"   // added — keeps debug + release installable side-by-side
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }



    buildFeatures {
        compose     = true
        buildConfig = true                   // added — needed to expose SUPABASE_URL/KEY to Kotlin code
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"  // added — prevents duplicate file errors at build
        }
    }
}

dependencies {
    // ── Core (your existing) ──────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // ── Compose BOM — pins all Compose versions together ─────────────────
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)  // added — for icons across the app
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ── Navigation (added) ────────────────────────────────────────────────
    implementation(libs.androidx.navigation.compose)

    // ── Room (added) ──────────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ── WorkManager (added) ───────────────────────────────────────────────
    implementation(libs.androidx.work.runtime)

    // ── DataStore (added) ─────────────────────────────────────────────────
    implementation(libs.androidx.datastore.prefs)

    // ── Supabase (added) ──────────────────────────────────────────────────
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.ktor.client.android)

    // ── Image loading (added) ─────────────────────────────────────────────
    implementation(libs.coil.compose)

    // ── Lottie animations (added) ─────────────────────────────────────────
    implementation(libs.lottie.compose)

    // ── CSV parsing — teacher roster import (added) ───────────────────────
    implementation(libs.opencsv)

    // ── Kotlin serialization — JSON seed parsing (added) ─────────────────
    implementation(libs.kotlinx.serialization.json)

    // ── Testing (your existing + additions) ──────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
// ADD THIS AT THE BOTTOM OF THE FILE
kotlin {
    jvmToolchain(17)
}