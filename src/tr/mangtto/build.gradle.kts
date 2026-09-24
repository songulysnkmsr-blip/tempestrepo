plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
}

ext {
    set("pkgNameSuffix", "tr.mangtto")
    set("extClass", ".Mangtto")
    set("extVersionCode", 1)
    set("libVersion", "1.4")
}

apply(from = "$rootDir/common.gradle")
