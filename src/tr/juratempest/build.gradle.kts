plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "eu.kanade.tachiyomi.extension.tr.juratempest"
    compileSdk = 35

    defaultConfig {
        applicationId = "eu.kanade.tachiyomi.extension.tr.juratempest"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.4.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    compileOnly("com.github.tachiyomiorg:extensions-lib:1.4.1")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation("org.jsoup:jsoup:1.18.1")
}
