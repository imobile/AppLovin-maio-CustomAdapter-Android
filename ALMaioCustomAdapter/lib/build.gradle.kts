plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "jp.maio.adapter.applovin"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.applovin.sdk)
    implementation(libs.maio.android.sdk) {
        artifact {
            type = "aar"
        }
    }
}