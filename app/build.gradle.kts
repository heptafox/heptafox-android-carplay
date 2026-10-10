import java.util.Properties

plugins {
    id("com.android.application")
}

// Secrets live in local.properties (git-ignored), e.g. YT_API_KEY=AIza...
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.heptafox.carplay"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.heptafox.carplay"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "YT_API_KEY", "\"${localProperties.getProperty("YT_API_KEY", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.car.app:app:1.7.0")
    implementation("androidx.car.app:app-projected:1.7.0") // CarAudioRecord's Android Auto implementation
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
}
