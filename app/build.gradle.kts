plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.vermont.possin"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.vermont.possin"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.3"
    }

    // ---------- FLAVORS ----------
    flavorDimensions += "dist"

    productFlavors {
        create("full") { dimension = "dist" }
        create("fdroid") { dimension = "dist" }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = project.findProperty("RELEASE_STORE_FILE")?.toString()
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = project.findProperty("RELEASE_STORE_PASSWORD")?.toString()
                keyAlias = project.findProperty("RELEASE_KEY_ALIAS")?.toString()
                keyPassword = project.findProperty("RELEASE_KEY_PASSWORD")?.toString()
            }
        }
    }


    // ---------- CONFIGURE FLAVOR-SPECIFIC DEPENDENCIES ----------
    configurations.configureEach {
        when (name) {
            "fullDebugImplementation",
            "fullReleaseImplementation" ->
                extendsFrom(configurations.getByName("fullImplementation"))

            "fdroidDebugImplementation",
            "fdroidReleaseImplementation" ->
                extendsFrom(configurations.getByName("fdroidImplementation"))
        }
    }


    // ---------- BUILD TYPES ----------
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    // Crypto
    implementation(libs.bouncycastle)

    implementation(libs.web3j) {
        exclude("org.bouncycastle", "bcprov-jdk15to18")
        exclude("org.bouncycastle", "bcprov-jdk18on")
    }

    implementation(libs.bitcoinj) {
        exclude("org.bouncycastle", "bcprov-jdk15to18")
        exclude("org.bouncycastle", "bcprov-jdk18on")
    }

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.recyclerview)

    // Core
    implementation(libs.guava)
    implementation(libs.commons.codec)
    implementation(libs.org.slf4j)
    implementation(libs.logback.classic)

    // ZXing
    implementation(libs.zxing)
    implementation(libs.zxing.android)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit2)
    implementation(libs.retrofit.scalars)
    implementation(libs.okhttp)
    implementation(libs.okhttp3)
    implementation(libs.logging.interceptor)
    implementation(libs.grid.layout)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.room.testing)

    // Printer
    implementation(libs.thermalPrinter)
    add("fdroidImplementation", libs.thermalPrinter)

    // ---------- GIF (FLAVOR-SPECIFIC) ----------
    add("fullImplementation", libs.android.gif.drawable)
    add("fdroidImplementation", "io.coil-kt:coil:2.6.0")
    add("fdroidImplementation", "io.coil-kt:coil-gif:2.6.0")


    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.runner)
}
