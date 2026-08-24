plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.firstapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.firstapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "environment"

    productFlavors {
        create("sit") {
            dimension = "environment"
            buildConfigField("String", "ACTIVE_API_URL", "\"https://sit-active-api.yourapp.com\"")
            buildConfigField("String", "PASSIVE_API_URL", "\"https://sit-passive-api.yourapp.com\"")
            buildConfigField("String", "API_BASE_URL", "\"https://sit-active-api.yourapp.com\"")
            buildConfigField("String", "WS_BASE_URL", "\"wss://sit-active-api.yourapp.com\"")
            buildConfigField("Boolean", "ENABLE_DEV_SWITCH", "true")
        }
        create("uat") {
            dimension = "environment"
            buildConfigField("String", "ACTIVE_API_URL", "\"https://uat-active-api.yourapp.com\"")
            buildConfigField("String", "PASSIVE_API_URL", "\"https://uat-passive-api.yourapp.com\"")
            buildConfigField("String", "API_BASE_URL", "\"https://uat-active-api.yourapp.com\"")
            buildConfigField("String", "WS_BASE_URL", "\"wss://uat-active-api.yourapp.com\"")
            buildConfigField("Boolean", "ENABLE_DEV_SWITCH", "true")
        }
        create("beta") {
            dimension = "environment"
            // buildConfigField("String", "ACTIVE_API_URL", "\"https://firstapp-backend.fly.dev/api/v1/\"")
            // buildConfigField("String", "PASSIVE_API_URL", "\"https://firstapp-backend.fly.dev/api/v1/\"")
            // buildConfigField("String", "API_BASE_URL", "\"https://firstapp-backend.fly.dev/api/v1/\"")
            // buildConfigField("String", "WS_BASE_URL", "\"wss://firstapp-backend.fly.dev/api/v1/\"")
            buildConfigField("String", "ACTIVE_API_URL", "\"http://192.168.100.7:8082/api/v1/\"")
            buildConfigField("String", "PASSIVE_API_URL", "\"http://192.168.100.7:8082/api/v1/\"")
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.100.7:8082/api/v1/\"")
            buildConfigField("String", "WS_BASE_URL", "\"ws://192.168.100.7:8082/api/v1/\"")
            buildConfigField("Boolean", "ENABLE_DEV_SWITCH", "true")
        }
        create("prod") {
            dimension = "environment"
            // buildConfigField("String", "ACTIVE_API_URL", "\"https://firstapp-backend.fly.dev/api/v1/\"")
            // buildConfigField("String", "PASSIVE_API_URL", "\"https://firstapp-backend.fly.dev/api/v1/\"")
            // buildConfigField("String", "API_BASE_URL", "\"https://firstapp-backend.fly.dev/api/v1/\"")
            // buildConfigField("String", "WS_BASE_URL", "\"wss://firstapp-backend.fly.dev/api/v1/\"")
            buildConfigField("String", "ACTIVE_API_URL", "\"http://192.168.100.7:8082/api/v1/\"")
            buildConfigField("String", "PASSIVE_API_URL", "\"http://192.168.100.7:8082/api/v1/\"")
            buildConfigField("String", "API_BASE_URL", "\"http://192.168.100.7:8082/api/v1/\"")
            buildConfigField("String", "WS_BASE_URL", "\"ws://192.168.100.7:8082/api/v1/\"")
            buildConfigField("Boolean", "ENABLE_DEV_SWITCH", "false")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols.add("**/libandroidx.graphics.path.so")
            keepDebugSymbols.add("**/libapplovin-native-crash-reporter.so")
            keepDebugSymbols.add("**/libdatastore_shared_counter.so")
            keepDebugSymbols.add("**/libjingle_peerconnection_so.so")
            keepDebugSymbols.add("**/libsqlcipher.so")
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation(libs.androidx.activity.compose)
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.ads)
    implementation(libs.play.services.location)
    implementation(libs.places)
    implementation(libs.google.maps.services)
    implementation(libs.coil.compose)
    implementation("io.coil-kt:coil-video:2.5.0")
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource)
    implementation(libs.media3.database)
    implementation(libs.media3.ui)
    implementation("androidx.media3:media3-session:1.2.1")
    implementation("io.github.webrtc-sdk:android:144.7559.01")

    // HTTP Client
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.inappmessaging)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher)
    implementation(libs.sqlite.ktx)

    implementation("nl.dionsegijn:konfetti-compose:2.0.5")
    implementation(libs.androidx.work.runtime.ktx)

    // Ad Mediation - AppLovin MAX
    implementation(libs.applovin.sdk)
    implementation(libs.applovin.google.adapter)

    // Paging 3
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Security & Biometrics
    implementation("androidx.security:security-crypto:1.1.0")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
