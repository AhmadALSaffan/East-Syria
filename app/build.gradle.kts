import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.compose)
    id ("kotlin-parcelize")
}

android {
    namespace = "com.example.eastsyria"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.eastsyria"
        minSdk = 31
        targetSdk = 36
        versionCode = 3
        versionName = "3.0"
        val properties = Properties()
        properties.load(FileInputStream(rootProject.file("local.properties")))
        val mapsApiKey = properties.getProperty("MAPS_API_KEY", "")

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        // Supabase Storage holds the photos (Firebase Storage needs the Blaze plan).
        // The publishable key is meant to ship in the app; uploads are guarded by bucket policies.
        val supabaseUrl = properties.getProperty("SUPABASE_URL", "https://rqndcsdrrtibwpzabuqu.supabase.co")
        val supabaseKey = properties.getProperty("SUPABASE_KEY", "sb_publishable_oPX0z_P39yzyUuPu3p3RgQ_Boh0YcOH")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/LICENSE"
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        // Compose draws the onboarding scenes; the rest of the app is still XML views.
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.auth)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.google.firebase:firebase-auth:24.0.1")
    implementation("com.google.firebase:firebase-bom:32.7.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.firebase:firebase-database:22.0.1")
    implementation("com.google.firebase:firebase-firestore:26.1.0")

    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("com.github.bumptech.glide:glide:5.0.5")

    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("com.google.maps.android:android-maps-utils:3.8.2")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    // QR code for setting up an authenticator app
    implementation("com.google.zxing:core:3.5.3")
}
apply (plugin = "com.google.gms.google-services")