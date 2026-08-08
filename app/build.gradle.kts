import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun adUnitId(key: String, fallback: String): String {
    val value = localProperties.getProperty(key)?.takeIf { it.isNotBlank() } ?: fallback
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

android {
    namespace = "ru.cashguide.prod"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "ru.cashguide.prod"
        minSdk = 24
        targetSdk = 36
        versionCode = 75
        versionName = "1.0.75"

        buildConfigField("String", "UPDATE_URL", "\"http://185.26.115.32:8088/cashguide/api/getversion.php\"")
        buildConfigField("String", "APK_URL", "\"http://185.26.115.32:8088/cashguide/cashguide.apk\"")
        buildConfigField("String", "NEWS_URL", "\"http://185.26.115.32:8088/cashguide/api/news.php\"")
        buildConfigField("String", "BANNER_AD_UNIT_ID", adUnitId("ad_unit_id_banner", "demo-banner-yandex"))
        buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", adUnitId("ad_unit_id_interstitial", "demo-interstitial-yandex"))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.cardview)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    implementation(libs.room.runtime)
    implementation(libs.room.rxjava2)
    annotationProcessor(libs.room.compiler)

    implementation(libs.rxjava)
    implementation(libs.rxandroid)

    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation(libs.threetenabp)
    implementation(libs.yandex.ads)
    implementation(libs.activity)
    implementation(libs.mlkit.text.recognition)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
