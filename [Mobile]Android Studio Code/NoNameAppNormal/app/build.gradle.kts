plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.nonameapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.nonameapp"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Enable multidex
        multiDexEnabled = true
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.retrofit)
    implementation(libs.gsonConverter)
    implementation(libs.swiperefreshlayout)
    implementation(libs.okhttp)
    implementation(libs.commonsCodec)
    implementation(libs.lifecycleExtensions)
    implementation(libs.lifecycleRuntime)
    implementation(libs.multidex)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}