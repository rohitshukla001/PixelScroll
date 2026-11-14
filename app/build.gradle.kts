plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.rohitshukla.pixelscroll"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.rohitshukla.pixelscroll"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// Copies the latest debug APK to apk/PixelScroll.apk after every debug build,
// so the apk folder always holds exactly one up-to-date APK.
val exportApk by tasks.registering(Copy::class) {
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(rootProject.layout.projectDirectory.dir("apk"))
    rename { "PixelScroll.apk" }
}
tasks.matching { it.name == "assembleDebug" }.configureEach {
    finalizedBy(exportApk)
}
