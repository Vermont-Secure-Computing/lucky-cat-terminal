plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.possin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.possin"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation(libs.bouncycastle)

    implementation(libs.web3j) {
        exclude( group = "org.bouncycastle", module = "bcprov-jdk15to18")
        exclude( group = "org.bouncycastle", module = "bcprov-jdk18on")
    }

    implementation(libs.bitcoinj) {
        exclude( group = "org.bouncycastle", module = "bcprov-jdk15to18")
        exclude( group = "org.bouncycastle", module = "bcprov-jdk18on")
    }
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
//    implementation(libs.bitcoinj)
    implementation(libs.guava)
    implementation(libs.card.view)
    implementation(libs.grid.layout)
    implementation(libs.zxing)
    implementation(libs.zxing.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit2)
    implementation(libs.okhttp)
    implementation(libs.okhttp3)
    implementation(libs.androidx.foundation.android)
    implementation(libs.thermalPrinter)
    implementation(libs.dogecoinj)
//    implementation(libs.web3j)
    implementation(libs.novecoincrypto)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

