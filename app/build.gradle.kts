//import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
//    alias(libs.plugins.shadow)
}

android {
    namespace = "com.vermont.possin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vermont.possin"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        setProperty("archivesBaseName", "LuckyCat-$versionName")
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
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }

    implementation(libs.bitcoinj) {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk18on")
    }
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

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
    implementation(libs.android.gif.drawable)
    implementation(libs.org.slf4j)
    implementation(libs.logback.classic)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.retrofit.scalars)
    implementation(libs.commons.codec)


    implementation(libs.room.runtime)
    implementation(libs.androidx.core.splashscreen)
    kapt(libs.room.compiler)
    implementation(libs.room.ktx)
    implementation(libs.room.testing)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.test.core)
    testImplementation(libs.test.ext.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.hamcrest)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.espresso.accessibility)
    androidTestImplementation(libs.androidx.espresso.web)
    androidTestImplementation(libs.hamcrest.library)
//    androidTestImplementation(libs.androidx.espresso.idling)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.google.truth)
    // others
    //    implementation(libs.web3j)
    //    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
//    implementation(libs.kotlinStdlib)
//    implementation(libs.trust.wallet)
}
//
//tasks.register<ShadowJar>("shadowJar") {
//    archiveClassifier.set("all")
//
//    // Include the JAR file explicitly
//    from(fileTree("libs") { include("woodcoinj-core-0.14.2.jar") })
//
//    // Relocate org.bitcoinj classes in woodcoinj-core to avoid conflicts
//    relocate("org.bitcoinj", "org.woodcoinj.shaded.bitcoinj")
//}
//
//tasks.withType<ShadowJar> {
//    mergeServiceFiles()
//    manifest {
//        attributes["Main-Class"] = "com.example.possin.MainActivity" // Adjust the main class if necessary
//    }
//}

