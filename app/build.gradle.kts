import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.taxeca.calculator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.taxeca.calculator"
        minSdk = 24
        targetSdk = 36
        versionCode = 15
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "APP_NAME", "\"TaxeCA\"")

        // AdMob configuration (read from local.properties for release)
        buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
    }

    signingConfigs {
        create("release") {
            storeFile     = localProps.getProperty("keystore.path")?.let { file(it) }
            storePassword = localProps.getProperty("keystore.password")
            keyAlias      = localProps.getProperty("keystore.alias")
            keyPassword   = localProps.getProperty("keystore.alias.password")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            // AdMob configuration for release (inject from local.properties)
            val admobAppId = localProps.getProperty("admob.app.id") ?: "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${localProps.getProperty("admob.banner.id") ?: "ca-app-pub-3940256099942544/6300978111"}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${localProps.getProperty("admob.interstitial.id") ?: "ca-app-pub-3940256099942544/1033173712"}\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"${localProps.getProperty("admob.rewarded.id") ?: "ca-app-pub-3940256099942544/5224354917"}\"")

            manifestPlaceholders["admobAppId"] = admobAppId
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
        debug {
            isMinifyEnabled   = false
            isShrinkResources = false
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    lint {
        // Baseline suppresses pre-existing RemoteViewLayout widget issue
        // (App widget uses a custom view; fix tracked separately)
        baseline = file("lint-baseline.xml")
        abortOnError = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.appcompat)
    implementation(libs.play.services.ads)
    implementation(libs.billing)
    implementation(libs.play.review)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
