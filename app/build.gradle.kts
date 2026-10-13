val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull

val testAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdMobBannerId = "ca-app-pub-3940256099942544/9214589741"
val testAdMobInterstitialId = "ca-app-pub-3940256099942544/1033173712"

val productionAdMobAppId = providers.environmentVariable("ADMOB_APP_ID").orNull
val productionAdMobBannerId = providers.environmentVariable("ADMOB_BANNER_ID").orNull
val productionAdMobInterstitialId = providers.environmentVariable("ADMOB_INTERSTITIAL_ID").orNull

val productionAdsConfigured =
    listOf(
        productionAdMobAppId,
        productionAdMobBannerId,
        productionAdMobInterstitialId
    ).all { !it.isNullOrBlank() }

val resolvedAdMobAppId =
    if (productionAdsConfigured) productionAdMobAppId!! else testAdMobAppId
val resolvedAdMobBannerId =
    if (productionAdsConfigured) productionAdMobBannerId!! else testAdMobBannerId
val resolvedAdMobInterstitialId =
    if (productionAdsConfigured) productionAdMobInterstitialId!! else testAdMobInterstitialId

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.riccardopinato.inviaalpc"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.riccardopinato.inviaalpc"
        minSdk = 29
        targetSdk = 36
        versionCode = 42
        versionName = "1.27.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        manifestPlaceholders["ADMOB_APP_ID"] =
            resolvedAdMobAppId

        buildConfigField(
            "String",
            "ADMOB_APP_ID",
            "\"$resolvedAdMobAppId\""
        )
        buildConfigField(
            "String",
            "ADMOB_BANNER_ID",
            "\"$resolvedAdMobBannerId\""
        )
        buildConfigField(
            "String",
            "ADMOB_INTERSTITIAL_ID",
            "\"$resolvedAdMobInterstitialId\""
        )
        buildConfigField(
            "boolean",
            "ADMOB_PRODUCTION_CONFIGURED",
            productionAdsConfigured.toString()
        )
    }

    signingConfigs {
        val signingReady =
            listOf(
                releaseKeystorePath,
                releaseKeystorePassword,
                releaseKeyAlias,
                releaseKeyPassword
            ).all {
                !it.isNullOrBlank()
            }

        if (signingReady) {
            create("persistentRelease") {
                storeFile =
                    file(
                        releaseKeystorePath!!
                    )
                storePassword =
                    releaseKeystorePassword
                keyAlias =
                    releaseKeyAlias
                keyPassword =
                    releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            manifestPlaceholders["ADMOB_APP_ID"] =
                testAdMobAppId
            buildConfigField(
                "String",
                "ADMOB_APP_ID",
                "\"$testAdMobAppId\""
            )
            buildConfigField(
                "String",
                "ADMOB_BANNER_ID",
                "\"$testAdMobBannerId\""
            )
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_ID",
                "\"$testAdMobInterstitialId\""
            )
            buildConfigField(
                "boolean",
                "ADMOB_PRODUCTION_CONFIGURED",
                "false"
            )
        }

        create("slim") {
            initWith(getByName("debug"))
            isDebuggable = false
            manifestPlaceholders["ADMOB_APP_ID"] =
                testAdMobAppId
            buildConfigField(
                "String",
                "ADMOB_APP_ID",
                "\"$testAdMobAppId\""
            )
            buildConfigField(
                "String",
                "ADMOB_BANNER_ID",
                "\"$testAdMobBannerId\""
            )
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_ID",
                "\"$testAdMobInterstitialId\""
            )
            buildConfigField(
                "boolean",
                "ADMOB_PRODUCTION_CONFIGURED",
                "false"
            )
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        release {
            signingConfigs
                .findByName(
                    "persistentRelease"
                )
                ?.let {
                    signingConfig = it
                }

            isMinifyEnabled = true
            isShrinkResources = true
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
        resources.excludes += setOf(
            "/META-INF/AL2.0",
            "/META-INF/LGPL2.1",
            "/META-INF/LICENSE*",
            "/META-INF/NOTICE*"
        )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.4")

    // Lifecycle 2.11 Compose artifacts require compileSdk 37.
    // 2.10.0 keeps this release compatible with stable Android SDK 36.
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")

    // Compose 1.11.x line: compatible with compileSdk 36.
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")


    implementation("com.google.zxing:core:3.5.4")

    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
