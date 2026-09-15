plugins {
    id("com.android.application")
}

android {
    namespace = "id.feezet.efauth"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.feezet.efauth"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "1.1.8"
    }

    signingConfigs {
        create("evaultStable") {
            val keyFile = rootProject.file("signing/evault-debug.jks")
            if (keyFile.exists()) {
                storeFile = keyFile
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            val keyFile = rootProject.file("signing/evault-debug.jks")
            if (keyFile.exists()) signingConfig = signingConfigs.getByName("evaultStable")
        }
    }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
}
