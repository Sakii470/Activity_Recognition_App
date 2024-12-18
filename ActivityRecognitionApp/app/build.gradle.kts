import java.util.Properties


plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id ("kotlin-kapt")
    id ("com.google.dagger.hilt.android")
    id ("com.google.devtools.ksp")

}

android {
    namespace = "com.example.activityrecognitionapp"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    val properties = Properties()
    properties.load(File(rootDir, "local.properties").inputStream())
    val key: String = properties.getProperty("supabaseKey")
    val url: String = properties.getProperty("supabaseUrl")

    defaultConfig {
        applicationId = "com.example.activityrecognitionapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String","supabaseKey","\"$key\"")
        buildConfigField("String","supabaseUrl","\"$url\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core AndroidX libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)

    // Jetpack Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3.android)
    implementation (libs.androidx.material.icons.extended)

    // Bluetooth and client libraries
    implementation(libs.client)

    // Hilt for dependency injection
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.common)
    implementation (libs.androidx.hilt.work)
    implementation(libs.firebase.firestore.ktx)


    kapt(libs.hilt.android.compiler)

    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    testImplementation (libs.mockito.mockito.core)
    testImplementation (libs.mockito.kotlin)
    testImplementation (libs.kotlinx.coroutines.test)
    testImplementation (libs.junitparams)
    testImplementation (libs.mockk)
    testImplementation (libs.mockk.v1137)

    // Debugging tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //Supabase
    implementation(libs.gotrue.kt)
    implementation(libs.ktor.client.cio)

    //DataStore
    implementation(libs.androidx.datastore.core.android)
    implementation (libs.datastore.preferences)

    //Animation
    implementation (libs.accompanist.navigation.animation)

    //Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    //MPAndroidChart
    implementation (libs.mpandroidchart)

    //Gson
    implementation (libs.gson)

    //Room for Data
    implementation (libs.androidx.room.runtime)
    ksp (libs.androidx.room.compiler)
    implementation (libs.androidx.room.ktx)

    //Data Synchronization
    implementation (libs.androidx.work.runtime.ktx)

    // Hilt
    implementation (libs.hilt.android.v249)
    kapt (libs.hilt.compiler)

    // Hilt WorkManager integration
    implementation (libs.androidx.hilt.work)
    kapt (libs.androidx.hilt.compiler)

    // WorkManager
    implementation (libs.androidx.work.runtime.ktx)


}


