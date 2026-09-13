plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.uvcmonitor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.uvcmonitor"
        minSdk = 26
        // Keep at 33: AUSBC 3.2.7's USBMonitor registers a receiver for a custom
        // action without RECEIVER_EXPORTED/NOT_EXPORTED, which Android 14 rejects
        // (SecurityException on launch) for apps targeting SDK 34.
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // UVC / HDMI-capture engine (jiangdongguo AUSBC), pulled from JitPack.
    // 3.2.7 is the newest tag JitPack built successfully; 3.3.x builds are broken
    // (libuvc module never published, so libausbc cannot resolve).
    implementation("com.github.jiangdongguo.AndroidUSBCamera:libausbc:3.2.7")
}

// libausbc 3.2.7 still declares two dependencies that only ever lived on
// JCenter (shut down). Both authors republished under new coordinates on
// Maven Central, so redirect them there.
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.gyf.immersionbar:immersionbar"))
            .using(module("com.geyifeng.immersionbar:immersionbar:3.2.2"))
            .because("JCenter is gone; author republished on Maven Central")
        substitute(module("com.zlc.glide:webpdecoder"))
            .using(module("com.github.zjupure:webpdecoder:2.0.4.13.2"))
            .because("JCenter is gone; same library, new groupId on Maven Central")
    }
}
