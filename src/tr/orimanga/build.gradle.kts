plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "eu.kanade.tachiyomi.extension.tr.orimanga"
    compileSdk = 34

    defaultConfig {
        applicationId = "eu.kanade.tachiyomi.extension.tr.orimanga"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.4.1"
    }

    signingConfigs {
        create("release") {
            val keyFile = rootProject.file("signingkey.p12")
            if (keyFile.exists()) {
                storeFile = keyFile
                storePassword = "tempestpass"
                keyAlias = "tempest"
                keyPassword = "tempestpass"
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("src")
            res.srcDirs("res")
        }
    }
}

dependencies {
    compileOnly("com.github.tachiyomiorg:extensions-lib:1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.reactivex:rxjava:1.3.8")
}
