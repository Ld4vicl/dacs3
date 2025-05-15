import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

// Đọc từ local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.app.chefshare"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.chefshare"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val openAIKey = localProperties["OPENAI_API_KEY"] as? String ?: ""
        buildConfigField("String", "OPENAI_API_KEY", "\"$openAIKey\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file("chefshare.keystore")
            storePassword = "12345678"
            keyAlias = "chefsharekey"
            keyPassword = "12345678"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ✅ Firebase BOM để đồng bộ version tránh conflict
    implementation(platform("com.google.firebase:firebase-bom:32.7.3"))

    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation(libs.androidx.monitor)
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.junit.ktx)
    testImplementation(libs.junit.junit)
    testImplementation(libs.testng)
    androidTestImplementation(libs.junit.junit)

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // CircleImageView
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // OpenAI - Retrofit + OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}
