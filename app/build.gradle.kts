plugins {
    alias(libs.plugins.android.application)
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
        versionCode = 71
        versionName = "1.0.71"

        buildConfigField("String", "UPDATE_URL", "\"http://185.26.115.32:8088/cashguide/api/getversion.php\"")
        buildConfigField("String", "APK_URL", "\"http://185.26.115.32:8088/cashguide/cashguide.apk\"")

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

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
