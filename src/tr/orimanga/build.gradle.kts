plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
}

ext {
    set("pkgNameSuffix", "tr.orimanga")
    set("extClass", ".OriManga")
    set("extVersionCode", 1)
    set("libVersion", "1.4")
}

apply(from = "$rootDir/common.gradle")

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
